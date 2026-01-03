package com.fyoyi.betterfood.block;

import com.fyoyi.betterfood.better_food;
import com.fyoyi.betterfood.block.cooking_pan.SimpleFoodBlock;
import com.fyoyi.betterfood.block.large_pot.LargePotBlock;
import com.fyoyi.betterfood.block.lid.LidBlock;
import com.fyoyi.betterfood.block.cutting_board.CuttingBoardBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.fyoyi.betterfood.block.bowl.PlacedBowlBlock;
import com.fyoyi.betterfood.block.bowl.OilBowlBlock;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, better_food.MOD_ID);

    public static final RegistryObject<Block> COOKING_PAN =
            BLOCKS.register("cooking_pan", () -> new SimpleFoodBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_ORANGE)
                            .strength(2.0F, 2.0F)
                            .sound(SoundType.METAL)
            ));

    public static final RegistryObject<Block> LARGE_POT =
            BLOCKS.register("large_pot", () -> new LargePotBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_ORANGE)
                            .strength(2.0F, 2.0F)
                            .sound(SoundType.METAL)
            ));

    public static final RegistryObject<Block> LID =
            BLOCKS.register("lid", () -> new LidBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_ORANGE)
                            .strength(2.0F, 2.0F)
                            .sound(SoundType.METAL)
            ));

    public static final RegistryObject<Block> CUTTING_BOARD =
            BLOCKS.register("cutting_board", () -> new CuttingBoardBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD)
                            .strength(1.5F, 1.5F) // 硬度和抗爆性
                            .noOcclusion() // 防止被视为完整方块导致周围变黑
            ));

    public static final RegistryObject<Block> PLACED_BOWL =
            BLOCKS.register("placed_bowl", () -> new PlacedBowlBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                            .noOcclusion() // 防止透视
                            .strength(1.0f) // 硬度
            ));

    public static final RegistryObject<Block> OIL_BOWL =
            BLOCKS.register("oil_bowl", () -> new OilBowlBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                            .noOcclusion().strength(1.0f)
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}