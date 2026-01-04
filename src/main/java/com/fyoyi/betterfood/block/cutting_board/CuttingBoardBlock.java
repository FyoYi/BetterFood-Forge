package com.fyoyi.betterfood.block.cutting_board;

import com.fyoyi.betterfood.block.entity.CuttingBoardBlockEntity;
import com.fyoyi.betterfood.item.ModItems;
import com.fyoyi.betterfood.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) { return InteractionResult.PASS; }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof CuttingBoardBlockEntity cuttingBoard)) { return InteractionResult.FAIL; }
        ItemStack handStack = pPlayer.getItemInHand(pHand);

        if (pLevel.isClientSide) {
            return (!cuttingBoard.isEmpty() || !handStack.isEmpty()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (handStack.isEmpty() && pPlayer.isShiftKeyDown()) {
            ItemStack boardStack = new ItemStack(this);
            cuttingBoard.saveToItem(boardStack);
            if (!pPlayer.getInventory().add(boardStack)) { pPlayer.drop(boardStack, false); }
            cuttingBoard.clearContent();
            pLevel.removeBlock(pPos, false);
            pLevel.playSound(null, pPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        if (!handStack.isEmpty() && handStack.is(ModTags.Items.CUTTABLE_FOODS) && cuttingBoard.isEmpty()) {
            ItemStack itemToPlace = pPlayer.getAbilities().instabuild ? handStack.copy() : handStack.split(1);
            itemToPlace.setCount(1);
            cuttingBoard.setItem(itemToPlace);
            pLevel.playSound(null, pPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 0.8f);
            return InteractionResult.SUCCESS;
        }

        if (handStack.isEmpty() && !cuttingBoard.isEmpty()) {
            ItemStack takenItem = cuttingBoard.removeItem();
            if (!pPlayer.getInventory().add(takenItem)) { pPlayer.drop(takenItem, false); }
            pLevel.playSound(null, pPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        if (handStack.getItem() == ModItems.KNIFE.get() && !cuttingBoard.isEmpty()) {
            boolean success = cuttingBoard.processCutting(pPlayer);
            if (success) {
                pLevel.playSound(null, pPos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.2f);
                if (!pPlayer.getAbilities().instabuild) {
                    handStack.hurtAndBreak(1, pPlayer, (player) -> player.broadcastBreakEvent(pHand));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) { super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving); }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) { return new CuttingBoardBlockEntity(pPos, pState); }
    @Override public RenderShape getRenderShape(BlockState pState) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) { Direction facing = pState.getValue(FACING); return (facing == Direction.EAST || facing == Direction.WEST) ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext pContext) { return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) { pBuilder.add(FACING); }
}