package com.fyoyi.betterfood.block.cooking_pan;
import com.fyoyi.betterfood.block.entity.ModBlockEntities;
import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.client.gui.PotInfoOverlay;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.network.NetworkManager;
import com.fyoyi.betterfood.network.PotMessagePacket;
import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import com.fyoyi.betterfood.util.CookednessHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Set;
public class SimpleFoodBlock extends BaseEntityBlock {
    private static final VoxelShape BOTTOM_SHAPE = Block.box(2, 0, 2, 14, 1, 14);
    private static final VoxelShape NORTH_WALL = Block.box(2, 1, 2, 14, 4, 3);
    private static final VoxelShape SOUTH_WALL = Block.box(2, 1, 13, 14, 4, 14);
    private static final VoxelShape WEST_WALL = Block.box(2, 1, 3, 3, 4, 13);
    private static final VoxelShape EAST_WALL = Block.box(13, 1, 3, 14, 4, 13);
    private static final VoxelShape SHAPE = Shapes.or(BOTTOM_SHAPE, NORTH_WALL, SOUTH_WALL, WEST_WALL, EAST_WALL);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

//boolean isHeating = PotBlockEntity.isHeated(level,pos);

    public SimpleFoodBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PotBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.POT_BE.get(), PotBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack handStack = pPlayer.getItemInHand(pHand);

        // 1. 蹲下拿锅
        if (pPlayer.isShiftKeyDown() && handStack.isEmpty()) {
            if (!pLevel.isClientSide) {
                ItemStack potItem = new ItemStack(this);
                BlockEntity be = pLevel.getBlockEntity(pPos);
                if (be instanceof PotBlockEntity pot) {
                    pot.saveToItem(potItem);
                    pot.getItems().clear();
                }
                pLevel.removeBlock(pPos, false);
                pPlayer.setItemInHand(pHand, potItem);
                pLevel.playSound(null, pPos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (handStack.getItem() == Items.STICK) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                pot.triggerFlip();
                if(PotBlockEntity.isHeated(pLevel,pPos))
                    pLevel.playSound(pPlayer, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5F, 1.5F);
                pLevel.playSound(pPlayer, pPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 2.5F);

                if (!pLevel.isClientSide) {
                    // 检查是否有匹配的菜谱
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

                    // 发送网络包到客户端，让消息显示在锅的上方
                    if (pPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        NetworkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PotMessagePacket(pPos, message));
                    }
                }

                return InteractionResult.sidedSuccess(pLevel.isClientSide);
            }
        }

        // 碗 出锅
        if (handStack.getItem() == Items.BOWL) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                ItemStack result = pot.serveDish();
                if (!result.isEmpty()) {
                    if (!pPlayer.isCreative()) handStack.shrink(1);
                    if (handStack.isEmpty()) pPlayer.setItemInHand(pHand, result);
                    else if (!pPlayer.getInventory().add(result)) pPlayer.drop(result, false);

                    if(PotBlockEntity.isHeated(pLevel, pPos))
                        pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(pLevel.isClientSide);
                }
            }
        }

        // 放入逻辑
        if (!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                if (!handStack.isEmpty()) {
                    if (handStack.getItem().isEdible()) {
                        boolean success = pot.pushItem(handStack);
                        if (success) {
                            pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                            if (!pPlayer.isCreative()) handStack.shrink(1);
                        }
                    }
                } else {
                    ItemStack takenItem = pot.popItem();
                    if (!takenItem.isEmpty()) {
                        if (!pPlayer.getInventory().add(takenItem))
                            pPlayer.drop(takenItem, false);
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
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}