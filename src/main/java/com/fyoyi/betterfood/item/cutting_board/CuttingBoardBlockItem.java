package com.fyoyi.betterfood.item.cutting_board;

import com.fyoyi.betterfood.client.renderer.cutting_board.CuttingBoardItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class CuttingBoardBlockItem extends BlockItem {
    public CuttingBoardBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    // === 核心修改：动态堆叠控制 ===
    @Override
    public int getMaxStackSize(ItemStack stack) {
        // 检查是否有 BlockEntityTag (意味着里面存了数据)
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            // 检查是否有物品
            if (tag.contains("Items")) {
                NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(tag, items);
                for (ItemStack s : items) {
                    if (!s.isEmpty()) return 1; // 有物品则不可堆叠
                }
            }
            // 检查是否有切割进度
            if (tag.contains("CutProgress") && tag.getInt("CutProgress") > 0) {
                return 1; // 有进度则不可堆叠
            }
        }
        // 完全干净的空菜板，允许堆叠 64
        return 64;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new CuttingBoardItemRenderer();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);

            int count = 0;
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    tooltip.add(Component.literal("- " + s.getHoverName().getString()).withStyle(ChatFormatting.GRAY));
                    count++;
                }
            }

            if (tag.contains("CutProgress")) {
                int progress = tag.getInt("CutProgress");
                if (progress > 0) {
                    tooltip.add(Component.literal("切割进度: " + progress + "/3").withStyle(ChatFormatting.YELLOW));
                }
            }

            if (count == 0 && !tag.contains("CutProgress")) {
                tooltip.add(Component.literal("空").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}