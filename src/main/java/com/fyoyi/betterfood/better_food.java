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

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Mod(better_food.MOD_ID)
public class better_food
{
    public static final String MOD_ID = "better_food";
    private static final Logger LOGGER = LogUtils.getLogger();

    public better_food(FMLJavaModLoadingContext context)
    {
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

        // 注册网络包
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

        // 注册 Tooltip 事件处理器
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

    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");

            // 这让锅铲在有 "IsOily" NBT 时能够切换贴图
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.SPATULA.get(),
                        new ResourceLocation(MOD_ID, "oily"),
                        (stack, level, entity, seed) -> {

                            return (stack.hasTag() && stack.getTag().getBoolean("IsOily")) ? 1.0F : 0.0F;
                        });
            });
        }

        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.POT_BE.get(), PotRendererDispatcher::new);
        }

        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("pot_info", PotInfoOverlay.INSTANCE);
        }

        public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack defaultStack = new ItemStack(item);
                if (FoodConfig.canRot(defaultStack)) {
                    event.register(item, (graphics, font, stack, x, y) -> {
                        if (Minecraft.getInstance().level == null) return false;
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

    // =================================================================
    // 内部类 2: 客户端 Forge 总线事件 (Tooltip等)
    // =================================================================
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();

            // 腐肉特殊处理
            if (stack.getItem() == net.minecraft.world.item.Items.ROTTEN_FLESH) {
                event.getToolTip().add(Component.literal("新鲜度: 已腐烂").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal(" - 100%概率获得饥饿 (25秒)").withStyle(ChatFormatting.DARK_RED));
                addFoodTagsInfo(event, stack);
                return;
            }

            if (TimeManager.DECAY_ENABLED && FoodConfig.canRot(stack)) {
                long lifetime = FoodConfig.getItemLifetime(stack);

                if (lifetime == FoodConfig.SHELF_LIFE_INFINITE) {
                    event.getToolTip().add(Component.literal("保质期: 永久保鲜").withStyle(ChatFormatting.GOLD));
                    addFreshFoodEffects(event, stack);
                    addFoodTagsInfo(event, stack);
                    return;
                }

                net.minecraft.world.level.Level level = event.getEntity() != null ? event.getEntity().level() : null;
                if (level == null) return;

                String lifeStr = FreshnessHelper.formatDuration(lifetime);
                event.getToolTip().add(Component.literal("保质期: " + lifeStr).withStyle(ChatFormatting.BLUE));

                long expiry = FreshnessHelper.getExpiryTime(level, stack, false);
                long remaining = (expiry == Long.MAX_VALUE) ? lifetime : Math.max(0, expiry - TimeManager.getEffectiveTime(level));

                String remainStr = FreshnessHelper.formatDuration(remaining);
                float percent = (float) remaining / lifetime;

                ChatFormatting color = percent > 0.5f ? ChatFormatting.GREEN : (percent > 0.2f ? ChatFormatting.YELLOW : ChatFormatting.RED);
                event.getToolTip().add(Component.literal("距离腐烂: " + remainStr).withStyle(color));

                addFreshnessStatus(event, percent, stack);
                addFoodTagsInfo(event, stack);
            }
        }

        private static void addFreshnessStatus(net.minecraftforge.event.entity.player.ItemTooltipEvent event, float percent, ItemStack stack) {
            String status;
            ChatFormatting color;
            if (percent >= 0.8f) { status = "新鲜"; color = ChatFormatting.GREEN; }
            else if (percent >= 0.5f) { status = "不新鲜"; color = ChatFormatting.YELLOW; }
            else if (percent >= 0.3f) { status = "略微变质"; color = ChatFormatting.GOLD; }
            else if (percent >= 0.1f) { status = "变质"; color = ChatFormatting.RED; }
            else { status = "严重变质"; color = ChatFormatting.DARK_RED; }

            event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(color));
            if (percent >= 0.8f) addFreshFoodEffects(event, stack);
        }

        private static void addFreshFoodEffects(net.minecraftforge.event.entity.player.ItemTooltipEvent event, ItemStack stack) {
            List<FoodConfig.EffectBonus> bonuses = FoodConfig.getBonusEffects(stack);
            if (bonuses != null && !bonuses.isEmpty()) {
                boolean hasBonus = false;
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.LIGHT_PURPLE));
                for (FoodConfig.EffectBonus bonus : bonuses) {
                    if (bonus.chance > 0) {
                        hasBonus = true;
                        String name = bonus.effect.getDescriptionId();
                        event.getToolTip().add(Component.literal(" - " + (int)(bonus.chance*100) + "% " + name).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
                if (!hasBonus) event.getToolTip().add(Component.literal(" 无").withStyle(ChatFormatting.GRAY));
            }
        }

        private static void addFoodTagsInfo(net.minecraftforge.event.entity.player.ItemTooltipEvent event, ItemStack stack) {
            Set<String> tags = FoodConfig.getFoodTags(stack);
            if (!tags.isEmpty()) {
                String classification = null;
                Set<String> features = new HashSet<>();
                String nature = null;

                // 使用 CookednessHelper 获取熟度信息 (自动处理 NBT/Config 优先级)
                boolean hasDynamicCooked = CookednessHelper.hasDynamicCookedness(stack);

                for (String tag : tags) {
                    if (tag.startsWith("分类:")) {
                        classification = tag.substring(3);
                    } else if (tag.startsWith("特点:")) {
                        features.add(tag.substring(3));
                    } else if (tag.startsWith("熟度:")) {
                        // 使用 Helper 统一获取显示的熟度字符串
                        nature = CookednessHelper.getCookednessDisplayString(stack);
                    }
                }

                // 如果 JSON 里没配熟度标签，但物品确实有 NBT 熟度，也强制显示
                if (nature == null && hasDynamicCooked) {
                    nature = CookednessHelper.getCookednessDisplayString(stack);
                }

                event.getToolTip().add(Component.literal("食物属性:").withStyle(ChatFormatting.AQUA));
                if (classification != null) {
                    event.getToolTip().add(Component.literal("分类: " + classification).withStyle(ChatFormatting.GRAY));
                }
                if (!features.isEmpty()) {
                    StringBuilder featuresStr = new StringBuilder();
                    for (String feature : features) {
                        if (featuresStr.length() > 0) {
                            featuresStr.append(", ");
                        }
                        featuresStr.append(feature);
                    }
                    event.getToolTip().add(Component.literal("特点: " + featuresStr.toString()).withStyle(ChatFormatting.GRAY));
                }
                if (nature != null) {
                    // 使用 Helper 获取数值以确定颜色
                    float cookedValue = CookednessHelper.getCurrentCookedness(stack);
                    ChatFormatting natureColor = ChatFormatting.GRAY;
                    if (hasDynamicCooked) {
                        if (cookedValue >= 100f) natureColor = ChatFormatting.RED;   // 熟了/焦了
                        else if (cookedValue > 0f) natureColor = ChatFormatting.GREEN; // 正在煮
                    }

                    event.getToolTip().add(Component.literal("熟度: " + nature).withStyle(natureColor));
                }
            }

            // === 菜品评价显示 ===
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag.contains("DishScore")) {
                    float score = tag.getFloat("DishScore");
                    float avgFreshness = tag.getFloat("DishFreshness");
                    float avgCookednessDeviation = tag.getFloat("DishCookednessDeviation");

                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.literal("==== 菜品评价 ====").withStyle(ChatFormatting.GOLD));

                    // 根据分数变色
                    ChatFormatting scoreColor = ChatFormatting.RED;
                    if (score >= 90) scoreColor = ChatFormatting.GOLD;
                    else if (score >= 75) scoreColor = ChatFormatting.GREEN;
                    else if (score >= 60) scoreColor = ChatFormatting.YELLOW;

                    event.getToolTip().add(Component.literal(" 综合评分: " + String.format("%.1f", score)).withStyle(scoreColor));
                    event.getToolTip().add(Component.literal(" 食材新鲜: " + String.format("%.1f", avgFreshness) + "%").withStyle(ChatFormatting.GRAY));
                    event.getToolTip().add(Component.literal(" 火候控制: " + (avgCookednessDeviation < 5 ? "完美" : String.format("偏差 %.1f", avgCookednessDeviation))).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}