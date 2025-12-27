package com.fyoyi.betterfood.config;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class FoodConfig {

    public static final long TICKS_PER_DAY = 24000L;
    public static final long SHELF_LIFE_DEFAULT = 7 * TICKS_PER_DAY;
    public static final long SHELF_LIFE_INFINITE = -1L;

    // === 预设时间常量 (分钟) ===
    // 这里的单位是分钟，方便指令调用
    public static final float MIN_MINUTES = (2.5f * 20); // 50分钟
    public static final float SHORT_MINUTES = (5.0f * 20); // 100分钟
    public static final float MEDIUM_MINUTES = (12.0f * 20); // 240分钟
    public static final float LONG_MINUTES = (50.0f * 20); // 1000分钟

    private static final Map<Item, Long> CUSTOM_LIFETIMES = new HashMap<>();
    private static final Map<Item, Set<String>> FOOD_TAGS = new HashMap<>();
    private static final Map<Item, List<EffectBonus>> BONUS_EFFECTS = new HashMap<>();

    // === 奖励效果数据类 ===
    public static class EffectBonus {
        public final MobEffect effect;
        public final float chance;
        public final int durationSeconds;
        public final int amplifier;

        public EffectBonus(MobEffect effect, float chance, int durationSeconds, int amplifier) {
            this.effect = effect;
            this.chance = chance;
            this.durationSeconds = durationSeconds;
            this.amplifier = amplifier;
        }
    }

    public static void clear() {
        CUSTOM_LIFETIMES.clear();
        FOOD_TAGS.clear();
        BONUS_EFFECTS.clear();
    }

    public static void register(Item item, long ticks) {
        CUSTOM_LIFETIMES.put(item, ticks);
    }

    // === 【新增】注册奖励效果 ===
    public static void registerBonus(Item item, List<EffectBonus> effects) {
        BONUS_EFFECTS.put(item, effects);
    }

    // === 【新增】注册食物标签 ===
    public static void registerTags(Item item, Set<String> tags) {
        FOOD_TAGS.put(item, tags);
    }

    // === 【新增】移除配置 ===
    public static void remove(Item item) {
        CUSTOM_LIFETIMES.remove(item);
        FOOD_TAGS.remove(item);
        BONUS_EFFECTS.remove(item);
    }

    public static long getItemLifetime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (CUSTOM_LIFETIMES.containsKey(stack.getItem())) {
            return CUSTOM_LIFETIMES.get(stack.getItem());
        }
        return SHELF_LIFE_DEFAULT;
    }

    public static boolean canRot(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // 腐肉永远不会腐烂
        if (stack.getItem() == Items.ROTTEN_FLESH) return false;
        
        // 牛奶暂时不计入食物
        if (stack.getItem() == Items.MILK_BUCKET) return false;
        
        // 只有在配置中有自定义保质期的食物才可以腐烂
        if (CUSTOM_LIFETIMES.containsKey(stack.getItem())) return true;
        
        // 如果没有自定义保质期，但物品是可食用的，也可以腐烂
        return stack.getItem().isEdible();
    }
    
    public static long getItemLifetime(Item item) {
        // 牛奶暂时不计入食物
        if (item == Items.MILK_BUCKET) return SHELF_LIFE_INFINITE;
        
        if (CUSTOM_LIFETIMES.containsKey(item)) {
            return CUSTOM_LIFETIMES.get(item);
        }
        // 如果没有自定义保质期，但物品是可食用的，返回默认保质期
        return SHELF_LIFE_DEFAULT;
    }
    
    /**
     * 检查物品是否有自定义保质期
     */
    public static boolean hasCustomLifetime(Item item) {
        // 牛奶暂时不计入食物
        if (item == Items.MILK_BUCKET) return false;
        
        return CUSTOM_LIFETIMES.containsKey(item);
    }
    
    /**
     * 获取食物标签
     */
    public static Set<String> getFoodTags(ItemStack stack) {
        if (stack.isEmpty()) return new HashSet<>();
        return FOOD_TAGS.getOrDefault(stack.getItem(), new HashSet<>());
    }
    
    /**
     * 获取奖励效果
     */
    public static List<EffectBonus> getBonusEffects(ItemStack stack) {
        if (stack.isEmpty()) return new ArrayList<>();
        return BONUS_EFFECTS.getOrDefault(stack.getItem(), new ArrayList<>());
    }
    
    /**
     * 获取食物的初始熟度值
     */
    public static float getInitialCookedness(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        
        Set<String> tags = getFoodTags(stack);
        for (String tag : tags) {
            if (tag.startsWith("熟度:")) {
                String cookedStr = tag.substring(3).replace("%", "").trim();
                try {
                    return Float.parseFloat(cookedStr);
                } catch (NumberFormatException e) {
                    return 0.0f;
                }
            }
        }
        return 0.0f;
    }
    
    /**
     * 获取注册的食物数量
     */
    public static int getRegisteredFoodCount() {
        return CUSTOM_LIFETIMES.size();
    }
}