package com.fyoyi.betterfood.block.cutting_board;

import com.fyoyi.betterfood.block.entity.CuttingBoardBlockEntity;
import com.fyoyi.betterfood.block.entity.ModBlockEntities;
import com.fyoyi.betterfood.item.ModItems;
import com.fyoyi.betterfood.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CuttingBoardBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(1, 0, 3, 15, 1, 13);
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(3, 0, 1, 13, 1, 15);

    public CuttingBoardBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
            BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.CUTTING_BOARD_BE.get(),
                CuttingBoardBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
            BlockHitResult pHit) {
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof CuttingBoardBlockEntity cuttingBoard))
            return InteractionResult.FAIL;

        ItemStack handStack = pPlayer.getItemInHand(pHand);

        // 1. 蹲下 + 空手 = 拿起整个菜板
        if (handStack.isEmpty() && pPlayer.isShiftKeyDown()) {
            if (!pLevel.isClientSide) {
                ItemStack boardStack = new ItemStack(this);
                cuttingBoard.saveToItem(boardStack);
                if (!pPlayer.getInventory().add(boardStack))
                    pPlayer.drop(boardStack, false);
                cuttingBoard.clearContent();
                pLevel.removeBlock(pPos, false);
                pLevel.playSound(null, pPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. 产物收取逻辑 (优先) -> 变为掉落物
        if (!cuttingBoard.isEmpty() && !cuttingBoard.isRawIngredientState()) {
            if (!pLevel.isClientSide) {
                // [修改] 调用无参方法，掉落到地上
                cuttingBoard.dropAllItems();
                pLevel.playSound(null, pPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        // 3. 刀具交互
        if (handStack.getItem() == ModItems.KNIFE.get()) {
            boolean didInteract = cuttingBoard.interactKnife(pPlayer, handStack);
            if (didInteract && !pLevel.isClientSide) {
                if (!pPlayer.getAbilities().instabuild) {
                    handStack.hurtAndBreak(1, pPlayer, (p) -> p.broadcastBreakEvent(pHand));
                }
            }
            return didInteract ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        // 4. 空手拿回原料 -> 变为掉落物 (保持行为一致)
        if (handStack.isEmpty() && !cuttingBoard.isEmpty()) {
            if (!pLevel.isClientSide) {
                // [修改] 同样改为掉落
                cuttingBoard.dropAllItems();
                pLevel.playSound(null, pPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        // 5. 放置原料
        if (!handStack.isEmpty() && handStack.is(ModTags.Items.CUTTABLE_FOODS)) {
            if (cuttingBoard.addItem(handStack.copy().split(1))) {
                if (!pLevel.isClientSide) {
                    if (!pPlayer.getAbilities().instabuild)
                        handStack.shrink(1);
                    pLevel.playSound(null, pPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 0.8f);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pLevel.isClientSide && pState.getBlock() != pNewState.getBlock()) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof Container) {
                Containers.dropContents(pLevel, pPos, (Container) be);
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CuttingBoardBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction facing = pState.getValue(FACING);
        return (facing == Direction.EAST || facing == Direction.WEST) ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH;
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