package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.better_food;
import com.fyoyi.betterfood.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.fyoyi.betterfood.block.entity.CuttingBoardBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, better_food.MOD_ID);

    // 注册锅的方块实体
    public static final RegistryObject<BlockEntityType<PotBlockEntity>> POT_BE =
            BLOCK_ENTITIES.register("pot_be", () ->
                    BlockEntityType.Builder.of(PotBlockEntity::new,
                            ModBlocks.COOKING_PAN.get(),
                            ModBlocks.LARGE_POT.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD_BE =
            BLOCK_ENTITIES.register("cutting_board_be", () ->
                    BlockEntityType.Builder.of(CuttingBoardBlockEntity::new,
                            ModBlocks.CUTTING_BOARD.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}