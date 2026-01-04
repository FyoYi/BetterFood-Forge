package com.fyoyi.betterfood.client.renderer.cutting_board;

import com.fyoyi.betterfood.block.cutting_board.CuttingBoardBlock;
import com.fyoyi.betterfood.block.entity.CuttingBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CuttingBoardBlockRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity> {

    public CuttingBoardBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CuttingBoardBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        ItemStack stack = pBlockEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        pPoseStack.pushPose();

        // 将渲染坐标系移动到方块中心
        pPoseStack.translate(0.5, 1.25 / 16.0, 0.5);

        // 根据菜板朝向旋转物品
        Direction facing = pBlockEntity.getBlockState().getValue(CuttingBoardBlock.FACING);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // 将物品平躺
        pPoseStack.mulPose(Axis.XP.rotationDegrees(270f));

        // 调整物品大小
        pPoseStack.scale(0.6f, 0.6f, 0.6f);

        // 渲染物品
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, pPackedLight, pPackedOverlay, pPoseStack, pBufferSource, pBlockEntity.getLevel(), 0);

        pPoseStack.popPose();
    }
}