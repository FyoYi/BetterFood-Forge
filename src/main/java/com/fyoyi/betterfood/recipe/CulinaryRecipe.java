package com.fyoyi.betterfood.recipe;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.CookednessHelper;
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

    public CulinaryRecipe(ResourceLocation id, String group, RecipeTier tier,
                          List<Requirement> requirements, ItemStack result, List<NamingRule> namingRules) {
        this.id = id;
        this.group = group;
        this.tier = tier;
        this.requirements = requirements;
        this.result = result;
        this.namingRules = namingRules;
    }

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
                snapshots.add(new IngredientSnapshot(foundClass, cooked, stack.getHoverName().getString()));
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
        ItemStack output = result.copy();
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

        String finalName = result.getHoverName().getString();
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
        return output;
    }

    // Standard methods
    @Override public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess pRegistryAccess) { return result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CULINARY_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.CULINARY_TYPE.get(); }

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