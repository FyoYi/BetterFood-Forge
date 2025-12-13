package com.fyoyi.betterfood.command;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.TimeManager;
import com.fyoyi.betterfood.util.UserConfigManager;
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

@Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("betterfood")
                        .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource())))

                        // === 智能菜单 ===
                        .then(Commands.literal("menu").requires(s -> s.hasPermission(2)).executes(ctx -> showSetMenu(ctx.getSource())))

                        // === 奖励菜单 ===
                        .then(Commands.literal("bonus_menu").requires(s -> s.hasPermission(2)).executes(ctx -> showBonusMenu(ctx.getSource())))

                        // === 重载 ===
                        .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ctx -> reloadConfig(ctx.getSource())))

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
                                                    return 1;
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
                                                                            return 1;
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
                                            return 1;
                                        })
                                )
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
        source.sendSuccess(() -> Component.literal(" §6[Better Food] §r物品配置面板"), false);
        source.sendSuccess(() -> Component.empty(), false); // 空行

        // --- 信息显示 (悬停显示ID) ---
        MutableComponent infoLine = Component.literal(" §7目标: ");
        infoLine.append(Component.translatable(item.getDescriptionId())
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(registryName)))));

        infoLine.append("  §7当前: §f" + getDurationText(item));
        source.sendSuccess(() -> infoLine, false);
        source.sendSuccess(() -> Component.empty(), false);

        // --- 预设按钮 (一行显示) ---
        source.sendSuccess(() -> Component.literal(" §7快速预设:"), false);

        MutableComponent presets = Component.literal(" ");
        presets.append(makeSetBtn("极短", registryName, FoodConfig.MIN_MINUTES, ChatFormatting.RED));
        presets.append(makeSetBtn("短", registryName, FoodConfig.SHORT_MINUTES, ChatFormatting.YELLOW));
        presets.append(makeSetBtn("中", registryName, FoodConfig.MEDIUM_MINUTES, ChatFormatting.GREEN));
        presets.append(makeSetBtn("长", registryName, FoodConfig.LONG_MINUTES, ChatFormatting.BLUE));
        presets.append(makeSetBtn("无限", registryName, -1, ChatFormatting.GOLD));
        source.sendSuccess(() -> presets, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 底部操作栏 ---
        MutableComponent actions = Component.literal(" ");

        // 自定义按钮
        MutableComponent customBtn = Component.literal("[自定义输入]");
        customBtn.withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterfood set " + registryName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击后在输入框输入分钟数"))));

        // 重置按钮
        MutableComponent resetBtn = Component.literal("[重置默认]");
        resetBtn.withStyle(s -> s.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/betterfood reset " + registryName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("删除此物品的自定义设置"))));

        actions.append(customBtn).append(Component.literal("  ")).append(resetBtn);
        source.sendSuccess(() -> actions, false);

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

        // --- 效果按钮 (一行显示) ---
        source.sendSuccess(() -> Component.literal(" §7效果类型:"), false);

        MutableComponent effects = Component.literal(" ");
        effects.append(makeBonusBtn("饱和", registryName, "saturation", ChatFormatting.GREEN));
        effects.append(makeBonusBtn("生命恢复", registryName, "regeneration", ChatFormatting.RED));
        effects.append(makeBonusBtn("伤害吸收", registryName, "absorption", ChatFormatting.YELLOW));
        effects.append(makeBonusBtn("防火", registryName, "fire_resistance", ChatFormatting.GOLD));
        effects.append(makeBonusBtn("水下呼吸", registryName, "water_breathing", ChatFormatting.BLUE));
        effects.append(makeBonusBtn("幸运", registryName, "luck", ChatFormatting.LIGHT_PURPLE));
        source.sendSuccess(() -> effects, false);

        source.sendSuccess(() -> Component.empty(), false);

        // --- 帮助信息 ---
        source.sendSuccess(() -> Component.literal(" §7使用方法: 点击效果按钮后输入参数"), false);
        source.sendSuccess(() -> Component.literal(" §7格式: /betterfood set_bonus <物品> <效果> <概率> <时间> <等级>"), false);
        source.sendSuccess(() -> Component.literal(" §7示例: /betterfood set_bonus " + registryName + " saturation 0.8 10 0"), false);

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

    // === 帮助菜单 ===
    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§m---------------------------------------------").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(" §2Better Food 控制台"), false);

        sendClickable(source, " §b[设置手持物品]", "/betterfood menu", "打开当前食物的配置面板");
        sendClickable(source, " §d[设置奖励效果]", "/betterfood bonus_menu", "打开当前食物的奖励配置面板");
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
}