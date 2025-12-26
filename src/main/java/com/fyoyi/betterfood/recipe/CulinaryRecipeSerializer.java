package com.fyoyi.betterfood.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CulinaryRecipeSerializer implements RecipeSerializer<CulinaryRecipe> {

    @Override
    public CulinaryRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");

        // 1. 读取等级
        String tierStr = GsonHelper.getAsString(json, "tier", "basic");
        CulinaryRecipe.RecipeTier tier = CulinaryRecipe.RecipeTier.fromString(tierStr);

        // 2. 读取需求列表 (Requirements)
        List<CulinaryRecipe.Requirement> requirements = new ArrayList<>();
        JsonArray reqArray = GsonHelper.getAsJsonArray(json, "requirements");
        for (JsonElement e : reqArray) {
            JsonObject obj = e.getAsJsonObject();
            requirements.add(new CulinaryRecipe.Requirement(
                    GsonHelper.getAsString(obj, "classification"),
                    GsonHelper.getAsInt(obj, "count"),
                    GsonHelper.getAsFloat(obj, "ideal_cookedness"),
                    GsonHelper.getAsFloat(obj, "tolerance")
            ));
        }

        // 3. 读取结果物品 (Result)
        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

        // 4. 读取命名规则 (Naming Rules)
        List<CulinaryRecipe.NamingRule> namingRules = new ArrayList<>();
        if (json.has("naming_rules")) {
            JsonArray rulesArray = GsonHelper.getAsJsonArray(json, "naming_rules");
            for (JsonElement e : rulesArray) {
                JsonObject obj = e.getAsJsonObject();
                namingRules.add(new CulinaryRecipe.NamingRule(
                        GsonHelper.getAsString(obj, "required_feature"),
                        GsonHelper.getAsString(obj, "override_name", null),
                        GsonHelper.getAsString(obj, "prefix", null),
                        GsonHelper.getAsInt(obj, "priority", 0)
                ));
            }
        }

        return new CulinaryRecipe(recipeId, group, tier, requirements, result, namingRules);
    }

    @Override
    public @Nullable CulinaryRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        // 网络同步暂时留空，单机模式不影响，联机需要完善
        return null;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, CulinaryRecipe recipe) {
        // 网络同步写入
    }
}