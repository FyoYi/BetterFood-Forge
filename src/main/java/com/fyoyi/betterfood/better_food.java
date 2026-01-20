/*
 * Better Food 模组主类 - 最终完整版
 * 包含：核心注册、客户端渲染注册、物品属性覆写(锅铲变色)、HUD注册、Tooltip显示
 */
package com.fyoyi.betterfood;

// === 核心工具类引用 ===
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.FoodExpiryManager;
import com.fyoyi.betterfood.util.CookednessHelper;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.ModItems;
import com.fyoyi.betterfood.ModCreativeModeTabs;
import com.fyoyi.betterfood.block.entity.ModBlockEntities;

// === 渲染器 & GUI & 配方 & 网络 ===
import com.fyoyi.betterfood.client.renderer.PotRendererDispatcher;
import com.fyoyi.betterfood.client.gui.PotInfoOverlay;
import com.fyoyi.betterfood.recipe.ModRecipes;
import com.fyoyi.betterfood.network.NetworkManager;
import com.fyoyi.betterfood.client.renderer.PotWorldRenderer;
import com.fyoyi.betterfood.client.renderer.cutting_board.CuttingBoardBlockRenderer;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Mod(better_food.MOD_ID)
public class better_food {
    public static final String MOD_ID = "better_food";
    private static final Logger LOGGER = LogUtils.getLogger();

    public better_food(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 1. 注册核心内容
        modEventBus.addListener(this::commonSetup);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 注册配方系统
        ModRecipes.register(modEventBus);

        // 注册配置
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册网络消息
        NetworkManager.register();

        // 注册世界渲染器 (World Renderer)
        PotWorldRenderer.register();

        // 2. 注册客户端 Mod总线事件 (手动绑定，避免重复注册)
        modEventBus.addListener(ClientModEvents::registerRenderers);
        modEventBus.addListener(ClientModEvents::registerGuiOverlays);
        modEventBus.addListener(ClientModEvents::registerItemDecorations);
        modEventBus.addListener(ClientModEvents::onClientSetup); // 包含锅铲变色逻辑

        // 3. 注册 Forge总线事件
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);

        // 手动注册能力附加和菜谱解锁事件处理
        MinecraftForge.EVENT_BUS
                .register(new com.fyoyi.betterfood.event.PlayerRecipeBookCapability.CapabilityEventHandler());

        // 注册 Tooltip 事件处理者
        MinecraftForge.EVENT_BUS.register(ClientForgeEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FoodExpiryManager());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == ModItems.RECIPE_BOOK.get()) {
            net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) event
                    .getEntity();

            // 仅在客户端打开GUI
            if (player.level().isClientSide()) {
                // 从玩家能力中获取菜谱书
                var recipeBookCap = player
                        .getCapability(com.fyoyi.betterfood.event.PlayerRecipeBookCapability.RECIPE_BOOK_CAP);

                LOGGER.info("右键菜谱书 - 能力存在: {}", recipeBookCap.isPresent());

                if (recipeBookCap.isPresent()) {
                    com.fyoyi.betterfood.recipe.RecipeBook recipeBook = recipeBookCap.resolve().get().getRecipeBook();
                    // 打开自定义菜谱书GUI
                    net.minecraft.client.Minecraft.getInstance()
                            .setScreen(new com.fyoyi.betterfood.client.gui.RecipeBookScreen(recipeBook));
                    LOGGER.info("打开菜谱书GUI，当前菜谱数: {}", recipeBook.getTotalRecipeCount());
                } else {
                    LOGGER.warn("菜谱书能力不存在，尝试创建空白菜谱书");
                    // 如果能力不存在，创建一个空白菜谱书
                    com.fyoyi.betterfood.recipe.RecipeBook emptyBook = new com.fyoyi.betterfood.recipe.RecipeBook();
                    net.minecraft.client.Minecraft.getInstance()
                            .setScreen(new com.fyoyi.betterfood.client.gui.RecipeBookScreen(emptyBook));
                }
            }

            // 取消默认行为
            event.setCanceled(true);
        }
    }

    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");

            // 这让锅铲在有 "IsOily" NBT 时能够切换贴图
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.SPATULA.get(),
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, "oily"),
                        (stack, level, entity, seed) -> {
                            return (stack.hasTag() && stack.getTag().getBoolean("IsOily")) ? 1.0F : 0.0F;
                        });
            });
        }

        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.POT_BE.get(), PotRendererDispatcher::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CUTTING_BOARD_BE.get(), CuttingBoardBlockRenderer::new);
        }

        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("pot_info", PotInfoOverlay.INSTANCE);
        }

        public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack defaultStack = new ItemStack(item);
                if (FoodConfig.canRot(defaultStack)) {
                    event.register(item, (graphics, font, stack, x, y) -> {
                        if (Minecraft.getInstance().level == null)
                            return false;
                        float percent = FreshnessHelper.getFreshnessPercentage(Minecraft.getInstance().level, stack);
                        if (percent < 1.0F) {
                            int barWidth = Math.round(13.0F * percent);
                            int color = java.awt.Color.HSBtoRGB(percent / 3.0F, 1.0F, 1.0F);
                            graphics.pose().pushPose();
                            graphics.pose().translate(0, 0, 200);
                            graphics.fill(x + 2, y + 13, x + 15, y + 15, 0xFF000000);
                            graphics.fill(x + 2, y + 13, x + 2 + barWidth, y + 14, color | 0xFF000000);
                            graphics.pose().popPose();
                            return true;
                        }
                        return false;
                    });
                }
            }
        }
    }
}