/*
 * 物品注册类
 * 该类负责注册模组中的所有物品
 */
package com.fyoyi.betterfood.item;

import com.fyoyi.betterfood.better_food;
import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.cooking_pan.PotBlockItem;
import com.fyoyi.betterfood.item.large_pot.LargePotBlockItem;
import com.fyoyi.betterfood.item.lid.LidBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    /*
     * 物品的延迟注册器
     */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, better_food.MOD_ID);

    /*
     * 示例方块物品的注册对象
     */
    public static final RegistryObject<Item> COOKING_PAN = ITEMS.register("cooking_pan",
            () -> new PotBlockItem(ModBlocks.COOKING_PAN.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LARGE_POT = ITEMS.register("large_pot",
            () -> new LargePotBlockItem(ModBlocks.LARGE_POT.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LID = ITEMS.register("lid",
            () -> new LidBlockItem(ModBlocks.LID.get(), new Item.Properties().stacksTo(1)));

    /*
     * 注册物品
     * @param eventBus IEventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}