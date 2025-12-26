package com.fyoyi.betterfood.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

public interface ICulinaryKnowledge {

    // 菜谱掌握状态
    enum Status {
        LOCKED,     // 未知 (高级菜谱默认)
        DISCOVERED, // 已发现 (试错/残页)
        MASTERED    // 已掌握 (完美制作/交易/读书)
    }

    // 获取某道菜的状态
    Status getStatus(ResourceLocation recipeId);

    // 设置状态 (解锁/升级)
    void setStatus(ResourceLocation recipeId, Status status);

    // 检查玩家是否"认识"这道菜 (用于 Advanced 菜谱判定)
    boolean canRecognize(ResourceLocation recipeId);

    // 记录玩家做这道菜的历史最高分
    void setHighScore(ResourceLocation recipeId, float score);
    float getHighScore(ResourceLocation recipeId);

    // 数据保存与读取
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}