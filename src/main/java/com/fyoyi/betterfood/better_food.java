package com.fyoyi.betterfood;

// === 核心工具类引用 ===
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.FoodExpiryManager;
import com.fyoyi.betterfood.config.FoodConfig;
// ====================
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

@Mod(better_food.MOD_ID)
public class better_food
{
    public static final String MOD_ID = "better_food";
    private static final Logger LOGGER = LogUtils.getLogger();

    public better_food(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 注册服务端事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // >>> 【核心】注册资源重载监听器 (用于读取 JSON 配置) <<<
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);

        // 手动注册物品栏装饰器 (画耐久条)
        modEventBus.addListener(ClientModEvents::registerItemDecorations);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // === 【新增】事件回调：添加 JSON 读取器 ===
    public void addReloadListener(AddReloadListenerEvent event) {
        // 注册 FoodExpiryManager，它现在会同时处理保质期和新鲜奖励
        event.addListener(new FoodExpiryManager());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }

    // =================================================================
    // 客户端事件内部类 (负责渲染和提示)
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
        // 功能 1：Tooltip (显示保质期文字)
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
                return;
            }

            // 只要腐烂开启，且是可腐烂食物 (FoodConfig 判断)，就显示
            if (TimeManager.DECAY_ENABLED && FoodConfig.canRot(stack)) {

                // 1. 获取总寿命 (从 JSON 配置或默认值获取)
                long lifetime = FoodConfig.getItemLifetime(stack);

                // === 如果是无限保质期 (-1)，显示特殊文字并直接返回 ===
                if (lifetime == FoodConfig.SHELF_LIFE_INFINITE) {
                    event.getToolTip().add(Component.literal("保质期: 永久保鲜").withStyle(ChatFormatting.GOLD));
                    event.getToolTip().add(Component.literal("新鲜度: 永久新鲜").withStyle(ChatFormatting.AQUA));
                    // 显示新鲜食物效果
                    addFreshFoodEffects(event, stack);
                    return; // 不需要显示倒计时
                }

                net.minecraft.world.level.Level level = event.getEntity() != null ? event.getEntity().level() : null;
                if (level == null) return;

                // 2. 正常显示保质期
                String lifeStr = FreshnessHelper.formatDuration(lifetime);
                event.getToolTip().add(Component.literal("保质期: " + lifeStr).withStyle(ChatFormatting.BLUE));

                // 3. 计算剩余时间
                long expiry = FreshnessHelper.getExpiryTime(level, stack, false);
                long remaining;

                if (expiry == Long.MAX_VALUE) {
                    remaining = lifetime; // 全新
                } else {
                    long now = TimeManager.getEffectiveTime(level);
                    remaining = Math.max(0, expiry - now);
                }

                // 4. 显示倒计时
                String remainStr = FreshnessHelper.formatDuration(remaining);
                ChatFormatting color;
                float percent = (float) remaining / lifetime;

                if (percent > 0.5f) color = ChatFormatting.GREEN;
                else if (percent > 0.2f) color = ChatFormatting.YELLOW;
                else color = ChatFormatting.RED;

                event.getToolTip().add(Component.literal("距离腐烂: " + remainStr).withStyle(color));

                // 5. 显示新鲜度等级和对应效果
                addFreshnessStatus(event, percent, stack);
            }
        }

        /**
         * 添加新鲜度状态和食用效果提示
         */
        private static void addFreshnessStatus(net.minecraftforge.event.entity.player.ItemTooltipEvent event, float percent, ItemStack stack) {
            String status;
            ChatFormatting statusColor;
            
            // 新鲜度等级判定
            if (percent >= 0.8f) {
                status = "新鲜";
                statusColor = ChatFormatting.GREEN;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                addFreshFoodEffects(event, stack);
            } else if (percent >= 0.6f) {
                status = "不新鲜";
                statusColor = ChatFormatting.YELLOW;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果: 无").withStyle(ChatFormatting.GRAY));
            } else if (percent >= 0.4f) {
                status = "略微变质";
                statusColor = ChatFormatting.GOLD;
                event.getToolTip().add(Component.literal("新鲜度: " + status).withStyle(statusColor));
                event.getToolTip().add(Component.literal("食用效果: 30%概率获得饥饿 (10秒)").withStyle(ChatFormatting.GOLD));
            } else if (percent >= 0.2f) {
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

        /**
         * 添加新鲜食物的奖励效果提示
         */
        private static void addFreshFoodEffects(net.minecraftforge.event.entity.player.ItemTooltipEvent event, ItemStack stack) {
            java.util.List<FoodConfig.EffectBonus> bonuses = FoodConfig.getBonusEffects(stack);
            if (bonuses != null && !bonuses.isEmpty()) {
                event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.LIGHT_PURPLE));
                for (FoodConfig.EffectBonus bonus : bonuses) {
                    int chancePercent = (int)(bonus.chance * 100);
                    String effectName = getEffectName(bonus.effect);
                    String amplifierStr = bonus.amplifier > 0 ? (" " + toRoman(bonus.amplifier + 1)) : "";
                    event.getToolTip().add(Component.literal(" - " + chancePercent + "%概率获得" + effectName + amplifierStr + " (" + bonus.durationSeconds + "秒)").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                event.getToolTip().add(Component.literal("食用效果: 无额外效果").withStyle(ChatFormatting.GRAY));
            }
        }

        /**
         * 获取效果名称 (中文)
         */
        private static String getEffectName(net.minecraft.world.effect.MobEffect effect) {
            if (effect == net.minecraft.world.effect.MobEffects.SATURATION) return "饱和";
            if (effect == net.minecraft.world.effect.MobEffects.REGENERATION) return "生命恢复";
            if (effect == net.minecraft.world.effect.MobEffects.ABSORPTION) return "伤害吸收";
            if (effect == net.minecraft.world.effect.MobEffects.HEALTH_BOOST) return "生命提升";
            if (effect == net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED) return "速度";
            if (effect == net.minecraft.world.effect.MobEffects.DIG_SPEED) return "急迫";
            if (effect == net.minecraft.world.effect.MobEffects.DAMAGE_BOOST) return "力量";
            if (effect == net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE) return "抗性提升";
            if (effect == net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) return "防火";
            if (effect == net.minecraft.world.effect.MobEffects.WATER_BREATHING) return "水下呼吸";
            if (effect == net.minecraft.world.effect.MobEffects.NIGHT_VISION) return "夜视";
            if (effect == net.minecraft.world.effect.MobEffects.JUMP) return "跳跃提升";
            if (effect == net.minecraft.world.effect.MobEffects.LUCK) return "幸运";
            return effect.getDescriptionId();
        }

        /**
         * 将数字转为罗马数字 (1=I, 2=II, 3=III, ...)
         */
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

        // ========================================================
        // 功能 2：渲染耐久条 (新鲜度条)
        // ========================================================
        public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
            System.out.println(">>> BetterFood DEBUG: 装饰器正在注册！ <<<");

            for (Item item : ForgeRegistries.ITEMS) {
                // 使用 Config 检查
                if (FoodConfig.canRot(item.getDefaultInstance())) {

                    event.register(item, (graphics, font, stack, x, y) -> {
                        if (Minecraft.getInstance().level == null) return false;

                        // 使用 Helper 计算百分比 (内部会自动处理 -1 返回 1.0)
                        float percent = FreshnessHelper.getFreshnessPercentage(Minecraft.getInstance().level, stack);

                        // 只有不满的时候显示条 (percent < 1.0)
                        if (percent < 1.0F) {
                            int barWidth = Math.round(13.0F * percent);
                            int color = java.awt.Color.HSBtoRGB(percent / 3.0F, 1.0F, 1.0F);

                            graphics.pose().pushPose();
                            graphics.pose().translate(0, 0, 200);

                            // 黑底
                            graphics.fill(x + 2, y + 13, x + 15, y + 15, 0xFF000000);
                            // 彩条
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
