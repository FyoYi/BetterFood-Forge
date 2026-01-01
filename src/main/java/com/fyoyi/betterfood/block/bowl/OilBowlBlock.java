package com.fyoyi.betterfood.block.bowl;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OilBowlBlock extends HorizontalDirectionalBlock {

    // 1=最少, 4=满
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 4);

    // 碰撞箱 (保持不变)
    private static final VoxelShape BASE = Block.box(2, 0, 2, 14, 1, 14);
    private static final VoxelShape WALL_WEST = Block.box(1, 0, 1, 2, 3, 15);
    private static final VoxelShape WALL_EAST = Block.box(14, 0, 1, 15, 3, 15);
    private static final VoxelShape WALL_NORTH = Block.box(2, 0, 1, 14, 3, 2);
    private static final VoxelShape WALL_SOUTH = Block.box(2, 0, 14, 14, 3, 15);
    private static final VoxelShape SHAPE = Shapes.or(BASE, WALL_WEST, WALL_EAST, WALL_NORTH, WALL_SOUTH);

    public OilBowlBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LAYERS, 4));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        int currentLayers = state.getValue(LAYERS);

        // 检查铲子是否沾油
        boolean isSpatula = stack.getItem() == ModItems.SPATULA.get();
        boolean isOily = isSpatula && stack.hasTag() && stack.getTag().getBoolean("IsOily");

        // === 场景 1：用【干净】铲子挖油 ===
        if (isSpatula && !isOily) {
            if (!level.isClientSide) {
                // 1. 设置铲子为沾油 (写入 NBT)
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean("IsOily", true);
                // stack.setTag(tag); // getOrCreateTag 直接操作引用，无需 setTag

                // 2. 减少方块油量
                if (currentLayers > 1) {
                    level.setBlock(pos, state.setValue(LAYERS, currentLayers - 1), 3);
                } else {
                    // 没油了，变空碗
                    BlockState emptyBowlState = ModBlocks.PLACED_BOWL.get().defaultBlockState()
                            .setValue(FACING, state.getValue(FACING));
                    level.setBlock(pos, emptyBowlState, 3);
                }

                level.playSound(null, pos, SoundEvents.MUD_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        // === 场景 2：用【沾油】铲子放回油 ===
        if (isSpatula && isOily) {
            if (currentLayers < 4) { // 没满才能放
                if (!level.isClientSide) {
                    // 1. 设置铲子为干净 (移除 NBT)
                    if (stack.hasTag()) {
                        stack.getTag().remove("IsOily");
                    }

                    // 2. 增加方块油量
                    level.setBlock(pos, state.setValue(LAYERS, currentLayers + 1), 3);

                    level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return InteractionResult.SUCCESS;
            }
        }

        // === 场景 3: 蹲下捡起 ===
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack oilBowlItem = new ItemStack(ModItems.OIL_BOWL.get());

                int damage = 4 - currentLayers;
                oilBowlItem.setDamageValue(damage);

                if (!player.getInventory().add(oilBowlItem)) player.drop(oilBowlItem, false);

                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // === 放置逻辑：继承物品耐久 ===
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        int damage = stack.getDamageValue(); // 0-3

        int layers = 4 - damage;
        // 容错处理
        if (layers < 1) layers = 1;
        if (layers > 4) layers = 4;

        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LAYERS, layers);
    }

    // 中键复制/挖掘掉落 (默认给满油的)
    // 如果你想挖掘也掉落对应损耗的，需要写 LootTable 或者在这里特殊处理 (通常 LootTable 不好处理动态耐久)
    // 既然我们有“蹲下捡起”逻辑，直接破坏掉落满油的或者不掉落也行。
    // 为了简单，这里 getCloneItemStack 返回满油。
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.OIL_BOWL.get());
    }

    // 破坏方块时的掉落逻辑需要修改 ModServerEvents 或者 LootTable
    // 建议：玩家尽量养成用右键拾取的习惯。
    // 如果强制要求打碎掉落带耐久的，需要重写 getDrops (比较麻烦) 或者用 LootTable Function。

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAYERS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}