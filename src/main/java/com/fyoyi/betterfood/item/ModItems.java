package com.fyoyi.betterfood.item;

import com.fyoyi.betterfood.better_food;
import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.cooking_pan.PotBlockItem;
import com.fyoyi.betterfood.item.large_pot.LargePotBlockItem;
import com.fyoyi.betterfood.item.lid.LidBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items; // 导入原版物品
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, better_food.MOD_ID);

    // 锅具
    public static final RegistryObject<Item> COOKING_PAN = ITEMS.register("cooking_pan",
            () -> new PotBlockItem(ModBlocks.COOKING_PAN.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LARGE_POT = ITEMS.register("large_pot",
            () -> new LargePotBlockItem(ModBlocks.LARGE_POT.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LID = ITEMS.register("lid",
            () -> new LidBlockItem(ModBlocks.LID.get(), new Item.Properties().stacksTo(1)));


    // 炒菜类
    public static final RegistryObject<Item> STIR_FRIED_MEAT = ITEMS.register("stir_fried_meat",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationMod(0.8f).build())));

    public static final RegistryObject<Item> TEST_APPLE_SALAD = ITEMS.register("test_apple_salad",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(0.7f).build())));

    public static final RegistryObject<Item> MIXED_STIR_FRY = ITEMS.register("mixed_stir_fry",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationMod(0.75f).build())));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}