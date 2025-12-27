package com.fyoyi.betterfood.command;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.UserConfigManager;
import com.fyoyi.betterfood.util.CookednessHelper;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("betterfood")
                        .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource())))

                        // === 智能菜单 ===
                        .then(Commands.literal("menu").requires(s -> s.hasPermission(2)).executes(ctx -> showSetMenu(ctx.getSource())))

                        // === 属性菜单 ===
                        .then(Commands.literal("attr_menu").requires(s -> s.hasPermission(2)).executes(ctx -> showAttrMenu(ctx.getSource())))

                        // === 奖励菜单 ===
                        .then(Commands.literal("bonus_menu").requires(s -> s.hasPermission(2)).executes(ctx -> showBonusMenu(ctx.getSource())))
                        
                        // === 奖励细节菜单 ===
                        .then(Commands.literal("bonus_detail_menu").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> showBonusDetailMenu(ctx.getSource(), StringArgumentType.getString(ctx, "effect")))
                                )
                        )
                        
                        // === 特点切换指令 ===
                        .then(Commands.literal("toggle_feature").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("feature", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            // 提供常用特点建议
                                            builder.suggest("猪肉");
                                            builder.suggest("牛肉");
                                            builder.suggest("鸡肉");
                                            builder.suggest("羊肉");
                                            builder.suggest("怪物肉");
                                            builder.suggest("兔肉");
                                            builder.suggest("黄金");
                                            builder.suggest("毒素");
                                            builder.suggest("附魔");
                                            builder.suggest("酒类");
                                            builder.suggest("甜食");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String feature = StringArgumentType.getString(ctx, "feature");
                                            return toggleFeature(ctx.getSource(), feature);
                                        })
                                )
                        )
                        
                        // === 设置分类指令 ===
                        .then(Commands.literal("set_classification").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("classification", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            // 提供常用分类建议
                                            builder.suggest("蔬菜");
                                            builder.suggest("肉类");
                                            builder.suggest("饮品");
                                            builder.suggest("鱼类");
                                            builder.suggest("水果");
                                            builder.suggest("谷物");
                                            builder.suggest("汤食");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String classification = StringArgumentType.getString(ctx, "classification");
                                            return setClassification(ctx.getSource(), classification);
                                        })
                                )
                        )
                        
                        // === 设置性质指令 ===
                        .then(Commands.literal("set_nature").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("nature", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            // 提供常用性质建议
                                            builder.suggest("生食");
                                            builder.suggest("熟食");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String nature = StringArgumentType.getString(ctx, "nature");
                                            return setNature(ctx.getSource(), nature);
                                        })
                                )
                        )
                        
                        // === 重载 ===
                        .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ctx -> reloadConfig(ctx.getSource())))
                        
                        // === 初始化所有属性设置 ===
                        .then(Commands.literal("reset_all_attrs").requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                        return 0;
                                    }
                                    
                                    ItemStack stack = player.getMainHandItem();
                                    if (stack.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                        return 0;
                                    }
                                    
                                    Item item = stack.getItem();
                                    if (!item.isEdible()) {
                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                        return 0;
                                    }
                                    
                                    // 移除用户配置中的标签覆盖
                                    UserConfigManager.removeTagsOverride(item);
                                    
                                    // 重新加载配置
                                    reloadConfig(ctx.getSource());
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal("§e[重置] §f已移除 " + item.getDescriptionId() + " 的所有属性自定义设置"), true);
                                    
                                    // 重新显示属性菜单
                                    return showAttrMenu(ctx.getSource());
                                })
                        )
                        
                        // === 初始化所有设置 ===
                        .then(Commands.literal("reset_all").requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    UserConfigManager.clearAllOverrides();
                                    reloadConfig(ctx.getSource());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§c[警告] 已清空所有用户自定义配置，恢复默认值！"), true);
                                    return 1;
                                })
                        )
                        
                        // === 移除奖励效果 ===
                        .then(Commands.literal("remove_bonus").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String effect = StringArgumentType.getString(ctx, "effect");
                                            
                                            if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                                ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                                return 0;
                                            }
                                            
                                            ItemStack stack = player.getMainHandItem();
                                            if (stack.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                                return 0;
                                            }
                                            
                                            Item item = stack.getItem();
                                            if (!item.isEdible()) {
                                                ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                                return 0;
                                            }
                                            
                                            // 获取现有奖励效果
                                            List<FoodConfig.EffectBonus> currentBonuses = new ArrayList<>(FoodConfig.getBonusEffects(stack));
                                            
                                            // 查找并移除指定效果
                                            int existingIndex = -1;
                                            for (int i = 0; i < currentBonuses.size(); i++) {
                                                FoodConfig.EffectBonus bonus = currentBonuses.get(i);
                                                String effectName = getEffectName(bonus.effect);
                                                if (effectName.equals(effect)) {
                                                    existingIndex = i;
                                                    break;
                                                }
                                            }
                                            
                                            if (existingIndex >= 0) {
                                                currentBonuses.remove(existingIndex);
                                                ctx.getSource().sendSuccess(() -> Component.literal("§d[奖励] §f已移除 " +
                                                        ForgeRegistries.ITEMS.getKey(item) + " 的 " + getEffectChineseName(effect) + " 效果"), true);
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("错误：未找到 " + getEffectChineseName(effect) + " 效果"));
                                                return 0;
                                            }
                                            
                                            // 保存到用户配置
                                            UserConfigManager.saveBonusOverride(item, currentBonuses);
                                            
                                            // 注册到 FoodConfig
                                            FoodConfig.registerBonus(item, currentBonuses);
                                            
                                            // 重新显示奖金细节菜单
                                            return showBonusDetailMenu(ctx.getSource(), effect);
                                        })
                                )
                        )
                        
                        // === 添加奖励效果 ===
                        .then(Commands.literal("add_bonus").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String effect = StringArgumentType.getString(ctx, "effect");
                                            
                                            if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                                ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                                return 0;
                                            }
                                            
                                            ItemStack stack = player.getMainHandItem();
                                            if (stack.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                                return 0;
                                            }
                                            
                                            Item item = stack.getItem();
                                            if (!item.isEdible()) {
                                                ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                                return 0;
                                            }
                                            
                                            // 获取现有奖励效果
                                            List<FoodConfig.EffectBonus> currentBonuses = new ArrayList<>(FoodConfig.getBonusEffects(stack));
                                            
                                            // 检查是否已包含该效果
                                            boolean alreadyExists = false;
                                            for (FoodConfig.EffectBonus bonus : currentBonuses) {
                                                String effectName = getEffectName(bonus.effect);
                                                if (effectName.equals(effect)) {
                                                    alreadyExists = true;
                                                    break;
                                                }
                                            }
                                            
                                            if (alreadyExists) {
                                                ctx.getSource().sendFailure(Component.literal("错误：" + getEffectChineseName(effect) + " 效果已存在"));
                                                return 0;
                                            }
                                            
                                            // 添加默认效果 (80%概率, 10秒, 0级)
                                            net.minecraft.world.effect.MobEffect mobEffect = parseEffect(effect);
                                            if (mobEffect == null) {
                                                ctx.getSource().sendFailure(Component.literal("错误：不支持的效果类型 '" + effect + "'"));
                                                return 0;
                                            }
                                            
                                            FoodConfig.EffectBonus newBonus = new FoodConfig.EffectBonus(mobEffect, 0.8f, 10, 0);
                                            currentBonuses.add(newBonus);
                                            
                                            // 保存到用户配置
                                            UserConfigManager.saveBonusOverride(item, currentBonuses);
                                            
                                            // 注册到 FoodConfig
                                            FoodConfig.registerBonus(item, currentBonuses);
                                            
                                            ctx.getSource().sendSuccess(() -> Component.literal("§d[奖励] §f已添加 " +
                                                    ForgeRegistries.ITEMS.getKey(item) + " 的 " + getEffectChineseName(effect) + " 效果"), true);
                                            
                                            // 重新显示奖金细节菜单
                                            return showBonusDetailMenu(ctx.getSource(), effect);
                                        })
                                )
                        )
                        
                        // === 设置奖励等级 ===
                        .then(Commands.literal("set_bonus_level").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 2))
                                                .executes(ctx -> {
                                                    String effect = StringArgumentType.getString(ctx, "effect");
                                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                                    
                                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                                        return 0;
                                                    }
                                                    
                                                    ItemStack stack = player.getMainHandItem();
                                                    if (stack.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                                        return 0;
                                                    }
                                                    
                                                    Item item = stack.getItem();
                                                    if (!item.isEdible()) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                                        return 0;
                                                    }
                                                    
                                                    // 获取现有奖励效果
                                                    List<FoodConfig.EffectBonus> currentBonuses = new ArrayList<>(FoodConfig.getBonusEffects(stack));
                                                    
                                                    // 查找并更新指定效果的等级
                                                    boolean found = false;
                                                    for (int i = 0; i < currentBonuses.size(); i++) {
                                                        FoodConfig.EffectBonus bonus = currentBonuses.get(i);
                                                        String effectName = getEffectName(bonus.effect);
                                                        if (effectName.equals(effect)) {
                                                            // 创建新的效果对象，只改变等级
                                                            FoodConfig.EffectBonus updatedBonus = new FoodConfig.EffectBonus(
                                                                bonus.effect, bonus.chance, bonus.durationSeconds, level);
                                                            currentBonuses.set(i, updatedBonus);
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    
                                                    if (!found) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：未找到 " + getEffectChineseName(effect) + " 效果"));
                                                        return 0;
                                                    }
                                                    
                                                    // 保存到用户配置
                                                    UserConfigManager.saveBonusOverride(item, currentBonuses);
                                                    
                                                    // 注册到 FoodConfig
                                                    FoodConfig.registerBonus(item, currentBonuses);
                                                    
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§d[奖励] §f已更新 " +
                                                            ForgeRegistries.ITEMS.getKey(item) + " 的 " + getEffectChineseName(effect) + " 等级为 " + (level + 1)), true);
                                                    
                                                    // 重新显示奖金细节菜单
                                                    return showBonusDetailMenu(ctx.getSource(), effect);
                                                })
                                        )
                                )
                        )
                        
                        // === 设置奖励时长 ===
                        .then(Commands.literal("set_bonus_duration").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    String effect = StringArgumentType.getString(ctx, "effect");
                                                    int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                                    
                                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                                        return 0;
                                                    }
                                                    
                                                    ItemStack stack = player.getMainHandItem();
                                                    if (stack.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                                        return 0;
                                                    }
                                                    
                                                    Item item = stack.getItem();
                                                    if (!item.isEdible()) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                                        return 0;
                                                    }
                                                    
                                                    // 获取现有奖励效果
                                                    List<FoodConfig.EffectBonus> currentBonuses = new ArrayList<>(FoodConfig.getBonusEffects(stack));
                                                    
                                                    // 查找并更新指定效果的时长
                                                    boolean found = false;
                                                    for (int i = 0; i < currentBonuses.size(); i++) {
                                                        FoodConfig.EffectBonus bonus = currentBonuses.get(i);
                                                        String effectName = getEffectName(bonus.effect);
                                                        if (effectName.equals(effect)) {
                                                            // 创建新的效果对象，只改变时长
                                                            FoodConfig.EffectBonus updatedBonus = new FoodConfig.EffectBonus(
                                                                bonus.effect, bonus.chance, duration, bonus.amplifier);
                                                            currentBonuses.set(i, updatedBonus);
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    
                                                    if (!found) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：未找到 " + getEffectChineseName(effect) + " 效果"));
                                                        return 0;
                                                    }
                                                    
                                                    // 保存到用户配置
                                                    UserConfigManager.saveBonusOverride(item, currentBonuses);
                                                    
                                                    // 注册到 FoodConfig
                                                    FoodConfig.registerBonus(item, currentBonuses);
                                                    
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§d[奖励] §f已更新 " +
                                                            ForgeRegistries.ITEMS.getKey(item) + " 的 " + getEffectChineseName(effect) + " 时长为 " + duration + "秒"), true);
                                                    
                                                    // 重新显示奖金细节菜单
                                                    return showBonusDetailMenu(ctx.getSource(), effect);
                                                })
                                        )
                                )
                        )

                        // === 设置奖励概率 ===
                        .then(Commands.literal("set_bonus_chance").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("chance", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                .executes(ctx -> {
                                                    String effect = StringArgumentType.getString(ctx, "effect");
                                                    float chance = FloatArgumentType.getFloat(ctx, "chance");
                                                    
                                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                                        return 0;
                                                    }
                                                    
                                                    ItemStack stack = player.getMainHandItem();
                                                    if (stack.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                                        return 0;
                                                    }
                                                    
                                                    Item item = stack.getItem();
                                                    if (!item.isEdible()) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                                        return 0;
                                                    }
                                                    
                                                    // 获取现有奖励效果
                                                    List<FoodConfig.EffectBonus> currentBonuses = new ArrayList<>(FoodConfig.getBonusEffects(stack));
                                                    
                                                    // 查找并更新指定效果的概率
                                                    boolean found = false;
                                                    for (int i = 0; i < currentBonuses.size(); i++) {
                                                        FoodConfig.EffectBonus bonus = currentBonuses.get(i);
                                                        String effectName = getEffectName(bonus.effect);
                                                        if (effectName.equals(effect)) {
                                                            // 创建新的效果对象，只改变概率
                                                            FoodConfig.EffectBonus updatedBonus = new FoodConfig.EffectBonus(
                                                                bonus.effect, chance, bonus.durationSeconds, bonus.amplifier);
                                                            currentBonuses.set(i, updatedBonus);
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    
                                                    if (!found) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：未找到 " + getEffectChineseName(effect) + " 效果"));
                                                        return 0;
                                                    }
                                                    
                                                    // 保存到用户配置
                                                    UserConfigManager.saveBonusOverride(item, currentBonuses);
                                                    
                                                    // 注册到 FoodConfig
                                                    FoodConfig.registerBonus(item, currentBonuses);
                                                    
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§d[奖励] §f已更新 " +
                                                            ForgeRegistries.ITEMS.getKey(item) + " 的 " + getEffectChineseName(effect) + " 概率为 " + String.format("%.0f%%", chance * 100)), true);
                                                    
                                                    // 重新显示奖金细节菜单
                                                    return showBonusDetailMenu(ctx.getSource(), effect);
                                                })
                                        )
                                )
                        )

                        // === 腐烂开关 ===
                        .then(Commands.literal("decay").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("enable", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enable = BoolArgumentType.getBool(ctx, "enable");
                                            TimeManager.DECAY_ENABLED = enable;
                                            if (!enable) TimeManager.setPaused(false, ctx.getSource().getLevel());
                                            String s = enable ? "§a开启" : "§c关闭";
                                            ctx.getSource().sendSuccess(() -> Component.literal("§6[BetterFood]§r 腐烂系统已" + s), true);
                                            return 1;
                                        })
                                )
                        )

                        // === 暂停 ===
                        .then(Commands.literal("pause").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("pause", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean p = BoolArgumentType.getBool(ctx, "pause");
                                            TimeManager.setPaused(p, ctx.getSource().getLevel());
                                            String s = p ? "§e暂停" : "§a继续";
                                            ctx.getSource().sendSuccess(() -> Component.literal("§6[BetterFood]§r 计时器已" + s), true);
                                            return 1;
                                        })
                                )
                        )

                        // === 设置时间 (加入食物校验) ===
                        .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("minutes", FloatArgumentType.floatArg())
                                                .executes(ctx -> {
                                                    Item item = ItemArgument.getItem(ctx, "item").getItem();

                                                    // 必须是食物
                                                    if (!item.isEdible()) {
                                                        ctx.getSource().sendFailure(Component.literal("错误：只能设置食物类物品！"));
                                                        return 0;
                                                    }

                                                    float minutes = FloatArgumentType.getFloat(ctx, "minutes");
                                                    long ticks = (minutes < 0) ? -1 : (long) (minutes * 1200);

                                                    UserConfigManager.saveOverride(item, ticks);
                                                    FoodConfig.register(item, ticks);

                                                    String t = (ticks == -1) ? "§6永久保鲜" : ("§b" + minutes + " 分钟");
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[设置成功] §f" + item.getDescriptionId() + " -> " + t), true);
                                                    
                                                    // 重新加载配置
                                                    reloadConfig(ctx.getSource());
                                                    
                                                    // 刷新当前食物的设置菜单
                                                    return showSetTimeMenu(ctx.getSource());
                                                })
                                        )
                                )
                        )

                        // === 设置奖励 ===
                        .then(Commands.literal("set_bonus").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("effect", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    String[] effects = {"saturation", "regeneration", "absorption", "fire_resistance", "water_breathing", "luck"};
                                                    for (String effect : effects) {
                                                        builder.suggest(effect);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("chance", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 2))
                                                                        .executes(ctx -> {
                                                                            Item item = ItemArgument.getItem(ctx, "item").getItem();
                                                                            
                                                                            if (!item.isEdible()) {
                                                                                ctx.getSource().sendFailure(Component.literal("错误：只能设置食物类物品！"));
                                                                                return 0;
                                                                            }
                                                                            
                                                                            String effectStr = StringArgumentType.getString(ctx, "effect");
                                                                            float chance = FloatArgumentType.getFloat(ctx, "chance");
                                                                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                                                            int amplifier = IntegerArgumentType.getInteger(ctx, "amplifier");
                                                                            
                                                                            // 解析效果类型
                                                                            net.minecraft.world.effect.MobEffect effect = parseEffect(effectStr);
                                                                            if (effect == null) {
                                                                                ctx.getSource().sendFailure(Component.literal("错误：不支持的效果类型 '" + effectStr + "'"));
                                                                                return 0;
                                                                            }
                                                                            
                                                                            // 创建效果奖励
                                                                            java.util.List<FoodConfig.EffectBonus> bonuses = new java.util.ArrayList<>();
                                                                            bonuses.add(new FoodConfig.EffectBonus(effect, chance, duration, amplifier));
                                                                            
                                                                            // 保存到用户配置
                                                                            UserConfigManager.saveBonusOverride(item, bonuses);
                                                                            
                                                                            // 注册到 FoodConfig
                                                                            FoodConfig.registerBonus(item, bonuses);
                                                                            
                                                                            String effectName = getEffectChineseName(effectStr);
                                                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[奖励设置成功] §f" + item.getDescriptionId() + " -> " + effectName + " (概率:" + chance + ", 时间:" + duration + ", 等级:" + amplifier + ")"), true);
                                                                            
                                                                            // 重新加载配置
                                                                            reloadConfig(ctx.getSource());
                                                                            
                                                                            // 刷新奖励菜单
                                                                            return showBonusMenu(ctx.getSource());
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )

                        // === 清除单个设置 ===
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(ctx -> {
                                            Item item = ItemArgument.getItem(ctx, "item").getItem();
                                            UserConfigManager.removeOverride(item);
                                            reloadConfig(ctx.getSource());
                                            ctx.getSource().sendSuccess(() -> Component.literal("§e[重置] §f已移除 " + item.getDescriptionId() + " 的自定义设置"), true);
                                            
                                            // 刷新主设置菜单
                                            return showSetMenu(ctx.getSource());
                                        })
                                )
                        )
                        
                        // === 初始化所有设置（菜单二的初始化按钮）===
                        .then(Commands.literal("init_all_settings").requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                        return 0;
                                    }
                                    
                                    ItemStack stack = player.getMainHandItem();
                                    if (stack.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                        return 0;
                                    }
                                    
                                    Item item = stack.getItem();
                                    if (!item.isEdible()) {
                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                        return 0;
                                    }
                                    
                                    // 1. 移除用户配置中的保质期覆盖
                                    UserConfigManager.removeOverride(item);
                                    
                                    // 2. 移除用户配置中的奖励覆盖
                                    UserConfigManager.removeBonusOverride(item);
                                    
                                    // 3. 移除用户配置中的标签覆盖
                                    UserConfigManager.removeTagsOverride(item);
                                    
                                    // 重新加载配置
                                    reloadConfig(ctx.getSource());
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[初始化] §f已初始化 " + item.getDescriptionId() + " 的所有设置"), true);
                                    
                                    // 重新显示主设置菜单
                                    return showSetMenu(ctx.getSource());
                                })
                        )

                        // === 初始化奖励 ===
                        .then(Commands.literal("init_bonus").requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof Player player)) {
                                        ctx.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
                                        return 0;
                                    }
                                    
                                    ItemStack stack = player.getMainHandItem();
                                    if (stack.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("请先将物品拿在手上！"));
                                        return 0;
                                    }
                                    
                                    Item item = stack.getItem();
                                    if (!item.isEdible()) {
                                        ctx.getSource().sendFailure(Component.literal("错误：当前手持的不是食物"));
                                        return 0;
                                    }
                                    
                                    // 移除用户配置中的奖励覆盖
                                    UserConfigManager.removeBonusOverride(item);
                                    
                                    // 移除用户配置中的标签覆盖
                                    UserConfigManager.removeTagsOverride(item);
                                    
                                    // 移除用户配置中的保质期覆盖
                                    UserConfigManager.removeOverride(item);
                                    
                                    // 重新加载配置
                                    reloadConfig(ctx.getSource());
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[初始化] §f已初始化 " + item.getDescriptionId() + " 的奖励和属性设置"), true);
                                    
                                    // 重新加载配置
                                    reloadConfig(ctx.getSource());
                                    
                                    // 重新显示奖励菜单
                                    return showBonusMenu(ctx.getSource());
                                })
                        )
                        
                        // === 刷新奖金详情菜单 ===
                        .then(Commands.literal("refresh_bonus_detail").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("saturation");
                                            builder.suggest("regeneration");
                                            builder.suggest("absorption");
                                            builder.suggest("fire_resistance");
                                            builder.suggest("water_breathing");
                                            builder.suggest("luck");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String effect = StringArgumentType.getString(ctx, "effect");
                                            
                                            // 重新加载配置
                                            reloadConfig(ctx.getSource());
                                            
                                            // 重新显示奖金详情菜单
                                            return showBonusDetailMenu(ctx.getSource(), effect);
                                        })
                                )
                        )
                        
                        // === 保质期设置菜单 ===
                        .then(Commands.literal("set_menu").requires(s -> s.hasPermission(2))
                                .executes(ctx -> showSetMenu(ctx.getSource()))
                        )
                        
                        // === 保质期时间设置菜单 ===
                        .then(Commands.literal("set_time_menu").requires(s -> s.hasPermission(2))
                                .executes(ctx -> showSetTimeMenu(ctx.getSource()))
                        )

        );
    }

    private static int showSetMenu(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }

        Item item = stack.getItem();

        // 【校验 2】菜单也进行校验
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物，无法设置保质期。"));
            return 0;
        }

        String registryName = ForgeRegistries.ITEMS.getKey(item).toString();

        // --- 头部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r食物配置面板"), false);
        
        // --- 食物基本信息 ---
        source.sendSuccess(() -> Component.literal(" §7名称: " + item.getDescription().getString()), false);
        source.sendSuccess(() -> Component.literal(" §7ID: " + registryName), false);
        
        // 显示当前保质期
        source.sendSuccess(() -> Component.literal(" §7保质期: §f" + getDurationText(item)), false);
        
        // 显示当前奖励效果
        List<FoodConfig.EffectBonus> currentBonuses = FoodConfig.getBonusEffects(stack);
        if (!currentBonuses.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" §7奖励效果:"), false);
            for (FoodConfig.EffectBonus bonus : currentBonuses) {
                String effectName = getEffectChineseName(getEffectName(bonus.effect));
                source.sendSuccess(() -> Component.literal("  §f- " + effectName + " (等级:" + (bonus.amplifier + 1) + ", 概率:" + String.format("%.0f%%", bonus.chance * 100) + ", 时长:" + bonus.durationSeconds + "秒)"), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal(" §7奖励效果: §c无"), false);
        }
        
        // 显示当前特性
        Set<String> currentTags = FoodConfig.getFoodTags(stack);
        String currentClassification = null;
        Set<String> currentFeatures = new HashSet<>();
        String currentNature = null;
        
        for (String tag : currentTags) {
            if (tag.startsWith("分类:")) {
                currentClassification = tag.substring(3);
            } else if (tag.startsWith("特点:")) {
                currentFeatures.add(tag.substring(3));
            } else if (tag.startsWith("熟度:")) {
                currentNature = tag.substring(3);
            }
        }
        
        StringBuilder featuresStr = new StringBuilder();
        if (currentClassification != null) {
            featuresStr.append("分类:").append(currentClassification);
        }
        if (currentNature != null) {
            if (featuresStr.length() > 0) featuresStr.append(", ");
            featuresStr.append("熟度:").append(currentNature);
        }
        for (String feature : currentFeatures) {
            if (featuresStr.length() > 0) featuresStr.append(", ");
            featuresStr.append("特点:").append(feature);
        }
        
        if (featuresStr.length() > 0) {
            source.sendSuccess(() -> Component.literal(" §7特性: §f" + featuresStr.toString()), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7特性: §c无"), false);
        }
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 功能按钮 ---
        MutableComponent buttons = Component.literal(" ");
        
        // 保质期设置按钮
        MutableComponent expiryBtn = Component.literal("[保质期设置]");
        expiryBtn.withStyle(s -> s.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood set_time_menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置食物保质期"))));
        buttons.append(expiryBtn).append(Component.literal("  "));
        
        // 新鲜值奖励设置按钮
        MutableComponent bonusBtnVar = Component.literal("[新鲜值奖励设置]");
        bonusBtnVar.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood bonus_menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置新鲜值奖励效果"))));
        buttons.append(bonusBtnVar).append(Component.literal("  "));
        
        // 食物特性设置按钮
        MutableComponent attrBtn = Component.literal("[食物特性设置]");
        attrBtn.withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood attr_menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置食物分类、特点、熟度"))));
        buttons.append(attrBtn);
        
        source.sendSuccess(() -> buttons, false);
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 操作按钮 ---
        MutableComponent actions = Component.literal(" ");
        
        // 返回主菜单按钮
        MutableComponent backBtn = Component.literal("[返回上级菜单]");
        backBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood help"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回主控制台"))));
        actions.append(backBtn).append(Component.literal("  "));
        
        // 初始化该食物设置按钮
        MutableComponent initBtn = Component.literal("[初始化该食物设置]");
        initBtn.withStyle(s -> s.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood init_all_settings"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("初始化当前食物的所有设置(保质期、奖励、特性)"))));
        actions.append(initBtn);
        
        source.sendSuccess(() -> actions, false);

        // --- 尾部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int showSetTimeMenu(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }

        Item item = stack.getItem();

        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物，无法设置保质期。"));
            return 0;
        }

        String registryName = ForgeRegistries.ITEMS.getKey(item).toString();
        long currentTicks = FoodConfig.getItemLifetime(item);
        String currentDuration = getDurationText(item);

        // --- 头部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r保质期设置"), false);
        source.sendSuccess(() -> Component.empty(), false); // 空行

        // --- 信息显示 ---
        MutableComponent infoLine = Component.literal(" §7目标: ");
        infoLine.append(Component.translatable(item.getDescriptionId())
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(registryName)))));
        source.sendSuccess(() -> infoLine, false);

        source.sendSuccess(() -> Component.literal(" §7当前保质期: §f" + currentDuration), false);
        source.sendSuccess(() -> Component.empty(), false);

        // --- 预设按钮 ---
        source.sendSuccess(() -> Component.literal(" §7预设时间:"), false);
        MutableComponent presets = Component.literal(" ");
        presets.append(makeSetBtn("50分钟", registryName, 50.0f, ChatFormatting.RED));
        presets.append(makeSetBtn("100分钟", registryName, 100.0f, ChatFormatting.GOLD));
        presets.append(makeSetBtn("240分钟", registryName, 240.0f, ChatFormatting.YELLOW));
        presets.append(makeSetBtn("1000分钟", registryName, 1000.0f, ChatFormatting.GREEN));
        presets.append(makeSetBtn("永久", registryName, -1.0f, ChatFormatting.AQUA));
        source.sendSuccess(() -> presets, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 自定义设置 ---
        source.sendSuccess(() -> Component.literal(" §7自定义设置:"), false);
        MutableComponent custom = Component.literal(" ");
        MutableComponent customBtn = Component.literal("[自定义分钟数]");
        customBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set " + registryName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入分钟数"))));
        custom.append(customBtn);
        source.sendSuccess(() -> custom, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 操作按钮 ---
        MutableComponent actions = Component.literal(" ");
        
        // 重置按钮
        MutableComponent resetBtn = Component.literal("[重置保质期]");
        resetBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood reset " + registryName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("重置为默认保质期"))));
        actions.append(resetBtn);
        
        source.sendSuccess(() -> actions, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 返回按钮 ---
        MutableComponent backButton = Component.literal(" ");
        MutableComponent backBtn = Component.literal("[返回上级菜单]");
        backBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回食物配置面板"))));
        backButton.append(backBtn);
        
        // 添加返回主菜单按钮
        MutableComponent mainMenuBtn = Component.literal("  [返回主控制台]");
        mainMenuBtn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood help"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回主控制台"))));
        backButton.append(mainMenuBtn);
        
        source.sendSuccess(() -> backButton, false);

        // --- 尾部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int showAttrMenu(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }

        Item item = stack.getItem();
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物，无法设置属性。"));
            return 0;
        }

        String registryName = ForgeRegistries.ITEMS.getKey(item).toString();
        Set<String> currentTags = FoodConfig.getFoodTags(stack);
        
        // 解析当前属性
        String currentClassification = null;
        Set<String> currentFeatures = new HashSet<>();
        String currentNature = null;
        
        for (String tag : currentTags) {
            if (tag.startsWith("分类:")) {
                currentClassification = tag.substring(3);
            } else if (tag.startsWith("特点:")) {
                currentFeatures.add(tag.substring(3));
            } else if (tag.startsWith("熟度:")) {
                currentNature = tag.substring(3);
            }
        }

        // --- 头部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r食物属性配置面板"), false);
        source.sendSuccess(() -> Component.empty(), false); // 空行

        // --- 信息显示 (悬停显示ID) ---
        MutableComponent infoLine = Component.literal(" §7目标: ");
        infoLine.append(Component.translatable(item.getDescriptionId())
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(registryName)))));

        source.sendSuccess(() -> infoLine, false);
        source.sendSuccess(() -> Component.empty(), false);
        
        // --- 当前属性显示 ---
        if (currentClassification != null) {
            final String finalClassification = currentClassification;
            source.sendSuccess(() -> Component.literal(" §7当前分类: §f" + finalClassification), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前分类: §c未设置"), false);
        }
        
        if (!currentFeatures.isEmpty()) {
            StringBuilder featuresStr = new StringBuilder();
            for (String feature : currentFeatures) {
                if (featuresStr.length() > 0) {
                    featuresStr.append(", ");
                }
                featuresStr.append(feature);
            }
            final String finalFeatures = featuresStr.toString();
            source.sendSuccess(() -> Component.literal(" §7当前特点: §f" + finalFeatures), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前特点: §c未设置"), false);
        }
        
        if (currentNature != null) {
            final String finalNature = currentNature;
            source.sendSuccess(() -> Component.literal(" §7当前熟度: §f" + finalNature), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前熟度: §c未设置"), false);
        }
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 分类设置 ---
        source.sendSuccess(() -> Component.literal(" §7设置分类:"), false);
        MutableComponent classifications = Component.literal(" ");
        classifications.append(makeClassificationBtn("蔬菜", registryName, "蔬菜", currentClassification, ChatFormatting.GREEN));
        classifications.append(makeClassificationBtn("肉类", registryName, "肉类", currentClassification, ChatFormatting.RED));
        classifications.append(makeClassificationBtn("饮品", registryName, "饮品", currentClassification, ChatFormatting.LIGHT_PURPLE));
        classifications.append(makeClassificationBtn("鱼类", registryName, "鱼类", currentClassification, ChatFormatting.AQUA));
        classifications.append(makeClassificationBtn("水果", registryName, "水果", currentClassification, ChatFormatting.YELLOW));
        classifications.append(makeClassificationBtn("谷物", registryName, "谷物", currentClassification, ChatFormatting.GOLD));
        classifications.append(makeClassificationBtn("汤食", registryName, "汤食", currentClassification, ChatFormatting.BLUE));
        source.sendSuccess(() -> classifications, false);
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 特点设置 ---
        source.sendSuccess(() -> Component.literal(" §7设置特点:"), false);
        MutableComponent features = Component.literal(" ");
        features.append(makeFeatureBtn("猪肉", registryName, "猪肉", currentFeatures, ChatFormatting.RED));
        features.append(makeFeatureBtn("牛肉", registryName, "牛肉", currentFeatures, ChatFormatting.DARK_RED));
        features.append(makeFeatureBtn("鸡肉", registryName, "鸡肉", currentFeatures, ChatFormatting.GOLD));
        features.append(makeFeatureBtn("羊肉", registryName, "羊肉", currentFeatures, ChatFormatting.AQUA));
        features.append(makeFeatureBtn("怪物肉", registryName, "怪物肉", currentFeatures, ChatFormatting.DARK_PURPLE));
        features.append(makeFeatureBtn("兔肉", registryName, "兔肉", currentFeatures, ChatFormatting.BLUE));
        features.append(makeFeatureBtn("黄金", registryName, "黄金", currentFeatures, ChatFormatting.GOLD));
        features.append(makeFeatureBtn("毒素", registryName, "毒素", currentFeatures, ChatFormatting.GRAY));
        features.append(makeFeatureBtn("附魔", registryName, "附魔", currentFeatures, ChatFormatting.DARK_BLUE));
        features.append(makeFeatureBtn("酒类", registryName, "酒类", currentFeatures, ChatFormatting.LIGHT_PURPLE));
        features.append(makeFeatureBtn("甜食", registryName, "甜食", currentFeatures, ChatFormatting.GREEN));
        source.sendSuccess(() -> features, false);
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 熟度设置 ---
        source.sendSuccess(() -> Component.literal(" §7设置熟度:"), false);
        MutableComponent natures = Component.literal(" ");
        natures.append(makeNatureBtn("0%", registryName, "0%", currentNature, ChatFormatting.RED));
        natures.append(makeNatureBtn("70%", registryName, "70%", currentNature, ChatFormatting.GREEN));
        source.sendSuccess(() -> natures, false);
        
        source.sendSuccess(() -> Component.empty(), false);

        // --- 操作按钮 ---
        MutableComponent actions = Component.literal(" ");
        
        // 自定义分类按钮
        MutableComponent customClassBtn = Component.literal("[自定义分类]");
        customClassBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_classification "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入分类名称"))));
        actions.append(customClassBtn).append(Component.literal("  "));
        
        // 自定义性质按钮
        MutableComponent customNatureBtn = Component.literal("[自定义性质]");
        customNatureBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_nature "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入熟度名称"))));
        actions.append(customNatureBtn).append(Component.literal("  "));
        
        // 自定义特点按钮
        MutableComponent customFeatureBtn = Component.literal("[添加特点]");
        customFeatureBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood toggle_feature "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入特点名称"))));
        actions.append(customFeatureBtn).append(Component.literal("  "));
        
        // 重置按钮
        MutableComponent resetBtn = Component.literal("[重置属性]");
        resetBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood reset_all_attrs"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("删除此物品的所有属性自定义设置"))));
        actions.append(resetBtn);
        
        source.sendSuccess(() -> actions, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 返回按钮 ---
        MutableComponent backButton = Component.literal(" ");
        MutableComponent backBtn = Component.literal("[返回上级菜单]");
        backBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回物品配置面板"))));
        backButton.append(backBtn);
        
        // 添加返回主菜单按钮
        MutableComponent mainMenuBtn = Component.literal("  [返回主控制台]");
        mainMenuBtn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood help"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回主控制台"))));
        backButton.append(mainMenuBtn);
        
        source.sendSuccess(() -> backButton, false);

        // --- 尾部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int showBonusMenu(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }

        Item item = stack.getItem();

        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物，无法设置奖励。"));
            return 0;
        }

        String registryName = ForgeRegistries.ITEMS.getKey(item).toString();

        // --- 头部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r奖励配置面板"), false);
        source.sendSuccess(() -> Component.empty(), false); // 空行

        // --- 信息显示 (悬停显示ID) ---
        MutableComponent infoLine = Component.literal(" §7目标: ");
        infoLine.append(Component.translatable(item.getDescriptionId())
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(registryName)))));

        source.sendSuccess(() -> infoLine, false);
        source.sendSuccess(() -> Component.empty(), false);
        
        // --- 显示当前奖励效果 ---
        List<FoodConfig.EffectBonus> currentBonuses = FoodConfig.getBonusEffects(stack);
        if (!currentBonuses.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" §7当前奖励效果:"), false);
            for (FoodConfig.EffectBonus bonus : currentBonuses) {
                String effectName = getEffectChineseName(getEffectName(bonus.effect));
                final String displayText = "  §f- " + effectName + " (等级:" + (bonus.amplifier + 1) + ", 概率:" + String.format("%.0f%%", bonus.chance * 100) + ", 时长:" + bonus.durationSeconds + "秒)";
                source.sendSuccess(() -> Component.literal(displayText), false);
            }
            source.sendSuccess(() -> Component.empty(), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前奖励效果: §c无"), false);
            source.sendSuccess(() -> Component.empty(), false);
        }

        // --- 效果按钮 (一行显示) ---
        source.sendSuccess(() -> Component.literal(" §7效果类型:"), false);

        MutableComponent effects = Component.literal(" ");
        effects.append(makeToggleBonusBtn("饱和", registryName, "saturation", ChatFormatting.GREEN));
        effects.append(makeToggleBonusBtn("生命恢复", registryName, "regeneration", ChatFormatting.RED));
        effects.append(makeToggleBonusBtn("伤害吸收", registryName, "absorption", ChatFormatting.YELLOW));
        effects.append(makeToggleBonusBtn("防火", registryName, "fire_resistance", ChatFormatting.GOLD));
        effects.append(makeToggleBonusBtn("水下呼吸", registryName, "water_breathing", ChatFormatting.BLUE));
        effects.append(makeToggleBonusBtn("幸运", registryName, "luck", ChatFormatting.LIGHT_PURPLE));
        source.sendSuccess(() -> effects, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 帮助信息 ---
        source.sendSuccess(() -> Component.literal(" §7使用方法: 点击效果按钮进入详细设置"), false);
        source.sendSuccess(() -> Component.literal(" §7详细设置包括: 添加/移除效果、设置等级、设置时长"), false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 初始化按钮 ---
        MutableComponent initButton = Component.literal(" ");
        MutableComponent initBtn = Component.literal("[初始化奖励]");
        initBtn.withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood init_bonus"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("初始化当前食物的奖励效果"))));
        initButton.append(initBtn);
        source.sendSuccess(() -> initButton, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 返回按钮 ---
        MutableComponent backButton = Component.literal(" ");
        MutableComponent backBtn = Component.literal("[返回上级菜单]");
        backBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回物品配置面板"))));
        backButton.append(backBtn);
        
        // 添加返回主菜单按钮
        MutableComponent mainMenuBtn = Component.literal("  [返回主控制台]");
        mainMenuBtn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood help"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回主控制台"))));
        backButton.append(mainMenuBtn);
        
        source.sendSuccess(() -> backButton, false);

        // --- 尾部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int showBonusDetailMenu(CommandSourceStack source, String effect) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }

        Item item = stack.getItem();
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物"));
            return 0;
        }

        String registryName = ForgeRegistries.ITEMS.getKey(item).toString();
        String effectChineseName = getEffectChineseName(effect);

        // 获取现有奖励效果
        List<FoodConfig.EffectBonus> currentBonuses = FoodConfig.getBonusEffects(stack);
        
        // 查找当前效果的设置
        FoodConfig.EffectBonus currentBonus = null;
        for (FoodConfig.EffectBonus bonus : currentBonuses) {
            String effectName = getEffectName(bonus.effect);
            if (effectName.equals(effect)) {
                currentBonus = bonus;
                break;
            }
        }

        // --- 头部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r" + effectChineseName + " 设置"), false);
        source.sendSuccess(() -> Component.empty(), false); // 空行

        // --- 信息显示 ---
        MutableComponent infoLine = Component.literal(" §7目标: ");
        infoLine.append(Component.translatable(item.getDescriptionId())
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)));
        source.sendSuccess(() -> infoLine, false);
        
        // 显示当前食物的属性
        Set<String> currentTags = FoodConfig.getFoodTags(stack);
        String currentClassification = null;
        Set<String> currentFeatures = new HashSet<>();
        String currentNature = null;
        
        for (String tag : currentTags) {
            if (tag.startsWith("分类:")) {
                currentClassification = tag.substring(3);
            } else if (tag.startsWith("特点:")) {
                currentFeatures.add(tag.substring(3));
            } else if (tag.startsWith("熟度:")) {
                currentNature = tag.substring(3);
            }
        }
        
        // 显示当前属性
        if (currentClassification != null || !currentFeatures.isEmpty() || currentNature != null) {
            source.sendSuccess(() -> Component.literal(" §7当前属性:").withStyle(ChatFormatting.GRAY), false);
            if (currentClassification != null) {
                final String finalClassification = currentClassification;
                source.sendSuccess(() -> Component.literal("  §7分类: §f" + finalClassification).withStyle(ChatFormatting.GRAY), false);
            }
            if (!currentFeatures.isEmpty()) {
                StringBuilder featuresStr = new StringBuilder();
                for (String feature : currentFeatures) {
                    if (featuresStr.length() > 0) {
                        featuresStr.append(", ");
                    }
                    featuresStr.append(feature);
                }
                final String finalFeaturesStr = featuresStr.toString();
                source.sendSuccess(() -> Component.literal("  §7特点: §f" + finalFeaturesStr).withStyle(ChatFormatting.GRAY), false);
            }
            if (currentNature != null) {
                final String finalNature = currentNature;
                source.sendSuccess(() -> Component.literal("  §7熟度: §f" + finalNature).withStyle(ChatFormatting.GRAY), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前属性: §c未设置").withStyle(ChatFormatting.GRAY), false);
        }

        if (currentBonus != null) {
            source.sendSuccess(() -> Component.literal(" §7当前设置:").withStyle(ChatFormatting.GRAY), false);
            final float chance = currentBonus.chance;
            final int durationSeconds = currentBonus.durationSeconds;
            final int amplifier = currentBonus.amplifier;
            source.sendSuccess(() -> Component.literal("  §7概率: §f" + String.format("%.0f%%", chance * 100)).withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("  §7时长: §f" + durationSeconds + "秒").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("  §7等级: §f" + (amplifier + 1)).withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.literal(" §7当前状态: §c未设置").withStyle(ChatFormatting.GRAY), false);
        }
        source.sendSuccess(() -> Component.empty(), false);

        // --- 操作按钮 ---
        source.sendSuccess(() -> Component.literal(" §7操作选项:"), false);

        MutableComponent actions = Component.literal(" ");
        
        if (currentBonus != null) {
            // 如果已设置，提供移除选项
            MutableComponent removeBtn = Component.literal("[取消效果]");
            removeBtn.withStyle(s -> s.withColor(ChatFormatting.RED)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood remove_bonus " + effect))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("移除该奖励效果"))));
            actions.append(removeBtn).append(" ");
        } else {
            // 如果未设置，提供添加选项
            MutableComponent addBtn = Component.literal("[添加效果]");
            addBtn.withStyle(s -> s.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood add_bonus " + effect))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("添加默认奖励效果"))));
            actions.append(addBtn).append(" ");
        }

        // 设置等级按钮
        MutableComponent levelBtn = Component.literal("[设置等级]");
        levelBtn.withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_bonus_level " + effect + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置效果等级 (0-2)"))));
        actions.append(levelBtn).append(" ");

        // 设置时长按钮
        MutableComponent durationBtn = Component.literal("[设置时长]");
        durationBtn.withStyle(s -> s.withColor(ChatFormatting.BLUE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_bonus_duration " + effect + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置效果时长 (秒)"))));
        actions.append(durationBtn).append(" ");
        
        // 设置概率按钮
        MutableComponent chanceBtn = Component.literal("[设置概率]");
        chanceBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_bonus_chance " + effect + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("设置效果概率 (0.0-1.0)"))));
        actions.append(chanceBtn).append(" ");
        
        // 刷新按钮
        MutableComponent refreshBtn = Component.literal("[刷新]");
        refreshBtn.withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood refresh_bonus_detail " + effect))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("刷新当前页面"))));
        actions.append(refreshBtn).append(" ");

        source.sendSuccess(() -> actions, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 返回按钮 ---
        MutableComponent backButton = Component.literal(" ");
        MutableComponent backBtn = Component.literal("[返回奖励菜单]");
        backBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood bonus_menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回奖励配置面板"))));
        backButton.append(backBtn);
        
        // 添加返回主菜单按钮
        MutableComponent mainMenuBtn = Component.literal("  [返回主控制台]");
        mainMenuBtn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood help"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("返回主控制台"))));
        backButton.append(mainMenuBtn);
        
        source.sendSuccess(() -> backButton, false);

        // --- 尾部 ---
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static String getDurationText(Item item) {
        long ticks = FoodConfig.getItemLifetime(new ItemStack(item));
        if (ticks == -1) return "§6永久";
        float mins = ticks / 1200f;
        return String.format("%.1f 分钟", mins); // 保留一位小数
    }

    private static MutableComponent makeSetBtn(String label, String itemId, float minutes, ChatFormatting color) {
        String cmd = "/betterfood set " + itemId + " " + minutes;
        MutableComponent btn = Component.literal("[" + label + "]");
        btn.withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击应用: " + (minutes == -1 ? "永久" : minutes + "分钟")))));
        return btn.append(" "); // 加个空格间隔
    }

    private static MutableComponent makeBonusBtn(String label, String itemId, String effect, ChatFormatting color) {
        String cmd = "/betterfood set_bonus " + itemId + " " + effect + " ";
        MutableComponent btn = Component.literal("[" + label + "]");
        btn.withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入: 概率 时间 等级"))));
        return btn.append(" "); // 加个空格间隔
    }

    private static MutableComponent makeToggleBonusBtn(String label, String itemId, String effect, ChatFormatting color) {
        String cmd = "/betterfood bonus_detail_menu " + effect;
        MutableComponent btn = Component.literal("[" + label + "]");
        btn.withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击设置: " + label))));
        return btn.append(" "); // 加个空格间隔
    }

    private static net.minecraft.world.effect.MobEffect parseEffect(String effectStr) {
        switch (effectStr.toLowerCase()) {
            case "saturation": return net.minecraft.world.effect.MobEffects.SATURATION;
            case "regeneration": return net.minecraft.world.effect.MobEffects.REGENERATION;
            case "absorption": return net.minecraft.world.effect.MobEffects.ABSORPTION;
            case "fire_resistance": return net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE;
            case "water_breathing": return net.minecraft.world.effect.MobEffects.WATER_BREATHING;
            case "luck": return net.minecraft.world.effect.MobEffects.LUCK;
            default: return null;
        }
    }

    private static String getEffectChineseName(String effectStr) {
        switch (effectStr.toLowerCase()) {
            case "saturation": return "饱和";
            case "regeneration": return "生命恢复";
            case "absorption": return "伤害吸收";
            case "fire_resistance": return "防火";
            case "water_breathing": return "水下呼吸";
            case "luck": return "幸运";
            default: return effectStr;
        }
    }

    private static String getEffectName(net.minecraft.world.effect.MobEffect effect) {
        if (effect == net.minecraft.world.effect.MobEffects.SATURATION) return "saturation";
        if (effect == net.minecraft.world.effect.MobEffects.REGENERATION) return "regeneration";
        if (effect == net.minecraft.world.effect.MobEffects.ABSORPTION) return "absorption";
        if (effect == net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) return "fire_resistance";
        if (effect == net.minecraft.world.effect.MobEffects.WATER_BREATHING) return "water_breathing";
        if (effect == net.minecraft.world.effect.MobEffects.LUCK) return "luck";
        return "";
    }

    // === 帮助菜单 ===
    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §2Better Food 控制台"), false);
        source.sendSuccess(() -> Component.empty(), false);

        // 食物设置按钮
        MutableComponent foodSettingBtn = Component.literal(" §b[食物设置]");
        foodSettingBtn.withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood menu"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("打开当前食物的配置面板"))));
        source.sendSuccess(() -> foodSettingBtn, false);

        sendClickable(source, " §e[重载配置文件]", "/betterfood reload", "重新读取所有配置");
        sendClickable(source, " §c[初始化所有设置]", "/betterfood reset_all", "警告：删除所有自定义文件！");

        source.sendSuccess(() -> Component.empty(), false);

        MutableComponent decayLine = Component.literal(" §7腐烂开关: ");
        decayLine.append(makeBtn("[开启]", "/betterfood decay true", ChatFormatting.GREEN));
        decayLine.append(makeBtn(" [关闭]", "/betterfood decay false", ChatFormatting.RED));
        source.sendSuccess(() -> decayLine, false);

        MutableComponent pauseLine = Component.literal(" §7计时状态: ");
        pauseLine.append(makeBtn("[继续]", "/betterfood pause false", ChatFormatting.GREEN));
        pauseLine.append(makeBtn(" [暂停]", "/betterfood pause true", ChatFormatting.GOLD));
        source.sendSuccess(() -> pauseLine, false);

        source.sendSuccess(() -> Component.empty(), false);
        
        // 添加自定义熟度设置按钮
        MutableComponent customNatureLine = Component.literal(" §7熟度设置: ");
        MutableComponent customNatureBtn = Component.literal("[自定义熟度]");
        customNatureBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set_nature "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后输入自定义熟度数值(如: 50%)"))));
        customNatureLine.append(customNatureBtn);
        source.sendSuccess(() -> customNatureLine, false);

        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static void sendClickable(CommandSourceStack source, String label, String cmd, String tooltip) {
        MutableComponent text = Component.literal(label);
        text.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tooltip))));
        source.sendSuccess(() -> text, false);
    }

    private static MutableComponent makeBtn(String label, String cmd, ChatFormatting color) {
        return Component.literal(label).withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)));
    }

    private static int reloadConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6[BetterFood]§r 正在重载配置文件..."), true);
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds());
        return 1;
    }
    
    // === 食物属性相关方法 ===
    
    /**
     * 创建分类按钮
     */
    private static MutableComponent makeClassificationBtn(String label, String itemId, String classification, String currentClassification, ChatFormatting color) {
        MutableComponent btn;
        if (classification.equals(currentClassification)) {
            // 当前选中的分类，显示为亮色
            btn = Component.literal("[" + label + "✓]");
            btn.withStyle(s -> s.withColor(ChatFormatting.WHITE).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood set_classification \"" + classification + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击取消设置分类: " + classification))));
        } else {
            // 未选中的分类，显示为暗色
            btn = Component.literal("[" + label + "]");
            btn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood set_classification \"" + classification + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击设置分类: " + classification))));
        }
        return btn.append(" "); // 加个空格间隔
    }
    
    /**
     * 创建特点按钮
     */
    private static MutableComponent makeFeatureBtn(String label, String itemId, String feature, Set<String> currentFeatures, ChatFormatting color) {
        MutableComponent btn;
        if (currentFeatures.contains(feature)) {
            // 当前选中的特点，显示为亮色
            btn = Component.literal("[" + label + "✓]");
            btn.withStyle(s -> s.withColor(ChatFormatting.WHITE).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood toggle_feature \"" + feature + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击取消特点: " + feature))));
        } else {
            // 未选中的特点，显示为暗色
            btn = Component.literal("[" + label + "]");
            btn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood toggle_feature \"" + feature + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击添加特点: " + feature))));
        }
        return btn.append(" "); // 加个空格间隔
    }
    
    /**
     * 创建性质按钮
     */
    private static MutableComponent makeNatureBtn(String label, String itemId, String nature, String currentNature, ChatFormatting color) {
        MutableComponent btn;
        if (nature.equals(currentNature)) {
            // 当前选中的性质，显示为亮色
            btn = Component.literal("[" + label + "✓]");
            btn.withStyle(s -> s.withColor(ChatFormatting.WHITE).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood set_nature \"" + nature + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击取消设置熟度: " + nature))));
        } else {
            // 未选中的性质，显示为暗色
            btn = Component.literal("[" + label + "]");
            btn.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood set_nature \"" + nature + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击设置熟度: " + nature))));
        }
        return btn.append(" "); // 加个空格间隔
    }
    
    /**
     * 切换特点
     */
    private static int toggleFeature(CommandSourceStack source, String feature) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
        
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }
        
        Item item = stack.getItem();
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物"));
            return 0;
        }
        
        // 获取当前标签
        Set<String> currentTags = new HashSet<>(FoodConfig.getFoodTags(stack));
        
        // 构造特点标签
        String featureTag = "特点:" + feature;
        
        // 切换特点
        if (currentTags.contains(featureTag)) {
            currentTags.remove(featureTag);
            source.sendSuccess(() -> Component.literal("§e[属性] §f已移除特点: " + feature), true);
        } else {
            currentTags.add(featureTag);
            source.sendSuccess(() -> Component.literal("§a[属性] §f已添加特点: " + feature), true);
        }
        
        // 保存到用户配置
        UserConfigManager.saveTagsOverride(item, currentTags);
        
        // 注册到 FoodConfig
        FoodConfig.registerTags(item, currentTags);
        
        // 重新显示属性菜单
        return showAttrMenu(source);
    }
    
    /**
     * 设置分类
     */
    private static int setClassification(CommandSourceStack source, String classification) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
        
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }
        
        Item item = stack.getItem();
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物"));
            return 0;
        }
        
        // 获取当前标签
        Set<String> currentTags = new HashSet<>(FoodConfig.getFoodTags(stack));
        
        // 移除现有的分类标签
        currentTags.removeIf(tag -> tag.startsWith("分类:"));
        
        // 添加新的分类标签
        String classificationTag = "分类:" + classification;
        currentTags.add(classificationTag);
        
        // 保存到用户配置
        UserConfigManager.saveTagsOverride(item, currentTags);
        
        // 注册到 FoodConfig
        FoodConfig.registerTags(item, currentTags);
        
        source.sendSuccess(() -> Component.literal("§a[属性] §f已设置分类: " + classification), true);
        
        // 重新显示属性菜单
        return showAttrMenu(source);
    }
    
    /**
     * 设置性质
     */
    private static int setNature(CommandSourceStack source, String nature) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
        
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先将物品拿在手上！"));
            return 0;
        }
        
        Item item = stack.getItem();
        if (!item.isEdible()) {
            source.sendFailure(Component.literal("错误：当前手持的不是食物"));
            return 0;
        }
        
        // 获取当前标签
        Set<String> currentTags = new HashSet<>(FoodConfig.getFoodTags(stack));
        
        // 移除现有的熟度标签
        currentTags.removeIf(tag -> tag.startsWith("熟度:"));
        
        // 添加新的熟度标签
        String natureTag = "熟度:" + nature;
        currentTags.add(natureTag);
        
        // 保存到用户配置
        UserConfigManager.saveTagsOverride(item, currentTags);
        
        // 注册到 FoodConfig
        FoodConfig.registerTags(item, currentTags);
        
        source.sendSuccess(() -> Component.literal("§a[属性] §f已设置熟度: " + nature), true);
        
        // 重新显示属性菜单
        return showAttrMenu(source);
    }
}