/*
 * Better Food 模组主类
 * 该类是整个模组的入口点，负责初始化模组的各种组件和服务
 */
package com.fyoyi.betterfood;

// === 核心工具类引用 ===
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.FoodExpiryManager;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.item.ModItems;
import com.fyoyi.betterfood.ModCreativeModeTabs;
import com.fyoyi.betterfood.block.entity.ModBlockEntities;
import com.fyoyi.betterfood.client.renderer.PotRendererDispatcher;
import com.fyoyi.betterfood.client.gui.PotInfoOverlay;
import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import com.fyoyi.betterfood.recipe.CulinaryRecipeSerializer;
import com.fyoyi.betterfood.network.NetworkManager;
import com.fyoyi.betterfood.client.renderer.PotWorldRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
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
import org.slf4j.Logger;

import java.util.Set;
import java.util.HashSet;

@Mod(better_food.MOD_ID)
public class better_food
{
    /*
     * 模组ID常量
     */
    public static final String MOD_ID = "better_food";

    /*
     * 日志记录器实例
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     * 模组构造函数
     */
    public better_food(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 注册通用设置
        modEventBus.addListener(this::commonSetup);

        // 注册方块、物品、实体等
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);

        // 注册配置
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册网络
        NetworkManager.register();

        // 注册世界渲染器
        PotWorldRenderer.register();

        // ========================================================
        // 客户端相关注册 (手动监听 Mod 总线事件)
        // ========================================================

        // 1. 注册物品栏装饰器 (新鲜度条)
        modEventBus.addListener(ClientModEvents::registerItemDecorations);

        // 2. 注册实体渲染器 (让锅里能显示物品 - TESR)
        modEventBus.addListener(ClientModEvents::registerRenderers);

        // 3. 【核心新增】注册 GUI Overlay (屏幕右侧的锅具信息)
        modEventBus.addListener(ClientModEvents::registerGuiOverlays);


        // ========================================================
        // Forge 总线事件
        // ========================================================

        // 注册服务端事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // 注册资源重载监听器
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FoodExpiryManager());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }

    // =================================================================
    // 客户端事件内部类 (负责渲染和提示)
    // 注解 bus = Bus.FORGE 是为了自动监听 ItemTooltipEvent
    // 其他 Mod 总线事件(如渲染器注册)已在构造函数中手动绑定，互不冲突
    // =================================================================
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("HELLO FROM CLIENT SETUP");
        }

        // ========================================================
        // 注册方块实体渲染器 (PotRendererDispatcher)
        // ========================================================
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // 使用调度器为不同类型的锅具选择合适的渲染器
            event.registerBlockEntityRenderer(ModBlockEntities.POT_BE.get(), PotRendererDispatcher::new);
        }

        // ========================================================
        // 【新增】注册 GUI Overlay (PotInfoOverlay)
        // ========================================================
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            // 注册到原版 HUD 之上，ID 为 "pot_info"
            event.registerAboveAll("pot_info", PotInfoOverlay.INSTANCE);
        }

        // ========================================================
        // 功能：Tooltip (显示保质期文字、标签等)
        // ========================================================
        @SubscribeEvent
        public static void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            net.minecraft.world.item.ItemStack stack = event.getItemStack();

            // 特殊处理：腐肉
            if (stack.getItem() == net.minecraft.world.item.Items.ROTTEN_FLESH) {
                event.getToolTip().add(Component.literal("新鲜度: 已腐烂").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal(" - 100%概率获得饥饿 (25秒)").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal(" - 80%概率获得中毒/反胃 (5秒)").withStyle(ChatFormatting.DARK_RED));
                addFoodTagsInfo(event, stack);
                return;
            }

            if (TimeManager.DECAY_ENABLED && FoodConfig.canRot(stack)) {
                long lifetime = FoodConfig.getItemLifetime(stack);

                if (lifetime == FoodConfig.SHELF_LIFE_INFINITE) {
                    event.getToolTip().add(Component.literal("保质期: 永久保鲜").withStyle(ChatFormatting.GOLD));
                    event.getToolTip().add(Component.literal("新鲜度: 永久新鲜").withStyle(ChatFormatting.AQUA));
                    addFreshFoodEffects(event, stack);
                    addFoodTagsInfo(event, stack);
                    return;
                }

                net.minecraft.world.level.Level level = event.getEntity() != null ? event.getEntity().level() : null;
                if (level == null) return;

                String lifeStr = FreshnessHelper.formatDuration(lifetime);
                event.getToolTip().add(Component.literal("保质期: " + lifeStr).withStyle(ChatFormatting.BLUE));

                long expiry = FreshnessHelper.getExpiryTime(level, stack, false);
                long remaining;

                if (expiry == Long.MAX_VALUE) {
                    remaining = lifetime;
                } else {
                    long now = TimeManager.getEffectiveTime(level);
                    remaining = Math.max(0, expiry - now);
                }

                String remainStr = FreshnessHelper.formatDuration(remaining);
                ChatFormatting color;
                float percent = (float) remaining / lifetime;

                if (percent > 0.5f) color = ChatFormatting.GREEN;
                else if (percent > 0.2f) color = ChatFormatting.YELLOW;
                else color = ChatFormatting.RED;

                event.getToolTip().add(Component.literal("距离腐烂: " + remainStr).withStyle(color));
                addFreshnessStatus(event, percent, stack);
                addFoodTagsInfo(event, stack);
            }
        }

        private static void addFreshnessStatus(net.minecraftforge.event.entity.player.ItemTooltipEvent event, float percent, ItemStack stack) {
            String status;
            ChatFormatting statusColor;

            if (percent >= 0.8f) {
                status = "新鲜";
                statusColor = ChatFormatting.GREEN;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                addFreshFoodEffects(event, stack);
            } else if (percent >= 0.5f) {
                status = "不新鲜";
                statusColor = ChatFormatting.YELLOW;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果: 无").withStyle(ChatFormatting.GRAY));
            } else if (percent >= 0.3f) {
                status = "略微变质";
                statusColor = ChatFormatting.GOLD;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果: 30%概率获得饥饿 (10秒)").withStyle(ChatFormatting.GOLD));
            } else if (percent >= 0.1f) {
                status = "变质";
                statusColor = ChatFormatting.RED;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.RED));
                event.getToolTip().add(Component.literal(" - 50%概率获得饥饿 (15秒)").withStyle(ChatFormatting.RED));
                event.getToolTip().add(Component.literal(" - 10%概率获得中毒/反胃 (5秒)").withStyle(ChatFormatting.RED));
            } else {
                status = "严重变质";
                statusColor = ChatFormatting.DARK_RED;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal(" - 80%概率获得饥饿 (20秒)").withStyle(ChatFormatting.DARK_RED));
                event.getToolTip().add(Component.literal(" - 50%概率获得中毒/反胃 (5秒)").withStyle(ChatFormatting.DARK_RED));
            }
        }

        private static void addFreshFoodEffects(net.minecraftforge.event.entity.player.ItemTooltipEvent event, ItemStack stack) {
            java.util.List<FoodConfig.EffectBonus> bonuses = FoodConfig.getBonusEffects(stack);
            if (bonuses != null && !bonuses.isEmpty()) {
                java.util.List<FoodConfig.EffectBonus> validBonuses = new java.util.ArrayList<>();
                for (FoodConfig.EffectBonus bonus : bonuses) {
                    if (bonus.chance > 0.0f) {
                        validBonuses.add(bonus);
                    }
                }

                if (!validBonuses.isEmpty()) {
                    event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.LIGHT_PURPLE));
                    for (FoodConfig.EffectBonus bonus : validBonuses) {
                        int chancePercent = (int)(bonus.chance * 100);
                        String effectName = getEffectName(bonus.effect);
                        String amplifierStr = bonus.amplifier > 0 ? (" " + toRoman(bonus.amplifier + 1)) : "";

                        String durationStr;
                        if (bonus.effect == net.minecraft.world.effect.MobEffects.SATURATION) {
                            int hunger = bonus.durationSeconds / 2;
                            durationStr = "+" + hunger + "饥饿度";
                        } else {
                            durationStr = bonus.durationSeconds + "秒";
                        }

                        event.getToolTip().add(Component.literal(" - " + chancePercent + "%概率获得" + effectName + amplifierStr + " (" + durationStr + ")").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                } else {
                    event.getToolTip().add(Component.literal("食用效果: 无额外效果").withStyle(ChatFormatting.GRAY));
                }
            } else {
                event.getToolTip().add(Component.literal("食用效果: 无额外效果").withStyle(ChatFormatting.GRAY));
            }
        }

        private static String getEffectName(net.minecraft.world.effect.MobEffect effect) {
            if (effect == net.minecraft.world.effect.MobEffects.SATURATION) return "饱和";
            if (effect == net.minecraft.world.effect.MobEffects.REGENERATION) return "生命恢复";
            if (effect == net.minecraft.world.effect.MobEffects.ABSORPTION) return "伤害吸收";
            if (effect == net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) return "防火";
            if (effect == net.minecraft.world.effect.MobEffects.WATER_BREATHING) return "水下呼吸";
            if (effect == net.minecraft.world.effect.MobEffects.LUCK) return "幸运";
            return effect.getDescriptionId();
        }

        private static String toRoman(int number) {
            return switch (number) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(number);
            };
        }

        private static void addFoodTagsInfo(net.minecraftforge.event.entity.player.ItemTooltipEvent event, ItemStack stack) {
            Set<String> tags = FoodConfig.getFoodTags(stack);
            if (!tags.isEmpty()) {
                String classification = null;
                Set<String> features = new HashSet<>();
                String nature = null;

                // 使用CookednessHelper获取熟度信息
                float currentCooked = com.fyoyi.betterfood.util.CookednessHelper.getCurrentCookedness(stack);
                boolean hasDynamicCooked = com.fyoyi.betterfood.util.CookednessHelper.hasDynamicCookedness(stack);

                for (String tag : tags) {
                    if (tag.startsWith("分类:")) {
                        classification = tag.substring(3);
                    } else if (tag.startsWith("特点:")) {
                        features.add(tag.substring(3));
                    } else if (tag.startsWith("熟度:")) {
                        // 使用CookednessHelper获取统一的熟度显示
                        nature = com.fyoyi.betterfood.util.CookednessHelper.getCookednessDisplayString(stack);
                    }
                }

                // 如果 JSON 里没配熟度标签，但物品确实被煮过，也强制显示
                if (nature == null && hasDynamicCooked) {
                    nature = com.fyoyi.betterfood.util.CookednessHelper.getCookednessDisplayString(stack);
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
                    // 使用CookednessHelper获取熟度值以确定颜色
                    float cookedValue = com.fyoyi.betterfood.util.CookednessHelper.getCurrentCookedness(stack);
                    ChatFormatting natureColor = ChatFormatting.GRAY;
                    if (hasDynamicCooked) {
                        if (cookedValue >= 100f) natureColor = ChatFormatting.RED;   // 熟了/焦了
                        else if (cookedValue > 0f) natureColor = ChatFormatting.GREEN; // 正在煮
                    }

                    event.getToolTip().add(Component.literal("熟度: " + nature).withStyle(natureColor));
                }
            }
        }

        // ... 后面的 registerItemDecorations 等保持不变 ...

        // ========================================================
        // 渲染耐久条 (新鲜度条)
        // ========================================================
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
}