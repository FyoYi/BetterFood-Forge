package com.fyoyi.betterfood.recipe;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.config.FoodConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CulinaryRecipe implements Recipe<Container> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation id;
    private final String group;
    private final RecipeTier tier;
    private final List<Requirement> requirements;
    private final ItemStack result;
    private final List<NamingRule> namingRules;

    public CulinaryRecipe(ResourceLocation id, String group, RecipeTier tier,
                          List<Requirement> requirements, ItemStack result, List<NamingRule> namingRules) {
        this.id = id;
        this.group = group;
        this.tier = tier;
        this.requirements = requirements;
        this.result = result;
        this.namingRules = namingRules;
    }

    // === 内部类：用于临时存储锅内食材快照 ===
    private static class IngredientSnapshot {
        String classification;
        float cookedness;
        boolean isUsed;
        String itemName;

        IngredientSnapshot(String classification, float cookedness, String itemName) {
            this.classification = classification;
            this.cookedness = cookedness;
            this.itemName = itemName;
            this.isUsed = false;
        }
    }

    // === 核心逻辑：判断锅内物品是否符合这道菜 ===
    @Override
    public boolean matches(Container container, Level level) {
        if (!(container instanceof PotBlockEntity)) return false;
        PotBlockEntity pot = (PotBlockEntity) container;
        NonNullList<ItemStack> potItems = pot.getItems();

        // 1. 构建锅内快照列表
        List<IngredientSnapshot> snapshots = new ArrayList<>();

        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;

            float cooked = 0f;
            if (stack.hasTag() && stack.getTag().contains(PotBlockEntity.NBT_COOKED_PROGRESS)) {
                cooked = stack.getTag().getFloat(PotBlockEntity.NBT_COOKED_PROGRESS);
            }

            Set<String> tags = FoodConfig.getFoodTags(stack);
            String foundClass = null;

            // 提取主分类
            for (String tag : tags) {
                if (tag.startsWith("分类:")) {
                    foundClass = tag.substring(3).trim();
                    break;
                }
            }

            if (foundClass != null) {
                snapshots.add(new IngredientSnapshot(foundClass, cooked, stack.getHoverName().getString()));
            } else {
                // 如果发现没有分类的物品，直接匹配失败
                // 只有测试时开启下面这行日志，否则刷屏
                // LOGGER.info("[BetterFood] 匹配中止: 发现未知分类物品 {} (Tags: {})", stack.getHoverName().getString(), tags);
                return false;
            }
        }

        // 2. 逐条检查菜谱要求 (消耗法匹配)
        for (Requirement req : requirements) {
            int needed = req.count;

            // 遍历锅内所有物品，寻找符合当前要求的
            for (IngredientSnapshot snap : snapshots) {
                if (needed <= 0) break; // 这一类找够了
                if (snap.isUsed) continue; // 已经被别的要求用掉了

                // 检查分类
                if (!snap.classification.equals(req.classification)) continue;

                // 检查熟度
                if (Math.abs(snap.cookedness - req.idealCookedness) > req.tolerance) {
                    // 如果分类对但熟度不对，记录一下日志方便调试
                    // LOGGER.info("[BetterFood] 菜谱 {} - 熟度不符: 物品[{}] 实际={} 目标={}", this.id, snap.itemName, snap.cookedness, req.idealCookedness);
                    continue;
                }

                // 匹配成功，标记为已用
                snap.isUsed = true;
                needed--;
            }

            // 如果这一条要求没凑够数量
            if (needed > 0) {
                // LOGGER.info("[BetterFood] 菜谱 {} - 缺少分类: {} (缺 {} 个)", this.id, req.classification, needed);
                return false;
            }
        }

        // 3. 严格匹配检查：是否所有锅内物品都被用掉了？
        for (IngredientSnapshot snap : snapshots) {
            if (!snap.isUsed) {
                // LOGGER.info("[BetterFood] 菜谱 {} - 匹配失败: 锅内有多余食材 {}", this.id, snap.itemName);
                return false;
            }
        }

        LOGGER.info("[BetterFood] >>> 菜谱匹配成功！生成: {} <<<", this.id);
        return true;
    }

    // === 核心逻辑：组装成品 (应用命名规则) ===
    private ItemStack assembleInternal(PotBlockEntity pot) {
        ItemStack output = result.copy();
        NonNullList<ItemStack> potItems = pot.getItems();

        // 收集锅内所有的特点 (Features)
        List<String> allFeatures = new ArrayList<>();
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            Set<String> tags = FoodConfig.getFoodTags(stack);
            for (String tag : tags) {
                if (tag.startsWith("特点:")) {
                    allFeatures.add(tag.substring(3).trim());
                }
            }
        }

        // 应用命名规则
        String finalName = result.getHoverName().getString(); // 默认名字
        int highestPriority = -1;

        // 1. 查找最高优先级的改名规则 (Override Name)
        for (NamingRule rule : namingRules) {
            if (rule.overrideName != null && allFeatures.contains(rule.requiredFeature)) {
                if (rule.priority > highestPriority) {
                    finalName = rule.overrideName;
                    highestPriority = rule.priority;
                }
            }
        }

        // 2. 查找前缀规则 (Prefix) - 可以叠加
        String prefix = "";
        for (NamingRule rule : namingRules) {
            if (rule.prefix != null && allFeatures.contains(rule.requiredFeature)) {
                // 简单处理：避免重复前缀
                if (!prefix.contains(rule.prefix)) {
                    prefix += rule.prefix;
                }
            }
        }

        // 设置最终名字
        output.setHoverName(Component.literal(prefix + finalName));

        return output;
    }

    // === Recipe 接口标准方法 ===

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        if (pContainer instanceof PotBlockEntity) {
            return assembleInternal((PotBlockEntity) pContainer);
        }
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CULINARY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CULINARY_TYPE.get();
    }

    // === 内部类定义 ===

    public enum RecipeTier {
        BASIC, ADVANCED;
        public static RecipeTier fromString(String str) {
            return "advanced".equalsIgnoreCase(str) ? ADVANCED : BASIC;
        }
    }

    public static class Requirement {
        public final String classification;
        public final int count;
        public final float idealCookedness;
        public final float tolerance;

        public Requirement(String classification, int count, float idealCookedness, float tolerance) {
            this.classification = classification;
            this.count = count;
            this.idealCookedness = idealCookedness;
            this.tolerance = tolerance;
        }
    }

    public static class NamingRule {
        public final String requiredFeature;
        public final String overrideName;
        public final String prefix;
        public final int priority;

        public NamingRule(String requiredFeature, String overrideName, String prefix, int priority) {
            this.requiredFeature = requiredFeature;
            this.overrideName = overrideName;
            this.prefix = prefix;
            this.priority = priority;
        }
    }
}