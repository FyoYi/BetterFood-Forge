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
                if (level.getBlockState(placePos).canBeReplaced() && level.isUnobstructed(level.getBlockState(placePos),
                        placePos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {

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
                        level.playSound(null, placePos, net.minecraft.sounds.SoundEvents.WOOD_PLACE,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

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
                        if (slot.hasItem())
                            checkAndReplace(event.player.level(), slot);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide
                && event.level instanceof ServerLevel serverLevel) {
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
                if (slot.hasItem())
                    checkAndReplace(event.getEntity().level(), slot);
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
        if (!TimeManager.DECAY_ENABLED)
            return;

        ItemStack cursorStack = event.getCarriedItem();
        ItemStack slotStack = event.getStackedOnItem();
        ClickAction action = event.getClickAction();
        net.minecraft.world.level.Level level = event.getPlayer().level();

        if (FoodConfig.canRot(cursorStack) && FoodConfig.canRot(slotStack)
                && ItemStack.isSameItem(cursorStack, slotStack)) {
            float cooked1 = CookednessHelper.getCurrentCookedness(cursorStack);
            float cooked2 = CookednessHelper.getCurrentCookedness(slotStack);
            if (Math.abs(cooked1 - cooked2) > 0.01f)
                return;

            if (ItemStack.isSameItemSameTags(cursorStack, slotStack))
                return;

            if (action == ClickAction.PRIMARY)
                return;

            if (action == ClickAction.SECONDARY) {
                int maxStack = slotStack.getMaxStackSize();
                int currentSlotCount = slotStack.getCount();
                int space = maxStack - currentSlotCount;
                if (space <= 0)
                    return;

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

    @SubscribeEvent
    public static void onEntityItemPickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (!TimeManager.DECAY_ENABLED)
            return;

        Player player = event.getEntity();
        ItemEntity itemEntity = event.getItem();
        ItemStack pickedStack = itemEntity.getItem();
        Level level = player.level();

        if (!FoodConfig.canRot(pickedStack))
            return;

        // 查找背包中是否有可以堆叠的相同食物
        boolean foundMatch = false;
        ItemStack remainingStack = pickedStack.copy();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (remainingStack.isEmpty())
                break;

            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.isEmpty())
                continue;

            if (FoodConfig.canRot(slotStack) && ItemStack.isSameItem(remainingStack, slotStack)) {
                float cookedPicked = CookednessHelper.getCurrentCookedness(remainingStack);
                float cookedSlot = CookednessHelper.getCurrentCookedness(slotStack);
                if (Math.abs(cookedPicked - cookedSlot) > 0.01f)
                    continue; // 熟度不同，不堆叠

                float freshPicked = FreshnessHelper.getFreshnessPercentage(level, remainingStack);
                float freshSlot = FreshnessHelper.getFreshnessPercentage(level, slotStack);
                if (Math.abs(freshPicked - freshSlot) > 0.1f)
                    continue; // 新鲜度差距超过10%，不堆叠

                // 可以堆叠，计算平均新鲜度
                int pickedCount = remainingStack.getCount();
                int slotCount = slotStack.getCount();
                int maxStack = slotStack.getMaxStackSize();
                int space = maxStack - slotCount;
                if (space <= 0)
                    continue;

                int amountToMove = Math.min(pickedCount, space);
                long expiryPicked = FreshnessHelper.getExpiryTime(level, remainingStack, true);
                long expirySlot = FreshnessHelper.getExpiryTime(level, slotStack, true);
                long weightSlot = expirySlot * slotCount;
                long weightIncoming = expiryPicked * amountToMove;
                long newAverageExpiry = (weightSlot + weightIncoming) / (slotCount + amountToMove);

                slotStack.grow(amountToMove);
                FreshnessHelper.setExpiryTime(slotStack, newAverageExpiry);
                remainingStack.shrink(amountToMove);

                foundMatch = true;
            }
        }

        // 如果找到匹配并成功堆叠
        if (foundMatch) {
            if (remainingStack.isEmpty()) {
                // 完全堆叠，移除地上的物品实体，取消默认拾取
                itemEntity.discard();
                event.setCanceled(true);
            } else {
                // 部分堆叠，更新地上物品实体的数量
                itemEntity.setItem(remainingStack);
                event.setCanceled(true);
            }
        }
    }

    private static void mergeSimilarFoodSlots(Player player, Level level) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack1 = player.getInventory().getItem(i);
            if (stack1.isEmpty() || !FoodConfig.canRot(stack1))
                continue;

            for (int j = i + 1; j < player.getInventory().getContainerSize(); j++) {
                ItemStack stack2 = player.getInventory().getItem(j);
                if (stack2.isEmpty() || !ItemStack.isSameItem(stack1, stack2))
                    continue;

                float cooked1 = CookednessHelper.getCurrentCookedness(stack1);
                float cooked2 = CookednessHelper.getCurrentCookedness(stack2);
                if (Math.abs(cooked1 - cooked2) > 0.01f)
                    continue;

                float fresh1 = FreshnessHelper.getFreshnessPercentage(level, stack1);
                float fresh2 = FreshnessHelper.getFreshnessPercentage(level, stack2);
                if (Math.abs(fresh1 - fresh2) > 0.1f)
                    continue;

                // 可以合并，计算平均新鲜度
                int count1 = stack1.getCount();
                int count2 = stack2.getCount();
                int totalCount = count1 + count2;
                int maxStack = stack1.getMaxStackSize();

                if (totalCount <= maxStack) {
                    // 完全合并到stack1
                    long expiry1 = FreshnessHelper.getExpiryTime(level, stack1, true);
                    long expiry2 = FreshnessHelper.getExpiryTime(level, stack2, true);
                    long weight1 = expiry1 * count1;
                    long weight2 = expiry2 * count2;
                    long newAverageExpiry = (weight1 + weight2) / totalCount;

                    stack1.setCount(totalCount);
                    FreshnessHelper.setExpiryTime(stack1, newAverageExpiry);
                    player.getInventory().setItem(j, ItemStack.EMPTY);
                } else {
                    // 部分合并
                    int space = maxStack - count1;
                    int amountToMove = Math.min(space, count2);

                    long expiry1 = FreshnessHelper.getExpiryTime(level, stack1, true);
                    long expiry2 = FreshnessHelper.getExpiryTime(level, stack2, true);
                    long weight1 = expiry1 * count1;
                    long weight2 = expiry2 * amountToMove;
                    long newAverageExpiry = (weight1 + weight2) / (count1 + amountToMove);

                    stack1.setCount(maxStack);
                    FreshnessHelper.setExpiryTime(stack1, newAverageExpiry);
                    stack2.shrink(amountToMove);
                }
            }
        }
    }
}