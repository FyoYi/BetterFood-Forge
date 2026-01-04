package com.fyoyi.betterfood.tags; // 确认包名是否正确

import com.fyoyi.betterfood.better_food;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Items {

        public static final TagKey<Item> CUTTABLE_FOODS = create("cuttable_foods");

        private static TagKey<Item> create(String name) {
            return ItemTags.create(new ResourceLocation(better_food.MOD_ID, name));
        }
    }

    public static class Blocks {

    }
}