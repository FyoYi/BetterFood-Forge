package com.fyoyi.betterfood.item.cooking_pan;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.client.renderer.cooking_pan.PotItemRenderer;
import com.fyoyi.betterfood.util.CookednessHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.NonNullList;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PotBlockItem extends BlockItem {
    public PotBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new PotItemRenderer();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1. 调用父类，保证你的 better_food.java 里的事件能正常给锅本身加标签
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);

            boolean hasItem = false;
            for (ItemStack s : items)
                if (!s.isEmpty())
                    hasItem = true;

            if (hasItem) {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("=== 锅内食材 ===").withStyle(ChatFormatting.GOLD));

                // 2. 倒序遍历 (3->0)，顶层在上，底层在下
                for (int i = 3; i >= 0; i--) {
                    ItemStack foodStack = items.get(i);
                    if (!foodStack.isEmpty()) {
                        float cooked = CookednessHelper.getCurrentCookedness(foodStack);

                        // 3. 颜色逻辑：100%分界
                        ChatFormatting color = cooked >= 100.0f ? ChatFormatting.RED : ChatFormatting.GREEN;

                        // 层级名
                        String layerName = (i == 0) ? "[底层]" : (i == 3 ? "[顶层]" : "[层" + (i + 1) + "]");

                        MutableComponent line = Component.literal(layerName + " ").withStyle(ChatFormatting.GRAY);
                        // 食物名
                        line.append(foodStack.getHoverName().copy().withStyle(color));
                        // 熟度
                        line.append(Component.literal(String.format(" %.0f%%", cooked)).withStyle(color));

                        tooltip.add(line);
                    }
                }
            }
        }
    }
}