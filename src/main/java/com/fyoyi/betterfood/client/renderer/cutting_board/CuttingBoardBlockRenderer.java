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

import java.util.Random;

public class CuttingBoardBlockRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity> {

    private static final float ITEM_SCALE = 0.5f;
    private static final float Y_OFFSET = 1.15f / 16.0f;
    private static final float STACK_HEIGHT_INCREMENT = 0.035f;

    public CuttingBoardBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CuttingBoardBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        if (be.isEmpty())
            return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Direction facing = be.getBlockState().getValue(CuttingBoardBlock.FACING);

        float scaleY = be.getSquashScaleY(partialTick);
        float scaleXZ = be.getSquashScaleXZ(partialTick);

        int itemCount = 0;
        for (int i = 0; i < be.getContainerSize(); i++)
            if (!be.getItem(i).isEmpty())
                itemCount++;

        int renderIndex = 0;
        for (int i = 0; i < be.getContainerSize(); i++) {
            ItemStack stack = be.getItem(i);
            if (stack.isEmpty())
                continue;

            poseStack.pushPose();

            poseStack.translate(0.5, Y_OFFSET, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

            if (itemCount == 1) {
                // === 原料阶段 (单体，应用压扁动画) ===
                long seed = be.getRandomSeed(i);
                if (seed == 0) {
                    seed = be.getBlockPos().asLong() + i * 777L;
                }
                Random random = new Random(seed);

                // 原料位置随机较小，尽量靠近中心
                float offsetX = (random.nextFloat() - 0.5f) * 0.2f; // ±0.1
                float offsetZ = (random.nextFloat() - 0.5f) * 0.2f; // ±0.1
                float rotY = (random.nextFloat() - 0.5f) * 60.0f;

                // 保存原料位置给后续产物使用
                be.setRawIngredientPosition(offsetX, offsetZ);

                poseStack.translate(offsetX, 0, offsetZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(rotY + 180f));
                poseStack.mulPose(Axis.XP.rotationDegrees(90f));

                // 应用压扁
                poseStack.scale(ITEM_SCALE * scaleXZ, ITEM_SCALE * scaleXZ, ITEM_SCALE * scaleY);

            } else {
                // === 产物堆叠阶段 (多个) ===
                // 获取原料的位置作为基准
                float baseOffsetX = be.getRawIngredientOffsetX();
                float baseOffsetZ = be.getRawIngredientOffsetZ();

                long seed = be.getRandomSeed(i);
                if (seed == 0) {
                    seed = be.getBlockPos().asLong() + i * 777L;
                }
                Random random = new Random(seed);

                // 以原料位置为中心，产物在周围随机分散
                float offsetX = baseOffsetX + (random.nextFloat() - 0.5f) * 0.4f;
                float offsetZ = baseOffsetZ + (random.nextFloat() - 0.5f) * 0.3f;
                float rotY = (random.nextFloat() - 0.5f) * 60.0f;

                float stackY = renderIndex * STACK_HEIGHT_INCREMENT;

                poseStack.translate(offsetX, stackY, offsetZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(rotY + 180f));
                poseStack.mulPose(Axis.XP.rotationDegrees(90f));
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            }

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer,
                    be.getLevel(), 0);

            poseStack.popPose();

            renderIndex++;
        }
    }
}