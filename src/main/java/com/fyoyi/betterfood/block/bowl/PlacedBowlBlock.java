package com.fyoyi.betterfood.block.bowl;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlacedBowlBlock extends HorizontalDirectionalBlock {

    // 碰撞箱 (10x10, 高3)
    private static final VoxelShape BASE = Block.box(2, 0, 2, 14, 1, 14);
    private static final VoxelShape WALL_WEST = Block.box(1, 0, 1, 2, 3, 15);
    private static final VoxelShape WALL_EAST = Block.box(14, 0, 1, 15, 3, 15);
    private static final VoxelShape WALL_NORTH = Block.box(2, 0, 1, 14, 3, 2);
    private static final VoxelShape WALL_SOUTH = Block.box(2, 0, 14, 14, 3, 15);
    private static final VoxelShape SHAPE = Shapes.or(BASE, WALL_WEST, WALL_EAST, WALL_NORTH, WALL_SOUTH);

    public PlacedBowlBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // === 【新增】蹲下右键捡起碗 ===
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack stack = pPlayer.getItemInHand(pHand);

        if (stack.getItem() == ModItems.SPATULA.get() &&
                stack.hasTag() && stack.getTag().getBoolean("IsOily")) {

            if (!pLevel.isClientSide) {
                // 1. 变成 1层油的油碗
                BlockState oilyState = ModBlocks.OIL_BOWL.get().defaultBlockState()
                        .setValue(FACING, pState.getValue(FACING))
                        .setValue(OilBowlBlock.LAYERS, 1);

                pLevel.setBlock(pPos, oilyState, 3);

                // 2. 铲子变干净 (移除 NBT)
                stack.getTag().remove("IsOily");

                pLevel.playSound(null, pPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // 判定条件：蹲下 + 空手
        if (pPlayer.isShiftKeyDown() && pPlayer.getItemInHand(pHand).isEmpty()) {
            if (!pLevel.isClientSide) {
                // 1. 创建碗的物品
                ItemStack bowlItem = new ItemStack(Items.BOWL);

                // 2. 尝试放入玩家背包，放不下就扔在地上
                if (!pPlayer.getInventory().add(bowlItem)) {
                    pPlayer.drop(bowlItem, false);
                } else {
                    // 如果成功放入背包，播放“啵”的一声捡起音效
                    pLevel.playSound(null, pPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
                }

                // 3. 销毁地上的方块 (不掉落，因为上面已经手动给物品了)
                pLevel.removeBlock(pPos, false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.BOWL);
    }
}