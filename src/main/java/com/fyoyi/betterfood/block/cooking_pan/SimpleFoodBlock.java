package com.fyoyi.betterfood.block.cooking_pan;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers; // 【新增】用于掉落物品
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SimpleFoodBlock extends BaseEntityBlock {

    // 优化平底锅的碰撞体积，更精确地匹配模型
    private static final VoxelShape BOTTOM_SHAPE = Block.box(2, 0, 2, 14, 1, 14); // 底部
    private static final VoxelShape NORTH_WALL = Block.box(2, 1, 2, 14, 4, 3); // 北墙
    private static final VoxelShape SOUTH_WALL = Block.box(2, 1, 13, 14, 4, 14); // 南墙
    private static final VoxelShape WEST_WALL = Block.box(2, 1, 3, 3, 4, 13); // 西墙
    private static final VoxelShape EAST_WALL = Block.box(13, 1, 3, 14, 4, 13); // 东墙
    private static final VoxelShape SHAPE = Shapes.or(BOTTOM_SHAPE, NORTH_WALL, SOUTH_WALL, WEST_WALL, EAST_WALL);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    public SimpleFoodBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PotBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        // === 1. 蹲下拿锅逻辑 ===
        if (pPlayer.isShiftKeyDown() && pPlayer.getItemInHand(pHand).isEmpty()) {
            if (!pLevel.isClientSide) {
                ItemStack potItem = new ItemStack(this);

                BlockEntity be = pLevel.getBlockEntity(pPos);
                if (be instanceof PotBlockEntity pot) {
                    // 1. 保存数据到物品
                    pot.saveToItem(potItem);

                    // 2. 【核心修复】清空方块里的物品
                    // 为什么？因为下面调用 removeBlock 会触发 onRemove 方法
                    // 如果不清空，onRemove 会以为方块被破坏了，又把食物掉落一次，导致物品翻倍！
                    pot.getItems().clear();
                }

                // 3. 销毁方块 (会触发 onRemove)
                pLevel.removeBlock(pPos, false);

                // 4. 给玩家锅
                pPlayer.setItemInHand(pHand, potItem);
                pLevel.playSound(null, pPos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // === 2. 正常放入/取出逻辑 ===
        if (!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof PotBlockEntity pot) {
                ItemStack handItem = pPlayer.getItemInHand(pHand);

                if (!handItem.isEmpty()) {
                    if (handItem.getItem().isEdible()) {
                        boolean success = pot.pushItem(handItem);
                        if (success) {
                            pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                            if (!pPlayer.isCreative()) {
                                handItem.shrink(1);
                            }
                        }
                    }
                } else {
                    ItemStack takenItem = pot.popItem();
                    if (!takenItem.isEmpty()) {
                        boolean added = pPlayer.getInventory().add(takenItem);
                        if (!added || !takenItem.isEmpty()) {
                            pPlayer.drop(takenItem, false);
                        }
                        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // 【新增】方块被破坏时的逻辑 (onRemove)
    // =========================================================
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        // 如果方块真的被移除了（而不是仅仅修改了状态属性）
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PotBlockEntity) {
                // 使用 Minecraft 原生方法，把容器里的所有东西都喷出来
                Containers.dropContents(pLevel, pPos, ((PotBlockEntity) blockEntity).getItems());
                // 注意：这里不需要手动 updateNeighbourShapes，super.onRemove 会处理
            }
        }
        // 必须调用父类方法，否则 BlockEntity 不会被正确移除
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