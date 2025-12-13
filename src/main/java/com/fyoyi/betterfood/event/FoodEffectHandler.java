package com.fyoyi.betterfood.event;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FoodEffectHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        ItemStack stack = event.getItem();

        // 1. 腐肉特殊处理
        if (stack.getItem() == Items.ROTTEN_FLESH) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25 * 20, 0));
            // 80% 几率二选一
            if (RANDOM.nextFloat() < 0.8f) {
                applyRandomBadEffect(player, 10 * 20); // 5秒
            }
            return;
        }

        // 2. 普通食物处理
        if (!TimeManager.DECAY_ENABLED || !FoodConfig.canRot(stack)) {
            return;
        }

        float percent = FreshnessHelper.getFreshnessPercentage(player.level(), stack);

        // --- 阶段判定 ---

        // A. 新鲜阶段 (80% - 100%)
        if (percent >= 0.8f) {
            // 【核心修改】读取你在 FoodConfig 里定义的接口数据
            java.util.List<FoodConfig.EffectBonus> bonuses = FoodConfig.getBonusEffects(stack);

            // 如果你在 Config 里给这个食物定义了奖励
            if (bonuses != null && !bonuses.isEmpty()) {
                for (FoodConfig.EffectBonus bonus : bonuses) {
                    // 判断概率
                    if (RANDOM.nextFloat() < bonus.chance) {
                        // 给予效果 (duration * 20 把秒转为tick)
                        player.addEffect(new MobEffectInstance(bonus.effect, bonus.durationSeconds * 20, bonus.amplifier));
                    }
                }
            }
        }
        // B. 不新鲜 (60% - 80%)
        else if (percent >= 0.6f) {
            // 无效果
        }
        // C. 略微变质 (40% - 60%)
        else if (percent >= 0.4f) {
            if (RANDOM.nextFloat() < 0.3f) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10 * 20, 0));
            }
        }
        // D. 变质 (20% - 40%)
        else if (percent >= 0.2f) {
            if (RANDOM.nextFloat() < 0.5f) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 15 * 20, 0));
            }
            if (RANDOM.nextFloat() < 0.1f) {
                applyRandomBadEffect(player, 5 * 20);
            }
        }
        // E. 严重变质 (0% - 20%)
        else {
            if (RANDOM.nextFloat() < 0.8f) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 20, 0));
            }
            if (RANDOM.nextFloat() < 0.5f) {
                applyRandomBadEffect(player, 5 * 20);
            }
        }
    }

    /**
     * 随机坏效果 (二选一)
     */
    private static void applyRandomBadEffect(Player player, int duration) {
        if (RANDOM.nextBoolean()) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        }
    }
}