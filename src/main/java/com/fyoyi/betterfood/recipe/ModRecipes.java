package com.fyoyi.betterfood.recipe;

import com.fyoyi.betterfood.better_food;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, better_food.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, better_food.MOD_ID);

    // 注册序列化器 (用于读取 JSON)
    public static final RegistryObject<RecipeSerializer<CulinaryRecipe>> CULINARY_SERIALIZER =
            SERIALIZERS.register("culinary_match", CulinaryRecipeSerializer::new);

    // 注册配方类型 (用于在代码里查找配方)
    public static final RegistryObject<RecipeType<CulinaryRecipe>> CULINARY_TYPE =
            TYPES.register("culinary_match", () -> new RecipeType<CulinaryRecipe>() {
                @Override
                public String toString() {
                    return "better_food:culinary_match";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}