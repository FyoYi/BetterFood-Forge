package com.fyoyi.betterfood.client.gui;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.util.CookednessHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class PotInfoOverlay implements IGuiOverlay {
    public static final PotInfoOverlay INSTANCE = new PotInfoOverlay();
    
    // 保留原有功能，用于显示锅内信息
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 检测视线（用于显示锅内信息）
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());

        if (be instanceof PotBlockEntity pot) {
            // 显示锅内信息
            renderPotInfo(guiGraphics, mc.font, pot, screenWidth, screenHeight);
        }
    }
    
    private void renderPotInfo(GuiGraphics graphics, Font font, PotBlockEntity pot, int width, int height) {
        NonNullList<ItemStack> items = pot.getItems();

        // 起始坐标：物品栏右侧，稍微偏上
        int startX = width / 2 + 100;
        int startY = height - 20;

        // 正序遍历 0 -> 3 (底层 -> 顶层)
        // 我们希望底层显示在最下面，顶层显示在最上面
        // 所以我们从 startY 开始画底层，然后 Y 递减往上画
        for (int i = 0; i < 4; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            float cooked = CookednessHelper.getCurrentCookedness(stack);

            // 颜色逻辑
            ChatFormatting color = cooked >= 100.0f ? ChatFormatting.RED : ChatFormatting.GREEN;

            // 文本: [1] 牛排 50%
            String text = String.format("[%d] %s %.0f%%", i + 1, stack.getHoverName().getString(), cooked);

            // 1. 渲染文字
            graphics.drawString(font, Component.literal(text).withStyle(color), startX, startY, 0xFFFFFF, true);

            // 2. 渲染物品图标 (在文字后面)
            // 计算文字宽度，以便图标紧跟在文字后面
            int textWidth = font.width(text);
            graphics.renderItem(stack, startX + textWidth + 4, startY - 4);

            // Y轴向上移动，准备画上一层
            startY -= 16;
        }
    }
}