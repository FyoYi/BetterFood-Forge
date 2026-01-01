package com.fyoyi.betterfood.block.cooking_pan;

import com.fyoyi.betterfood.block.entity.ModBlockEntities;
import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.item.ModItems;
import com.fyoyi.betterfood.network.NetworkManager;
import com.fyoyi.betterfood.network.PotMessagePacket;
import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SimpleFoodBlock extends BaseEntityBlock {

    // 碰撞箱定义
    private static final VoxelShape BOTTOM_SHAPE = Block.box(2, 0, 2, 14, 1, 14);
    private static final VoxelShape NORTH_WALL = Block.box(2, 1, 2, 14, 4, 3);
    private static final VoxelShape SOUTH_WALL = Block.box(2, 1, 13, 14, 4, 14);
    private static final VoxelShape WEST_WALL = Block.box(2, 1, 3, 3, 4, 13);
    private static final VoxelShape EAST_WALL = Block.box(13, 1, 3, 14, 4, 13);
    private static final VoxelShape SHAPE = Shapes.or(BOTTOM_SHAPE, NORTH_WALL, SOUTH_WALL, WEST_WALL, EAST_WALL);

    // 方块属性
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_OIL = BooleanProperty.create("has_oil");
    public static final BooleanProperty IS_HEATED = BooleanProperty.create("is_heated");

    public SimpleFoodBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_OIL, false).setValue(IS_HEATED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PotBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.POT_BE.get(), (level, pos, state, blockEntity) -> {
            PotBlockEntity.tick(level, pos, state, blockEntity);
            
            // 检查热源状态并更新方块状态
            if (!level.isClientSide) {
                boolean hasHeat = PotBlockEntity.isHeated(level, pos);
                if (state.getValue(IS_HEATED) != hasHeat) {
                    level.setBlock(pos, state.setValue(IS_HEATED, hasHeat), 3);
                }
            }
        });
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack handStack = pPlayer.getItemInHand(pHand);

        // 拿锅
        if (pPlayer.isShiftKeyDown() && handStack.isEmpty()) {
            if (!pLevel.isClientSide) {
                ItemStack potItem = new ItemStack(this);
                BlockEntity be = pLevel.getBlockEntity(pPos);
                if (be instanceof PotBlockEntity pot) {
                    pot.saveToItem(potItem);
                    pot.getItems().clear();

                    CompoundTag tag = potItem.getOrCreateTagElement("BlockEntityTag");
                    tag.putBoolean("HasOil", pState.getValue(HAS_OIL));
                    tag.putBoolean("IsHeated", pState.getValue(IS_HEATED));
                }
                pLevel.removeBlock(pPos, false);
                pPlayer.setItemInHand(pHand, potItem);
                pLevel.playSound(null, pPos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // 锅铲交互
        if (handStack.getItem() == ModItems.SPATULA.get()) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {

                boolean isSpatulaOily = handStack.hasTag() && handStack.getTag().getBoolean("IsOily");
                boolean potHasOil = pState.getValue(HAS_OIL);

                if (isSpatulaOily && !potHasOil) {
                    if (!pLevel.isClientSide) {
                        // 修改方块状态 (视觉)
                        pLevel.setBlock(pPos, pState.setValue(HAS_OIL, true), 3);
                        // 同步给实体 (逻辑)
                        pot.setHasOil(true);

                        // 铲子变干净
                        handStack.getTag().remove("IsOily");

                        pLevel.playSound(null, pPos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                        if (PotBlockEntity.isHeated(pLevel, pPos)) {
                            if (be instanceof PotBlockEntity potBlockEntity) {
                                potBlockEntity.spawnOilInputParticles();
                            }
                            pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.5F, 3.5F);
                        }
                    }
                    return InteractionResult.sidedSuccess(pLevel.isClientSide);
                }

                // 翻炒
                else if (!isSpatulaOily) {
                    // 触发翻炒动画
                    pot.triggerFlip();

                    // 播放音效 (区分冷热锅)
                    if (PotBlockEntity.isHeated(pLevel, pPos) && potHasOil) {
                        pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5F, 1.5F);
                    }
                    pLevel.playSound(null, pPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 2.5F);

                    // 消耗铲子耐久
                    handStack.hurtAndBreak(1, pPlayer, (player) -> {
                        player.broadcastBreakEvent(pHand);
                    });

                    // 菜谱检测与提示 (仅服务端执行)
                    if (!pLevel.isClientSide) {
                        List<CulinaryRecipe> recipes = pLevel.getRecipeManager()
                                .getAllRecipesFor(ModRecipes.CULINARY_TYPE.get());

                        CulinaryRecipe matchedRecipe = null;
                        for (CulinaryRecipe recipe : recipes) {
                            if (recipe.matches(pot, pLevel)) {
                                matchedRecipe = recipe;
                                break;
                            }
                        }

                        boolean foundMatch = matchedRecipe != null;
                        String message = foundMatch ? "✅ 可制作: " + matchedRecipe.getResultItem(pLevel.registryAccess()).getHoverName().getString() : "❌ 当前食材无法匹配任何菜谱";

                        if (pPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            NetworkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PotMessagePacket(pPos, message));
                        }
                    }
                    return InteractionResult.sidedSuccess(pLevel.isClientSide);
                }
            }
        }

        // 3. 碗 -> 出锅
        if (handStack.getItem() == Items.BOWL) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                ItemStack result = pot.serveDish();
                if (!result.isEmpty()) {
                    if (!pPlayer.isCreative()) handStack.shrink(1);

                    if (handStack.isEmpty()) pPlayer.setItemInHand(pHand, result);
                    else if (!pPlayer.getInventory().add(result)) pPlayer.drop(result, false);

                    if (PotBlockEntity.isHeated(pLevel, pPos)) {
                        pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(pLevel.isClientSide);
                }
            }
        }

        // 4. 放入/取出物品
        if (!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                if (!handStack.isEmpty()) {
                    // === 必须是可食用物品才能放入 ===
                    if (handStack.getItem().isEdible()) {
                        boolean success = pot.pushItem(handStack);
                        if (success) {
                            pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                            if (!pPlayer.isCreative()) {
                                handStack.shrink(1);
                            }
                        }
                    }
                } else {
                    // 空手 -> 取出物品
                    ItemStack takenItem = pot.popItem();
                    if (!takenItem.isEmpty()) {
                        if (!pPlayer.getInventory().add(takenItem)) {
                            pPlayer.drop(takenItem, false);
                        }
                        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PotBlockEntity) {
                Containers.dropContents(pLevel, pPos, ((PotBlockEntity) blockEntity).getItems());
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        // 尝试从物品NBT读取是否有油和是否加热
        ItemStack stack = pContext.getItemInHand();
        boolean hasOil = false;
        boolean isHeated = false;
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("HasOil")) {
                    hasOil = tag.getBoolean("HasOil");
                }
                if (tag.contains("IsHeated")) {
                    isHeated = tag.getBoolean("IsHeated");
                }
            }
        }
        
        // 如果NBT中没有加热状态，检查放置位置下方是否有热源
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        if (!stack.hasTag() || !stack.getTagElement("BlockEntityTag").contains("IsHeated")) {
            isHeated = PotBlockEntity.isHeated(level, pos);
        }

        return this.defaultBlockState()
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(HAS_OIL, hasOil)
                .setValue(IS_HEATED, isHeated);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, HAS_OIL, IS_HEATED);
    }
}