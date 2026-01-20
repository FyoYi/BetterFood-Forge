package com.fyoyi.betterfood.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 玩家食谱书 - 记录玩家探索出的食谱
 * 分三类：平底锅、炖锅、切菜板
 */
public class RecipeBook {

    /**
     * 单个食谱记录 - 用于平底锅和炖锅
     */
    public static class CookRecipe {
        public String dishName; // 菜名（物品名）
        public float finalScore; // 最终评分
        public List<IngredientRequirement> ingredients; // 食材要求

        public CookRecipe(String dishName, float finalScore, List<IngredientRequirement> ingredients) {
            this.dishName = dishName;
            this.finalScore = finalScore;
            this.ingredients = new ArrayList<>(ingredients);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("DishName", dishName);
            tag.putFloat("FinalScore", finalScore);

            ListTag ingredientsList = new ListTag();
            for (IngredientRequirement req : ingredients) {
                ingredientsList.add(req.serializeNBT());
            }
            tag.put("Ingredients", ingredientsList);
            return tag;
        }

        public static CookRecipe deserializeNBT(CompoundTag tag) {
            String dishName = tag.getString("DishName");
            float finalScore = tag.getFloat("FinalScore");
            List<IngredientRequirement> ingredients = new ArrayList<>();

            ListTag ingredientsList = tag.getList("Ingredients", Tag.TAG_COMPOUND);
            for (int i = 0; i < ingredientsList.size(); i++) {
                ingredients.add(IngredientRequirement.deserializeNBT(ingredientsList.getCompound(i)));
            }

            return new CookRecipe(dishName, finalScore, ingredients);
        }
    }

    /**
     * 食材要求 - 记录某个食材分类需要的熟度范围
     */
    public static class IngredientRequirement {
        public String ingredientTag; // 食材分类标签（如 "肉类"、"菜类"）
        public float minCookedness; // 最小熟度 (0-120%)
        public float maxCookedness; // 最大熟度 (0-120%)

        public IngredientRequirement(String ingredientTag, float minCookedness, float maxCookedness) {
            this.ingredientTag = ingredientTag;
            this.minCookedness = minCookedness;
            this.maxCookedness = maxCookedness;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("IngredientTag", ingredientTag);
            tag.putFloat("MinCookedness", minCookedness);
            tag.putFloat("MaxCookedness", maxCookedness);
            return tag;
        }

        public static IngredientRequirement deserializeNBT(CompoundTag tag) {
            return new IngredientRequirement(
                    tag.getString("IngredientTag"),
                    tag.getFloat("MinCookedness"),
                    tag.getFloat("MaxCookedness"));
        }
    }

    /**
     * 切菜板菜谱记录 - 简单的输入->输出对应
     */
    public static class CuttingRecipe {
        public String inputItem; // 输入物品名
        public String outputItem; // 输出物品名

        public CuttingRecipe(String inputItem, String outputItem) {
            this.inputItem = inputItem;
            this.outputItem = outputItem;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("InputItem", inputItem);
            tag.putString("OutputItem", outputItem);
            return tag;
        }

        public static CuttingRecipe deserializeNBT(CompoundTag tag) {
            return new CuttingRecipe(
                    tag.getString("InputItem"),
                    tag.getString("OutputItem"));
        }
    }

    // ===== 数据存储 =====
    private Map<String, CookRecipe> cookingPanRecipes; // 平底锅菜谱：菜名 -> 菜谱
    private Map<String, CookRecipe> largePotRecipes; // 炖锅菜谱：菜名 -> 菜谱
    private Set<String> cuttingRecipes; // 切菜板菜谱：已记录过的食材转换

    public RecipeBook() {
        this.cookingPanRecipes = new HashMap<>();
        this.largePotRecipes = new HashMap<>();
        this.cuttingRecipes = new HashSet<>();
    }

    // ===== 平底锅菜谱管理 =====
    public void recordCookingPanRecipe(String dishName, float finalScore, List<IngredientRequirement> ingredients) {
        CookRecipe newRecipe = new CookRecipe(dishName, finalScore, ingredients);

        // 如果菜谱已存在，只在新评分更高时才更新
        if (cookingPanRecipes.containsKey(dishName)) {
            CookRecipe existing = cookingPanRecipes.get(dishName);
            if (finalScore > existing.finalScore) {
                cookingPanRecipes.put(dishName, newRecipe);
            }
        } else {
            // 新菜谱，直接记录
            cookingPanRecipes.put(dishName, newRecipe);
        }
    }

    public CookRecipe getCookingPanRecipe(String dishName) {
        return cookingPanRecipes.get(dishName);
    }

    public Collection<CookRecipe> getAllCookingPanRecipes() {
        return cookingPanRecipes.values();
    }

    public boolean hasCookingPanRecipe(String dishName) {
        return cookingPanRecipes.containsKey(dishName);
    }

    // ===== 炖锅菜谱管理 =====
    public void recordLargePotRecipe(String dishName, float finalScore, List<IngredientRequirement> ingredients) {
        CookRecipe newRecipe = new CookRecipe(dishName, finalScore, ingredients);

        if (largePotRecipes.containsKey(dishName)) {
            CookRecipe existing = largePotRecipes.get(dishName);
            if (finalScore > existing.finalScore) {
                largePotRecipes.put(dishName, newRecipe);
            }
        } else {
            largePotRecipes.put(dishName, newRecipe);
        }
    }

    public CookRecipe getLargePotRecipe(String dishName) {
        return largePotRecipes.get(dishName);
    }

    public Collection<CookRecipe> getAllLargePotRecipes() {
        return largePotRecipes.values();
    }

    public boolean hasLargePotRecipe(String dishName) {
        return largePotRecipes.containsKey(dishName);
    }

    // ===== 切菜板菜谱管理 =====
    public void recordCuttingRecipe(String inputItem, String outputItem) {
        // 生成一个唯一的key来记录这个转换
        String key = inputItem + " -> " + outputItem;
        cuttingRecipes.add(key);
    }

    public boolean hasCuttingRecipe(String inputItem, String outputItem) {
        String key = inputItem + " -> " + outputItem;
        return cuttingRecipes.contains(key);
    }

    public Set<String> getAllCuttingRecipes() {
        return new HashSet<>(cuttingRecipes);
    }

    // ===== NBT 序列化 =====
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 保存平底锅菜谱
        ListTag cookingPanList = new ListTag();
        for (CookRecipe recipe : cookingPanRecipes.values()) {
            cookingPanList.add(recipe.serializeNBT());
        }
        tag.put("CookingPanRecipes", cookingPanList);

        // 保存炖锅菜谱
        ListTag largePotList = new ListTag();
        for (CookRecipe recipe : largePotRecipes.values()) {
            largePotList.add(recipe.serializeNBT());
        }
        tag.put("LargePotRecipes", largePotList);

        // 保存切菜板菜谱
        ListTag cuttingList = new ListTag();
        for (String recipe : cuttingRecipes) {
            CompoundTag recipeTag = new CompoundTag();
            recipeTag.putString("Recipe", recipe);
            cuttingList.add(recipeTag);
        }
        tag.put("CuttingRecipes", cuttingList);

        return tag;
    }

    public static RecipeBook deserializeNBT(CompoundTag tag) {
        RecipeBook book = new RecipeBook();

        // 加载平底锅菜谱
        ListTag cookingPanList = tag.getList("CookingPanRecipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < cookingPanList.size(); i++) {
            CookRecipe recipe = CookRecipe.deserializeNBT(cookingPanList.getCompound(i));
            book.cookingPanRecipes.put(recipe.dishName, recipe);
        }

        // 加载炖锅菜谱
        ListTag largePotList = tag.getList("LargePotRecipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < largePotList.size(); i++) {
            CookRecipe recipe = CookRecipe.deserializeNBT(largePotList.getCompound(i));
            book.largePotRecipes.put(recipe.dishName, recipe);
        }

        // 加载切菜板菜谱
        ListTag cuttingList = tag.getList("CuttingRecipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < cuttingList.size(); i++) {
            String recipe = cuttingList.getCompound(i).getString("Recipe");
            book.cuttingRecipes.add(recipe);
        }

        return book;
    }

    // ===== 清空所有菜谱 =====
    public void clear() {
        cookingPanRecipes.clear();
        largePotRecipes.clear();
        cuttingRecipes.clear();
    }

    public int getTotalRecipeCount() {
        return cookingPanRecipes.size() + largePotRecipes.size() + cuttingRecipes.size();
    }
}
