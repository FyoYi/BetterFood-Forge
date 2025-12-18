package com.fyoyi.betterfood.block.lid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LidBlock extends HorizontalDirectionalBlock {
    // 定义锅盖的碰撞体积
    private static final VoxelShape BOTTOM_SHAPE = Block.box(1, 0, 1, 15, 1, 15); // 底部
    private static final VoxelShape NORTH_WALL = Block.box(1, 1, 1, 15, 2, 2); // 北墙
    private static final VoxelShape SOUTH_WALL = Block.box(1, 1, 14, 15, 2, 15); // 南墙
    private static final VoxelShape WEST_WALL = Block.box(1, 1, 2, 2, 2, 14); // 西墙
    private static final VoxelShape EAST_WALL = Block.box(14, 1, 2, 15, 2, 14); // 东墙
    private static final VoxelShape SHAPE = Shapes.or(BOTTOM_SHAPE, NORTH_WALL, SOUTH_WALL, WEST_WALL, EAST_WALL);
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public LidBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 蹲下右键拾取锅盖
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!level.isClientSide) {
                // 移除方块
                level.removeBlock(pos, false);
                
                // 给玩家锅盖物品
                ItemStack lidItem = new ItemStack(this);
                if (!player.getInventory().add(lidItem)) {
                    player.drop(lidItem, false);
                }
                
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
}