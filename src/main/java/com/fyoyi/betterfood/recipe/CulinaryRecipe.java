package com.fyoyi.betterfood.recipe;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.CookednessHelper;
import com.fyoyi.betterfood.util.FreshnessHelper;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CulinaryRecipe implements Recipe<Container> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation id;
    private final String group;
    private final RecipeTier tier;
    private final List<Requirement> requirements;
    private final ItemStack result;
    private final List<NamingRule> namingRules;
    private final String baseResultId; // 添加基础物品ID，用于生成不同的亚种

    public CulinaryRecipe(ResourceLocation id, String group, RecipeTier tier,
                          List<Requirement> requirements, ItemStack result, List<NamingRule> namingRules) {
        this.id = id;
        this.group = group;
        this.tier = tier;
        this.requirements = requirements;
        this.result = result;
        this.namingRules = namingRules;
        this.baseResultId = result.getItem().toString(); // 保存基础物品ID
    }

    private static class IngredientSnapshot {
        String classification;
        float cookedness;
        boolean isUsed;
        String itemName;
        Set<String> tags; // 添加标签集合

        IngredientSnapshot(String classification, float cookedness, String itemName, Set<String> tags) {
            this.classification = classification;
            this.cookedness = cookedness;
            this.itemName = itemName;
            this.tags = tags;
            this.isUsed = false;
        }
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (!(container instanceof PotBlockEntity)) return false;
        PotBlockEntity pot = (PotBlockEntity) container;
        NonNullList<ItemStack> potItems = pot.getItems();

        // 1. 构建快照
        List<IngredientSnapshot> snapshots = new ArrayList<>();

        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;

            float cooked = CookednessHelper.getCurrentCookedness(stack);

            Set<String> tags = FoodConfig.getFoodTags(stack);
            String foundClass = null;

            for (String tag : tags) {
                if (tag.startsWith("分类:")) {
                    foundClass = tag.substring(3).trim();
                    break;
                }
            }

            if (foundClass != null) {
                snapshots.add(new IngredientSnapshot(foundClass, cooked, stack.getHoverName().getString(), tags));
            } else {
                // LOGGER.info("[BetterFood] 发现未知分类物品 {}", stack.getHoverName().getString());
                return false;
            }
        }



        // 2. 检查要求
        for (Requirement req : requirements) {
            int needed = req.count;

            for (IngredientSnapshot snap : snapshots) {
                if (needed <= 0) break;
                if (snap.isUsed) continue;

                if (!snap.classification.equals(req.classification)) continue;

                if (Math.abs(snap.cookedness - req.idealCookedness) > req.tolerance) {
                    continue; // 熟度不符
                }

                snap.isUsed = true;
                needed--;
            }

            if (needed > 0) return false; // 缺食材
        }

        // 3. 严格匹配 (不能有多余食材)
        for (IngredientSnapshot snap : snapshots) {
            if (!snap.isUsed) return false;
        }

        LOGGER.info("[BetterFood] 匹配成功: {}", this.id);
        return true;
    }

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        if (pContainer instanceof PotBlockEntity) {
            return assembleInternal((PotBlockEntity) pContainer);
        }
        return result.copy();
    }

    private ItemStack assembleInternal(PotBlockEntity pot) {
        // 根据锅内食材生成特定的菜品变种
        ItemStack output = createResultBasedOnIngredients(pot);
        
        NonNullList<ItemStack> potItems = pot.getItems();
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

        String finalName = output.getHoverName().getString();
        
        // 检查是否使用了多种肉类特征
        java.util.Set<String> meatFeatures = getMeatFeatures(pot);
        if (meatFeatures.size() > 1) {
            // 如果有多种肉类特征，添加"混合肉类"特征
            allFeatures.add("混合肉类");
        }
        
        int highestPriority = -1;
        for (NamingRule rule : namingRules) {
            if (rule.overrideName != null && allFeatures.contains(rule.requiredFeature)) {
                if (rule.priority > highestPriority) {
                    finalName = rule.overrideName;
                    highestPriority = rule.priority;
                }
            }
        }
        String prefix = "";
        for (NamingRule rule : namingRules) {
            if (rule.prefix != null && allFeatures.contains(rule.requiredFeature)) {
                if (!prefix.contains(rule.prefix)) prefix += rule.prefix;
            }
        }
        
        output.setHoverName(Component.literal(prefix + finalName));
        
        // 应用评价系统：根据新鲜度和熟度偏差调整
        float avgFreshness = calculateAverageFreshness(pot);
        float avgCookednessDeviation = calculateAverageCookednessDeviation(pot);
        
        // 计算评价分数
        float score = calculateScore(avgFreshness, avgCookednessDeviation);
        
        // 将评价分数存储为NBT数据，供tooltip显示
        CompoundTag tag = output.getOrCreateTag();
        tag.putFloat("DishScore", score);
        tag.putFloat("DishFreshness", avgFreshness);
        tag.putFloat("DishCookednessDeviation", avgCookednessDeviation);
        
        return output;
    }
    
    // 根据食材生成对应的菜品变种
    private ItemStack createResultBasedOnIngredients(PotBlockEntity pot) {
        // 获取锅内食材的主要分类
        String mainClassification = getMainClassification(pot);
        
        // 创建基础结果的副本
        ItemStack baseResult = result.copy();
        
        // 根据主要分类和特点调整结果
        String modifiedName = result.getHoverName().getString();
        if (hasMixedIngredients(pot)) {
            // 如果使用了混合食材，则生成通用名称（如杂烩）
            modifiedName = getGenericMixedName(modifiedName, mainClassification);
        }
        
        baseResult.setHoverName(Component.literal(modifiedName));
        return baseResult;
    }
    
    // 获取通用混合名称
    private String getGenericMixedName(String originalName, String mainClassification) {
        // 根据原始名称和主要分类生成通用混合名称
        if (originalName.contains("肉")) {
            return originalName.replace("肉", "杂肉");
        } else if (originalName.contains("菜")) {
            return originalName.replace("菜", "杂烩");
        } else {
            return "杂烩";
        }
    }
    
    // 获取锅内食材的主要分类
    private String getMainClassification(PotBlockEntity pot) {
        NonNullList<ItemStack> potItems = pot.getItems();
        String classification = null;
        
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            Set<String> tags = FoodConfig.getFoodTags(stack);
            for (String tag : tags) {
                if (tag.startsWith("分类:")) {
                    classification = tag.substring(3).trim();
                    break;
                }
            }
            if (classification != null) break; // 只取第一个食材的分类作为主要分类
        }
        
        return classification;
    }
    
    // 获取肉类特征
    private java.util.Set<String> getMeatFeatures(PotBlockEntity pot) {
        NonNullList<ItemStack> potItems = pot.getItems();
        java.util.Set<String> meatFeatures = new java.util.HashSet<>();
        
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            
            Set<String> tags = FoodConfig.getFoodTags(stack);
            boolean isMeat = false;
            String meatFeature = null;
            
            for (String tag : tags) {
                if (tag.startsWith("分类:") && tag.substring(3).trim().equals("肉类")) {
                    isMeat = true;
                } else if (tag.startsWith("特点:")) {
                    String feature = tag.substring(3).trim();
                    // 假设肉类特点包括牛肉、猪肉等
                    if (feature.contains("牛") || feature.contains("猪") || feature.contains("羊") || 
                        feature.contains("鸡") || feature.contains("鸭") || feature.contains("鱼")) {
                        meatFeature = feature;
                    }
                }
            }
            
            if (isMeat && meatFeature != null) {
                meatFeatures.add(meatFeature);
            }
        }
        
        return meatFeatures;
    }
    
    // 检查是否使用了混合食材
    private boolean hasMixedIngredients(PotBlockEntity pot) {
        NonNullList<ItemStack> potItems = pot.getItems();
        String firstClassification = null;
        
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            Set<String> tags = FoodConfig.getFoodTags(stack);
            String currentClassification = null;
            
            for (String tag : tags) {
                if (tag.startsWith("分类:")) {
                    currentClassification = tag.substring(3).trim();
                    break;
                }
            }
            
            if (currentClassification != null) {
                if (firstClassification == null) {
                    firstClassification = currentClassification;
                } else if (!firstClassification.equals(currentClassification)) {
                    return true; // 发现不同分类的食材
                }
            }
        }
        
        return false;
    }
    
    // 计算平均新鲜度
    private float calculateAverageFreshness(PotBlockEntity pot) {
        NonNullList<ItemStack> potItems = pot.getItems();
        float totalFreshness = 0;
        int count = 0;
        
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            // 使用FreshnessHelper获取新鲜度百分比，而不是CookednessHelper
            float freshness = FreshnessHelper.getFreshnessPercentage(pot.getLevel(), stack) * 100f; // 转换为百分比
            // 应用非线性转换：95%以上算作100新鲜度
            float adjustedFreshness = applyNonLinearFreshness(freshness);
            totalFreshness += adjustedFreshness;
            count++;
        }
        
        return count > 0 ? totalFreshness / count : 0;
    }
    
    // 应用非线性新鲜度转换：95%以上算作100新鲜度
    private float applyNonLinearFreshness(float originalFreshness) {
        if (originalFreshness >= 95.0f) {
            return 100.0f;  // 95%以上直接算作100%
        } else {
            // 0-95%范围内线性计算，然后映射到0-95%范围
            return (originalFreshness / 95.0f) * 95.0f;
        }
    }
    
    // 计算平均熟度偏差
    private float calculateAverageCookednessDeviation(PotBlockEntity pot) {
        NonNullList<ItemStack> potItems = pot.getItems();
        float totalDeviation = 0;
        int count = 0;
        
        // 为每个食材计算与菜谱要求熟度的偏差
        for (ItemStack stack : potItems) {
            if (stack.isEmpty()) continue;
            
            // 获取食材的当前熟度
            float currentCookedness = CookednessHelper.getCurrentCookedness(stack);
            
            // 从菜谱中查找对应食材的理想熟度
            float expectedCookedness = 70.0f; // 默认理想熟度为70%
            
            // 获取食材的分类
            Set<String> tags = FoodConfig.getFoodTags(stack);
            String classification = null;
            for (String tag : tags) {
                if (tag.startsWith("分类:")) {
                    classification = tag.substring(3).trim();
                    break;
                }
            }
            
            // 遍历菜谱中的要求，查找匹配的分类
            if (classification != null) {
                for (Requirement req : requirements) {
                    if (req.classification.equals(classification)) {
                        expectedCookedness = req.idealCookedness;
                        break;
                    }
                }
            }
            
            totalDeviation += Math.abs(currentCookedness - expectedCookedness);
            count++;
        }
        
        return count > 0 ? totalDeviation / count : 0;
    }
    
    // 计算评价分数
    private float calculateScore(float avgFreshness, float avgCookednessDeviation) {
        // 分数 = 新鲜度百分比 * k + 偏差值 * b
        // 这里k=1.0，b=-0.3，可以根据需要调整权重
        float k = 1.0f; // 新鲜度权重
        float b = -0.3f; // 偏差权重（负值，偏差越大分数越低）
        
        float score = avgFreshness * k + avgCookednessDeviation * b;
        
        // 确保分数在合理范围内（0-100）
        return Math.max(0, Math.min(100, score));
    }

    // Standard methods
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
    
    // Getter方法，用于访问requirements
    public List<Requirement> getRequirements() {
        return requirements;
    }

    public enum RecipeTier {
        BASIC, ADVANCED;
        public static RecipeTier fromString(String str) { return "advanced".equalsIgnoreCase(str) ? ADVANCED : BASIC; }
    }
    public static class Requirement {
        public final String classification;
        public final int count;
        public final float idealCookedness;
        public final float tolerance;
        public Requirement(String classification, int count, float idealCookedness, float tolerance) {
            this.classification = classification; this.count = count; this.idealCookedness = idealCookedness; this.tolerance = tolerance;
        }
    }
    public static class NamingRule {
        public final String requiredFeature;
        public final String overrideName;
        public final String prefix;
        public final int priority;
        public NamingRule(String requiredFeature, String overrideName, String prefix, int priority) {
            this.requiredFeature = requiredFeature; this.overrideName = overrideName; this.prefix = prefix; this.priority = priority;
        }
    }
}