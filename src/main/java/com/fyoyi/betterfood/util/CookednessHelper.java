package com.fyoyi.betterfood.util;

import com.fyoyi.betterfood.config.FoodConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 熟度管理工具类
 * 统一处理食物的熟度相关逻辑
 */
public class CookednessHelper {
    
    public static final String NBT_COOKED_PROGRESS = "BetterFood_CookedProgress";
    
    /**
     * 获取物品的当前熟度值
     * 优先从NBT中获取动态熟度，如果没有则从配置中获取初始熟度
     */
    public static float getCurrentCookedness(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        
        // 优先获取NBT中的动态熟度值
        if (stack.hasTag() && stack.getTag().contains(NBT_COOKED_PROGRESS)) {
            return stack.getTag().getFloat(NBT_COOKED_PROGRESS);
        }
        
        // 否则返回配置中的初始熟度值
        return FoodConfig.getInitialCookedness(stack);
    }
    
    /**
     * 设置物品的熟度值到NBT
     */
    public static void setCookedness(ItemStack stack, float cookedness) {
        if (stack.isEmpty()) return;
        
        CompoundTag nbt = stack.getOrCreateTag();
        if (cookedness <= 0.0f) {
            // 如果熟度为0或负数，移除NBT标签以保持物品整洁
            if (nbt.contains(NBT_COOKED_PROGRESS)) {
                nbt.remove(NBT_COOKED_PROGRESS);
                if (nbt.isEmpty()) stack.setTag(null);
            }
        } else {
            nbt.putFloat(NBT_COOKED_PROGRESS, cookedness);
        }
    }
    
    /**
     * 增加物品的熟度值
     */
    public static void increaseCookedness(ItemStack stack, float increment) {
        float current = getCurrentCookedness(stack);
        float newCookedness = Math.min(120.0f, current + increment); // 限制最大熟度为120%
        setCookedness(stack, newCookedness);
    }
    
    /**
     * 检查物品是否有动态熟度值（即NBT中存储的熟度）
     */
    public static boolean hasDynamicCookedness(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_COOKED_PROGRESS);
    }
    
    /**
     * 获取熟度的显示字符串
     */
    public static String getCookednessDisplayString(ItemStack stack) {
        float cookedness = getCurrentCookedness(stack);
        return String.format("%.1f%%", cookedness);
    }
}