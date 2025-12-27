package com.fyoyi.betterfood.event;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.CookednessHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModServerEvents {

    // 1. 玩家背包检查 (保持不变)
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

    // 2. 全局扫描 (保持不变)
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide && event.level instanceof ServerLevel serverLevel) {
            if (event.level.getGameTime() % 40 == 0) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof ItemEntity itemEntity) {
                        ItemStack stack = itemEntity.getItem();

                        if (FoodConfig.canRot(stack)) {
                            if (TimeManager.DECAY_ENABLED && FreshnessHelper.isRotten(serverLevel, stack)) {
                                // 根据食物点决定过期后变成什么
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

    // 3. 打开箱子检查 (保持不变)
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
                // 根据食物点决定过期后变成什么
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
                    // 根据食物点决定过期后变成什么
                    ItemStack newItem = getRottenItemByTags(stack);
                    container.setItem(i, new ItemStack(newItem.getItem(), stack.getCount()));
                    container.setChanged();
                } else {
                    FreshnessHelper.getExpiryTime(level, stack, true);
                }
            }
        }
    }

    /**
     * 根据食物点决定过期后变成什么
     * @param stack 原始食物
     * @return 过期后的新物品
     */
    private static ItemStack getRottenItemByTags(ItemStack stack) {
        Set<String> tags = FoodConfig.getFoodTags(stack);

        // 检查分类属性
        for (String tag : tags) {
            if (tag.startsWith("分类:")) {
                String classification = tag.substring(3); // 去掉"分类:"前缀
                if ("蔬菜".equals(classification) || "水果".equals(classification) || "谷物".equals(classification)) {
                    // 蔬菜、水果、谷物类食物变成骨粉
                    return new ItemStack(Items.BONE_MEAL, stack.getCount());
                } else if ("肉类".equals(classification) || "鱼类".equals(classification)) {
                    // 肉类、鱼类食物变成腐肉
                    return new ItemStack(Items.ROTTEN_FLESH, stack.getCount());
                } else if ("汤食".equals(classification)) {
                    // 汤食类食物变成碗
                    return new ItemStack(Items.BOWL, stack.getCount());
                } else if ("饮品".equals(classification)) {
                    // 饮品类食物变成空玻璃瓶
                    return new ItemStack(Items.GLASS_BOTTLE, stack.getCount());
                }
            }
        }

        // 默认变成腐肉
        return new ItemStack(Items.ROTTEN_FLESH, stack.getCount());
    }

    // ==========================================================
    // 4. 堆叠逻辑 (最终修复版：加入熟度检测)
    // ==========================================================
    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        if (!TimeManager.DECAY_ENABLED) return;

        ItemStack cursorStack = event.getCarriedItem();
        ItemStack slotStack = event.getStackedOnItem();
        ClickAction action = event.getClickAction();
        net.minecraft.world.level.Level level = event.getPlayer().level();

        // 1. 基本检查
        if (FoodConfig.canRot(cursorStack) &&
                FoodConfig.canRot(slotStack) &&
                ItemStack.isSameItem(cursorStack, slotStack)) {

            // >>> 【核心修改】检查熟度是否一致 <<<
            // 如果熟度不一致，直接 return，禁止进入后续的新鲜度合并逻辑。
            // 这样 Minecraft 原版机制就会因为 NBT 不同而禁止它们堆叠。
            float cooked1 = CookednessHelper.getCurrentCookedness(cursorStack);
            float cooked2 = CookednessHelper.getCurrentCookedness(slotStack);

            // 允许极小的浮点误差 (0.01)，如果差值过大，视为不同熟度
            if (Math.abs(cooked1 - cooked2) > 0.01f) {
                return;
            }
            // >>> 修改结束 <<<

            // 2. 如果新鲜度（NBT）完全一样
            if (ItemStack.isSameItemSameTags(cursorStack, slotStack)) {
                return;
            }

            // --- 以下处理：新鲜度不同 (NBT不同) 但熟度相同的情况 ---

            // 3. 左键 (PRIMARY) -> 交换
            if (action == ClickAction.PRIMARY) {
                // 直接返回！
                // 原版检测到 NBT 不同，会执行"交换"操作。
                return;
            }

            // 4. 右键 (SECONDARY) -> 全部合并 (Merge All)
            if (action == ClickAction.SECONDARY) {
                int maxStack = slotStack.getMaxStackSize();
                int currentSlotCount = slotStack.getCount();
                int space = maxStack - currentSlotCount;

                // 如果满了，不处理（原版通常也无反应）
                if (space <= 0) return;

                // 核心修改：尽可能多放 (Math.min(cursor, space))
                int amountToMove = Math.min(cursorStack.getCount(), space);

                if (amountToMove > 0) {
                    long expiryCursor = FreshnessHelper.getExpiryTime(level, cursorStack, true);
                    long expirySlot = FreshnessHelper.getExpiryTime(level, slotStack, true);

                    // 加权平均 (所有移入的 + 原有的)
                    long weightSlot = expirySlot * currentSlotCount;
                    long weightIncoming = expiryCursor * amountToMove;
                    long newAverageExpiry = (weightSlot + weightIncoming) / (currentSlotCount + amountToMove);

                    // 修改物品
                    slotStack.grow(amountToMove);
                    FreshnessHelper.setExpiryTime(slotStack, newAverageExpiry);

                    cursorStack.shrink(amountToMove);

                    // 拦截原版交换逻辑
                    event.setCanceled(true);

                    // 强制刷新，防止鬼畜
                    event.getSlot().setChanged();
                    if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        serverPlayer.containerMenu.sendAllDataToRemote();
                    }
                }
            }
        }
    }

    /**
     * 判断两个食物是否属于相同新鲜度等级
     * @param level 世界对象
     * @param stack1 食物1
     * @param stack2 食物2
     * @return 是否属于相同新鲜度等级
     */
    private static boolean isSameFreshnessGrade(net.minecraft.world.level.Level level, ItemStack stack1, ItemStack stack2) {
        float percent1 = FreshnessHelper.getFreshnessPercentage(level, stack1);
        float percent2 = FreshnessHelper.getFreshnessPercentage(level, stack2);

        // 根据新鲜度百分比判断所属等级
        // 0.8, 0.5, 0.3, 0.1
        return getFreshnessGrade(percent1) == getFreshnessGrade(percent2);
    }

    /**
     * 根据新鲜度百分比获取新鲜度等级
     * @param percent 新鲜度百分比
     * @return 新鲜度等级 (0-4)
     */
    private static int getFreshnessGrade(float percent) {
        if (percent >= 0.8f) {
            return 4; // 新鲜
        } else if (percent >= 0.5f) {
            return 3; // 不新鲜
        } else if (percent >= 0.3f) {
            return 2; // 略微变质
        } else if (percent >= 0.1f) {
            return 1; // 变质
        } else {
            return 0; // 严重变质
        }
    }
}