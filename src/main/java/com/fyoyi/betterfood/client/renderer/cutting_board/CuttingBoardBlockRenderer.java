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

    // [修改] 基础高度回退到 1.15 (贴近板面)
    private static final float Y_OFFSET = 1.15f / 16.0f;

    // [新增] 堆叠高度：每个物品往上摞大约 0.035 方块高 (约 0.6 像素)
    // 这样保证它们在视觉上是层叠的，不会Z-fighting闪烁
    private static final float STACK_HEIGHT_INCREMENT = 0.035f;

    public CuttingBoardBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CuttingBoardBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.isEmpty()) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Direction facing = be.getBlockState().getValue(CuttingBoardBlock.FACING);

        float scaleY = be.getSquashScaleY(partialTick);
        float scaleXZ = be.getSquashScaleXZ(partialTick);

        int itemCount = 0;
        for(int i=0; i<be.getContainerSize(); i++) if(!be.getItem(i).isEmpty()) itemCount++;

        int renderIndex = 0;
        for (int i = 0; i < be.getContainerSize(); i++) {
            ItemStack stack = be.getItem(i);
            if (stack.isEmpty()) continue;

            poseStack.pushPose();

            // 1. 基础定位 (中心点 + 基础高度)
            poseStack.translate(0.5, Y_OFFSET, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

            if (itemCount == 1) {
                // === 原料阶段 (单体，应用压扁动画) ===
                poseStack.mulPose(Axis.XP.rotationDegrees(90f));

                // 应用压扁
                poseStack.scale(ITEM_SCALE * scaleXZ, ITEM_SCALE * scaleXZ, ITEM_SCALE * scaleY);

                // 压扁时修正底部，防止稍微穿模
                if (scaleY < 1.0f) {
                    // 不需要额外位移了，因为中心点在中间，scale变小会自动收缩
                }

            } else {
                // === 产物堆叠阶段 (多个) ===
                long seed = be.getBlockPos().asLong() + i * 777L;
                Random random = new Random(seed);

                // 随机水平分布 (范围稍微收窄一点，让它们更容易堆在一起)
                float offsetX = (random.nextFloat() - 0.5f) * 0.4f;
                float offsetZ = (random.nextFloat() - 0.5f) * 0.25f;

                // [核心修改] 垂直堆叠逻辑
                // renderIndex 0 在最下面，1 在上面，以此类推
                float stackY = renderIndex * STACK_HEIGHT_INCREMENT;

                float rotY = (random.nextFloat() - 0.5f) * 50.0f; // 随机旋转角度大一点，显得乱

                // 应用位移 (X, Y堆叠, Z)
                poseStack.translate(offsetX, stackY, offsetZ);

                poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
                poseStack.mulPose(Axis.XP.rotationDegrees(90f));
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            }

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, be.getLevel(), 0);

            poseStack.popPose();

            // 只有渲染了东西才增加计数器，确保堆叠顺序紧凑
            renderIndex++;
        }
    }
}