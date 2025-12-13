package com.fyoyi.betterfood.util;

import com.fyoyi.betterfood.config.FoodConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FoodExpiryManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public FoodExpiryManager() {
        super(GSON, "better_food_expiry");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        // 1. 清空旧数据
        FoodConfig.clear();
        FoodConfig.clearBonuses();
        System.out.println("[BetterFood] 开始加载数据包配置...");

        // 2. 加载数据包里的 JSON (默认配置)
        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            try {
                JsonObject jsonObject = entry.getValue().getAsJsonObject();
                long ticks = jsonObject.get("ticks").getAsLong();

                for (JsonElement itemElement : jsonObject.getAsJsonArray("items")) {
                    // 支持两种格式：
                    // 1. 字符串格式 (旧版): "minecraft:apple"
                    // 2. 对象格式 (新版): {"item": "minecraft:apple", "effects": [...]}
                    
                    if (itemElement.isJsonPrimitive()) {
                        // 旧格式：只有物品ID
                        String itemIdStr = itemElement.getAsString();
                        Item item = parseItemId(itemIdStr);
                        if (item != null) {
                            FoodConfig.register(item, ticks);
                        }
                    } else if (itemElement.isJsonObject()) {
                        // 新格式：包含物品ID和效果
                        JsonObject itemObj = itemElement.getAsJsonObject();
                        String itemIdStr = itemObj.get("item").getAsString();
                        Item item = parseItemId(itemIdStr);
                        
                        if (item != null) {
                            // 注册保质期
                            FoodConfig.register(item, ticks);
                            
                            // 如果有效果配置，也一起注册
                            if (itemObj.has("effects")) {
                                List<FoodConfig.EffectBonus> effects = new ArrayList<>();
                                for (JsonElement effectElement : itemObj.getAsJsonArray("effects")) {
                                    JsonObject effectObj = effectElement.getAsJsonObject();
                                    
                                    // 解析效果类型
                                    String effectType = effectObj.get("effect").getAsString();
                                    MobEffect mobEffect = parseEffect(effectType);
                                    if (mobEffect == null) {
                                        System.err.println("[BetterFood] 未知效果类型: " + effectType);
                                        continue;
                                    }
                                    
                                    // 解析参数
                                    float chance = effectObj.get("chance").getAsFloat();
                                    int duration = effectObj.get("duration").getAsInt();
                                    int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
                                    
                                    effects.add(new FoodConfig.EffectBonus(mobEffect, chance, duration, amplifier));
                                }
                                
                                if (!effects.isEmpty()) {
                                    FoodConfig.registerBonus(item, effects);
                                    System.out.println("[BetterFood] 已为 " + itemIdStr + " 注册 " + effects.size() + " 个新鲜奖励效果");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[BetterFood] 解析失败: " + entry.getKey());
                e.printStackTrace();
            }
        }

        // 3. 【核心新增】最后加载用户的本地覆盖配置 (优先级最高)
        UserConfigManager.loadOverrides();

        System.out.println("[BetterFood] 所有保质期配置加载完毕。");
    }

    /**
     * 解析物品ID
     */
    private Item parseItemId(String itemIdStr) {
        String namespace = "minecraft";
        String path = itemIdStr;
        if (itemIdStr.contains(":")) {
            String[] parts = itemIdStr.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        
        if (ForgeRegistries.ITEMS.containsKey(itemId)) {
            return ForgeRegistries.ITEMS.getValue(itemId);
        }
        return null;
    }

    /**
     * 将字符串转换为 MobEffect
     */
    private MobEffect parseEffect(String effectType) {
        return switch (effectType.toLowerCase()) {
            case "saturation", "饱和" -> MobEffects.SATURATION;
            case "regeneration", "生命恢复" -> MobEffects.REGENERATION;
            case "absorption", "伤害吸收" -> MobEffects.ABSORPTION;
            case "health_boost", "生命提升" -> MobEffects.HEALTH_BOOST;
            case "speed", "速度" -> MobEffects.MOVEMENT_SPEED;
            case "haste", "急迫" -> MobEffects.DIG_SPEED;
            case "strength", "力量" -> MobEffects.DAMAGE_BOOST;
            case "resistance", "抗性提升" -> MobEffects.DAMAGE_RESISTANCE;
            case "fire_resistance", "防火" -> MobEffects.FIRE_RESISTANCE;
            case "water_breathing", "水下呼吸" -> MobEffects.WATER_BREATHING;
            case "night_vision", "夜视" -> MobEffects.NIGHT_VISION;
            case "jump_boost", "跳跃提升" -> MobEffects.JUMP;
            case "luck", "幸运" -> MobEffects.LUCK;
            default -> null;
        };
    }
}