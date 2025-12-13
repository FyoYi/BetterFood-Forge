package com.fyoyi.betterfood.config;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodConfig {

    public static final long TICKS_PER_DAY = 24000L;
    public static final long SHELF_LIFE_DEFAULT = 7 * TICKS_PER_DAY;
    public static final long SHELF_LIFE_INFINITE = -1L;

    // 时间预设常量
    public static final float MIN_MINUTES = (2.5f * 20);
    public static final float SHORT_MINUTES = (5.0f * 20);
    public static final float MEDIUM_MINUTES = (12.0f * 20);
    public static final float LONG_MINUTES = (50.0f * 20);

    // 保质期存储
    private static final Map<Item, Long> CUSTOM_LIFETIMES = new HashMap<>();

    // === 【新增】新鲜奖励存储 (支持多个效果) ===
    private static final Map<Item, List<EffectBonus>> FRESH_BONUSES = new HashMap<>();

    // 单个效果的奖励数据
    public static class EffectBonus {
        public final MobEffect effect;      // 效果类型
        public final float chance;          // 概率 (0.0 - 1.0)
        public final int durationSeconds;   // 持续时间 (秒)
        public final int amplifier;         // 效果等级 (0 = I, 1 = II, ...)

        public EffectBonus(MobEffect effect, float chance, int durationSeconds, int amplifier) {
            this.effect = effect;
            this.chance = chance;
            this.durationSeconds = durationSeconds;
            this.amplifier = amplifier;
        }
    }

    // === 【兼容旧版】简化版奖励类（已废弃，仅用于向后兼容）===
    @Deprecated
    public static class FreshBonus {
        public final float chance;
        public final int durationSeconds;

        public FreshBonus(float chance, int durationSeconds) {
            this.chance = chance;
            this.durationSeconds = durationSeconds;
        }
    }

    // === 【核心接口】定义不同食物的新鲜奖励（现在由 JSON 管理，这里仅作为后备）===
    static {
        // 注意：现在推荐使用 JSON 配置，这里的代码仅在 JSON 未配置时生效
    }

    /**
     * 注册新鲜奖励接口 (支持多个效果)
     */
    public static void registerBonus(Item item, List<EffectBonus> effects) {
        FRESH_BONUSES.put(item, effects);
    }

    /**
     * 清空奖励数据 (用于重载)
     */
    public static void clearBonuses() {
        FRESH_BONUSES.clear();
    }

    /**
     * 获取奖励配置 (新版 - 返回效果列表)
     */
    public static List<EffectBonus> getBonusEffects(ItemStack stack) {
        return FRESH_BONUSES.get(stack.getItem());
    }

    /**
     * 获取奖励配置 (旧版 - 兼容性接口，已废弃)
     */
    @Deprecated
    public static FreshBonus getFreshBonus(ItemStack stack) {
        List<EffectBonus> effects = FRESH_BONUSES.get(stack.getItem());
        if (effects == null || effects.isEmpty()) return null;
        // 只返回第一个效果作为兼容
        EffectBonus first = effects.get(0);
        return new FreshBonus(first.chance, first.durationSeconds);
    }

    // --- 以下保持原有逻辑 ---

    public static void clear() {
        CUSTOM_LIFETIMES.clear();
        // 注意：FRESH_BONUSES 由 FoodBonusManager 单独管理
    }

    public static void register(Item item, long ticks) {
        CUSTOM_LIFETIMES.put(item, ticks);
    }

    public static void remove(Item item) {
        CUSTOM_LIFETIMES.remove(item);
    }

    public static long getItemLifetime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (CUSTOM_LIFETIMES.containsKey(stack.getItem())) {
            return CUSTOM_LIFETIMES.get(stack.getItem());
        }
        return SHELF_LIFE_DEFAULT;
    }

    public static boolean canRot(ItemStack stack) {
        if (stack.getItem() == Items.ROTTEN_FLESH) return false;
        if (CUSTOM_LIFETIMES.containsKey(stack.getItem())) return true;
        return stack.getItem().isEdible();
    }
}