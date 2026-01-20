package com.fyoyi.betterfood.event;

import com.fyoyi.betterfood.recipe.RecipeBook;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 玩家菜谱书能力处理 - 绑定菜谱书到玩家身上
 */
public class PlayerRecipeBookCapability {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<IRecipeBookData> RECIPE_BOOK_CAP = net.minecraftforge.common.capabilities.CapabilityManager
            .get(
                    new net.minecraftforge.common.capabilities.CapabilityToken<IRecipeBookData>() {
                    });

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Player> event) {
        LOGGER.info("附加菜谱书能力到玩家: {}", event.getObject().getName().getString());
        event.addCapability(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("better_food", "recipe_book"),
                new RecipeBookProvider());
    }

    /**
     * 菜谱书数据接口
     */
    public interface IRecipeBookData {
        RecipeBook getRecipeBook();

        void setRecipeBook(RecipeBook book);

        CompoundTag serializeNBT();

        void deserializeNBT(CompoundTag nbt);
    }

    /**
     * 菜谱书数据实现
     */
    public static class RecipeBookData implements IRecipeBookData {
        private RecipeBook recipeBook = new RecipeBook();

        @Override
        public RecipeBook getRecipeBook() {
            return recipeBook;
        }

        @Override
        public void setRecipeBook(RecipeBook book) {
            this.recipeBook = book;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("RecipeBook", recipeBook.serializeNBT());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains("RecipeBook")) {
                recipeBook = RecipeBook.deserializeNBT(nbt.getCompound("RecipeBook"));
            }
        }
    }

    /**
     * 菜谱书能力提供者
     */
    public static class RecipeBookProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final RecipeBookData data = new RecipeBookData();
        private final LazyOptional<IRecipeBookData> lazyOptional = LazyOptional.of(() -> data);

        @Override
        @NotNull
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, net.minecraft.core.Direction side) {
            if (cap == RECIPE_BOOK_CAP) {
                return lazyOptional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }
    }

    /**
     * 能力事件处理包装器 - 用于手动注册到事件总线
     */
    public static class CapabilityEventHandler {
        @SubscribeEvent
        public void onAttachCapabilities(AttachCapabilitiesEvent<Player> event) {
            LOGGER.info("附加菜谱书能力到玩家: {}", event.getObject().getName().getString());
            event.addCapability(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("better_food", "recipe_book"),
                    new RecipeBookProvider());
        }
    }
}
