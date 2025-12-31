/*
 * 创造模式物品栏类
 * 该类负责注册和管理模组的创造模式物品栏
 */
package com.fyoyi.betterfood;

import com.fyoyi.betterfood.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Items;

public class ModCreativeModeTabs {
    /*
     * 创造模式物品栏的延迟注册器
     */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, better_food.MOD_ID);

    /*
     * Better Food 创造模式物品栏的注册对象
     * 定义了物品栏的图标、标题和显示的物品
     */
    public static final RegistryObject<CreativeModeTab> BETTER_FOOD_TAB = CREATIVE_MODE_TABS.register("better_food_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COOKING_PAN.get()))
                    .title(Component.translatable("creativemodetab.better_food_tab"))
                    .displayItems((parameters, output) -> {
                        // 添加物品到创造模式物品栏
                        output.accept(ModItems.COOKING_PAN.get());
                        output.accept(ModItems.LARGE_POT.get());
                        output.accept(ModItems.LID.get());
                        output.accept(ModItems.SPATULA.get());
                        output.accept(Items.BOWL);
                        output.accept(ModItems.OIL_BOWL.get());
                    })
                    .build());

    /*
     * 注册创造模式物品栏
     * @param eventBus IEventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}