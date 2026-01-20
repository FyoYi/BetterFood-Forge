package com.fyoyi.betterfood;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.CookednessHelper;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = better_food.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // 腐肉特殊处理
        if (stack.getItem() == Items.ROTTEN_FLESH) {
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

            var level = event.getEntity() != null ? event.getEntity().level() : null;
            if (level == null)
                return;

            String lifeStr = FreshnessHelper.formatDuration(lifetime);
            event.getToolTip().add(Component.literal("保质期: " + lifeStr).withStyle(ChatFormatting.BLUE));

            long expiry = FreshnessHelper.getExpiryTime(level, stack, false);
            long remaining = (expiry == Long.MAX_VALUE) ? lifetime
                    : Math.max(0, expiry - TimeManager.getEffectiveTime(level));
            String remainStr = FreshnessHelper.formatDuration(remaining);
            float percent = (float) remaining / lifetime;

            ChatFormatting color = percent > 0.5f ? ChatFormatting.GREEN
                    : (percent > 0.2f ? ChatFormatting.YELLOW : ChatFormatting.RED);
            event.getToolTip().add(Component.literal("距离腐烂: " + remainStr).withStyle(color));

            addFreshFoodEffects(event, stack);
            addFoodTagsInfo(event, stack);
            return;
        }

        // 其他食物处理
        if (FoodConfig.canRot(stack)) {
            addFreshFoodEffects(event, stack);
            addFoodTagsInfo(event, stack);
        }
    }

    private static void addFreshFoodEffects(ItemTooltipEvent event, ItemStack stack) {
        List<FoodConfig.EffectBonus> bonuses = FoodConfig.getBonusEffects(stack);
        if (!bonuses.isEmpty()) {
            event.getToolTip().add(Component.literal("食用效果:").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (FoodConfig.EffectBonus bonus : bonuses) {
                String name = bonus.effect.getDisplayName().getString();
                event.getToolTip().add(Component.literal(" - " + (int) (bonus.chance * 100) + "% " + name)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            event.getToolTip().add(Component.literal("食用效果: 无").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addFoodTagsInfo(ItemTooltipEvent event, ItemStack stack) {
        Set<String> tags = FoodConfig.getFoodTags(stack);
        if (!tags.isEmpty()) {
            event.getToolTip().add(Component.literal("食物属性:").withStyle(ChatFormatting.AQUA));
            String classification = tags.stream().filter(t -> t.startsWith("分类:")).findFirst().orElse("");
            if (!classification.isEmpty()) {
                event.getToolTip().add(Component.literal(classification).withStyle(ChatFormatting.GRAY));
            }

            List<String> features = tags.stream().filter(t -> t.startsWith("特点:")).toList();
            if (!features.isEmpty()) {
                StringBuilder featuresStr = new StringBuilder();
                for (String feature : features) {
                    if (featuresStr.length() > 0)
                        featuresStr.append(", ");
                    featuresStr.append(feature.substring(3)); // 移除"特点:"
                }
                event.getToolTip()
                        .add(Component.literal("特点: " + featuresStr.toString()).withStyle(ChatFormatting.GRAY));
            }
        }

        // 熟度信息
        float cookedness = CookednessHelper.getCurrentCookedness(stack);
        if (cookedness > 0.0f) {
            String nature;
            ChatFormatting natureColor = ChatFormatting.GRAY;
            if (cookedness >= 120.0f) {
                nature = "焦了";
                natureColor = ChatFormatting.RED; // 熟了/焦了
            } else if (cookedness >= 80.0f) {
                nature = "熟了";
                natureColor = ChatFormatting.RED;
            } else if (cookedness >= 50.0f) {
                nature = "正在煮";
                natureColor = ChatFormatting.GREEN; // 正在煮
            } else {
                nature = String.format("%.0f%%", cookedness);
            }
            event.getToolTip().add(Component.literal("熟度: " + nature).withStyle(natureColor));
        }

        // 菜品评价 (如果有DishScore NBT)
        if (stack.hasTag() && stack.getTag().contains("DishScore")) {
            float score = stack.getTag().getFloat("DishScore");
            float avgFreshness = stack.getTag().getFloat("DishFreshness");
            float avgCookednessDeviation = stack.getTag().getFloat("DishCookednessDeviation");

            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.literal("==== 菜品评价 ====").withStyle(ChatFormatting.GOLD));

            // 根据分数变色
            ChatFormatting scoreColor = ChatFormatting.RED;
            if (score >= 90)
                scoreColor = ChatFormatting.GOLD;
            else if (score >= 75)
                scoreColor = ChatFormatting.GREEN;
            else if (score >= 60)
                scoreColor = ChatFormatting.YELLOW;

            event.getToolTip().add(Component.literal(" 综合评分: " + String.format("%.1f", score)).withStyle(scoreColor));
            event.getToolTip().add(Component.literal(" 食材新鲜: " + String.format("%.1f", avgFreshness) + "%")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip()
                    .add(Component.literal(" 火候控制: "
                            + (avgCookednessDeviation < 5 ? "完美" : String.format("偏差 %.1f", avgCookednessDeviation)))
                            .withStyle(ChatFormatting.GRAY));
        }
    }
}