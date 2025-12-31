package com.fyoyi.betterfood.event;

import com.fyoyi.betterfood.block.ModBlocks; // 【必须导入】
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.CookednessHelper;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent; // 【必须导入】
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModServerEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 1. 检查手里是不是原版碗
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == Items.BOWL) {
            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            Direction face = event.getFace();

            // 2. 必须点击方块的顶面 (UP)
            if (face == Direction.UP) {
                BlockPos placePos = pos.above();

                // 3. 检查上方是否是空气或可替换方块（比如草）
                // 并且检查有没有东西挡住（比如玩家站在上面）
                if (level.getBlockState(placePos).canBeReplaced() && level.isUnobstructed(level.getBlockState(placePos), placePos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {

                    // 4. 只在服务端执行放置，客户端返回成功以显示手臂动画
                    if (!level.isClientSide) {
                        // 获取玩家朝向，让碗面向玩家
                        Direction facing = event.getEntity().getDirection().getOpposite();

                        // 获取放置碗的方块状态
                        BlockState newState = ModBlocks.PLACED_BOWL.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, facing);

                        // 放置方块
                        level.setBlock(placePos, newState, 3);

                        // 播放音效
                        level.playSound(null, placePos, net.minecraft.sounds.SoundEvents.WOOD_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

                        // 扣除物品 (生存模式)
                        if (!event.getEntity().isCreative()) {
                            stack.shrink(1);
                        }
                    }

                    // 5. 拦截事件，防止触发原版逻辑
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player.level().getGameTime() % 20 == 0) {
                checkInventoryForRot(event.player.level(), event.player.getInventory());
                AbstractContainerMenu menu = event.player.containerMenu;
                if (menu != null) {
                    for (Slot slot : menu.slots) {
                        if (slot.hasItem()) checkAndReplace(event.player.level(), slot);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide && event.level instanceof ServerLevel serverLevel) {
            if (event.level.getGameTime() % 40 == 0) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof ItemEntity itemEntity) {
                        ItemStack stack = itemEntity.getItem();
                        if (FoodConfig.canRot(stack)) {
                            if (TimeManager.DECAY_ENABLED && FreshnessHelper.isRotten(serverLevel, stack)) {
                                ItemStack newItem = getRottenItemByTags(stack);
                                itemEntity.setItem(new ItemStack(newItem.getItem(), stack.getCount()));
                            } else {
                                FreshnessHelper.getExpiryTime(serverLevel, stack, true);
                            }
                        }
                    }
                }
                Set<ChunkPos> processedChunks = new HashSet<>();
                for (Player player : serverLevel.players()) {
                    ChunkPos pPos = player.chunkPosition();
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            ChunkPos cPos = new ChunkPos(pPos.x + x, pPos.z + z);
                            if (processedChunks.add(cPos) && serverLevel.hasChunk(cPos.x, cPos.z)) {
                                processChunkBlockEntities(serverLevel, serverLevel.getChunk(cPos.x, cPos.z));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void processChunkBlockEntities(ServerLevel level, LevelChunk chunk) {
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        for (BlockEntity be : blockEntities.values()) {
            if (be instanceof Container container) {
                checkInventoryForRot(level, container);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!event.getEntity().level().isClientSide) {
            AbstractContainerMenu menu = event.getContainer();
            for (Slot slot : menu.slots) {
                if (slot.hasItem()) checkAndReplace(event.getEntity().level(), slot);
            }
        }
    }

    private static void checkAndReplace(net.minecraft.world.level.Level level, Slot slot) {
        ItemStack stack = slot.getItem();
        if (FoodConfig.canRot(stack)) {
            if (TimeManager.DECAY_ENABLED && FreshnessHelper.isRotten(level, stack)) {
                ItemStack newItem = getRottenItemByTags(stack);
                slot.set(new ItemStack(newItem.getItem(), stack.getCount()));
            } else {
                FreshnessHelper.getExpiryTime(level, stack, true);
            }
        }
    }

    private static void checkInventoryForRot(net.minecraft.world.level.Level level, Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (FoodConfig.canRot(stack)) {
                if (TimeManager.DECAY_ENABLED && FreshnessHelper.isRotten(level, stack)) {
                    ItemStack newItem = getRottenItemByTags(stack);
                    container.setItem(i, new ItemStack(newItem.getItem(), stack.getCount()));
                    container.setChanged();
                } else {
                    FreshnessHelper.getExpiryTime(level, stack, true);
                }
            }
        }
    }

    private static ItemStack getRottenItemByTags(ItemStack stack) {
        Set<String> tags = FoodConfig.getFoodTags(stack);
        for (String tag : tags) {
            if (tag.startsWith("分类:")) {
                String classification = tag.substring(3);
                if ("蔬菜".equals(classification) || "水果".equals(classification) || "谷物".equals(classification)) {
                    return new ItemStack(Items.BONE_MEAL, stack.getCount());
                } else if ("肉类".equals(classification) || "鱼类".equals(classification)) {
                    return new ItemStack(Items.ROTTEN_FLESH, stack.getCount());
                } else if ("汤食".equals(classification)) {
                    return new ItemStack(Items.BOWL, stack.getCount());
                } else if ("饮品".equals(classification)) {
                    return new ItemStack(Items.GLASS_BOTTLE, stack.getCount());
                }
            }
        }
        return new ItemStack(Items.ROTTEN_FLESH, stack.getCount());
    }

    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        if (!TimeManager.DECAY_ENABLED) return;

        ItemStack cursorStack = event.getCarriedItem();
        ItemStack slotStack = event.getStackedOnItem();
        ClickAction action = event.getClickAction();
        net.minecraft.world.level.Level level = event.getPlayer().level();

        if (FoodConfig.canRot(cursorStack) && FoodConfig.canRot(slotStack) && ItemStack.isSameItem(cursorStack, slotStack)) {
            float cooked1 = CookednessHelper.getCurrentCookedness(cursorStack);
            float cooked2 = CookednessHelper.getCurrentCookedness(slotStack);
            if (Math.abs(cooked1 - cooked2) > 0.01f) return;

            if (ItemStack.isSameItemSameTags(cursorStack, slotStack)) return;

            if (action == ClickAction.PRIMARY) return;

            if (action == ClickAction.SECONDARY) {
                int maxStack = slotStack.getMaxStackSize();
                int currentSlotCount = slotStack.getCount();
                int space = maxStack - currentSlotCount;
                if (space <= 0) return;

                int amountToMove = Math.min(cursorStack.getCount(), space);
                if (amountToMove > 0) {
                    long expiryCursor = FreshnessHelper.getExpiryTime(level, cursorStack, true);
                    long expirySlot = FreshnessHelper.getExpiryTime(level, slotStack, true);
                    long weightSlot = expirySlot * currentSlotCount;
                    long weightIncoming = expiryCursor * amountToMove;
                    long newAverageExpiry = (weightSlot + weightIncoming) / (currentSlotCount + amountToMove);

                    slotStack.grow(amountToMove);
                    FreshnessHelper.setExpiryTime(slotStack, newAverageExpiry);
                    cursorStack.shrink(amountToMove);
                    event.setCanceled(true);
                    event.getSlot().setChanged();
                    if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        serverPlayer.containerMenu.sendAllDataToRemote();
                    }
                }
            }
        }
    }
}