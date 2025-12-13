package com.fyoyi.betterfood.util;

import com.fyoyi.betterfood.config.FoodConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("better_food");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("user_overrides.json").toFile();

    // 保存/更新
    public static void saveOverride(Item item, long ticks) {
        modifyJson(json -> {
            String key = ForgeRegistries.ITEMS.getKey(item).toString();
            json.addProperty(key, ticks);
        });
    }

    // 保存/更新奖励
    public static void saveBonusOverride(Item item, List<FoodConfig.EffectBonus> bonuses) {
        modifyJson(json -> {
            String key = ForgeRegistries.ITEMS.getKey(item).toString() + "_bonuses";
            JsonArray array = new JsonArray();
            for (FoodConfig.EffectBonus bonus : bonuses) {
                JsonObject obj = new JsonObject();
                obj.addProperty("effect", getEffectName(bonus.effect));
                obj.addProperty("chance", bonus.chance);
                obj.addProperty("duration", bonus.durationSeconds);
                obj.addProperty("amplifier", bonus.amplifier);
                array.add(obj);
            }
            json.add(key, array);
        });
    }

    // === 【新增】移除单个条目 ===
    public static void removeOverride(Item item) {
        modifyJson(json -> {
            String key = ForgeRegistries.ITEMS.getKey(item).toString();
            if (json.has(key)) {
                json.remove(key);
            }
        });
    }

    // === 【新增】重置所有 (删除文件) ===
    public static void clearAllOverrides() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.delete(CONFIG_FILE.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 内部辅助方法：读取 -> 修改 -> 写入
    private static void modifyJson(java.util.function.Consumer<JsonObject> modifier) {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            JsonObject json = new JsonObject();
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    JsonObject existing = GSON.fromJson(reader, JsonObject.class);
                    if (existing != null) json = existing;
                }
            }
            modifier.accept(json);
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载
    public static void loadOverrides() {
        if (!CONFIG_FILE.exists()) return;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;
            System.out.println("[BetterFood] 正在加载用户自定义配置...");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_bonuses")) {
                    // 处理奖励配置
                    String itemKey = key.substring(0, key.length() - 8); // 移除 "_bonuses"
                    ResourceLocation id = new ResourceLocation(itemKey);
                    if (ForgeRegistries.ITEMS.containsKey(id)) {
                        Item item = ForgeRegistries.ITEMS.getValue(id);
                        if (entry.getValue().isJsonArray()) {
                            JsonArray array = entry.getValue().getAsJsonArray();
                            List<FoodConfig.EffectBonus> bonuses = new ArrayList<>();
                            for (com.google.gson.JsonElement elem : array) {
                                if (elem.isJsonObject()) {
                                    JsonObject obj = elem.getAsJsonObject();
                                    String effectStr = obj.get("effect").getAsString();
                                    float chance = obj.get("chance").getAsFloat();
                                    int duration = obj.get("duration").getAsInt();
                                    int amplifier = obj.get("amplifier").getAsInt();
                                    MobEffect effect = parseEffect(effectStr);
                                    if (effect != null) {
                                        bonuses.add(new FoodConfig.EffectBonus(effect, chance, duration, amplifier));
                                    }
                                }
                            }
                            FoodConfig.registerBonus(item, bonuses);
                        }
                    }
                } else {
                    // 处理时间配置
                    ResourceLocation id = new ResourceLocation(key);
                    if (ForgeRegistries.ITEMS.containsKey(id)) {
                        Item item = ForgeRegistries.ITEMS.getValue(id);
                        FoodConfig.register(item, entry.getValue().getAsLong());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[BetterFood] 读取失败: " + e.getMessage());
        }
    }

    private static String getEffectName(MobEffect effect) {
        if (effect == MobEffects.SATURATION) return "saturation";
        if (effect == MobEffects.REGENERATION) return "regeneration";
        if (effect == MobEffects.ABSORPTION) return "absorption";
        if (effect == MobEffects.FIRE_RESISTANCE) return "fire_resistance";
        if (effect == MobEffects.WATER_BREATHING) return "water_breathing";
        if (effect == MobEffects.LUCK) return "luck";
        return "unknown";
    }

    private static MobEffect parseEffect(String effectStr) {
        switch (effectStr.toLowerCase()) {
            case "saturation": return MobEffects.SATURATION;
            case "regeneration": return MobEffects.REGENERATION;
            case "absorption": return MobEffects.ABSORPTION;
            case "fire_resistance": return MobEffects.FIRE_RESISTANCE;
            case "water_breathing": return MobEffects.WATER_BREATHING;
            case "luck": return MobEffects.LUCK;
            default: return null;
        }
    }
}