package com.fyoyi.betterfood.client.renderer.large_pot;

import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LargePotBlockRenderer implements BlockEntityRenderer<PotBlockEntity> {

    public LargePotBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PotBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        NonNullList<ItemStack> items = pBlockEntity.getItems();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        boolean hasWater = false;
        if (pBlockEntity.getLevel() != null && pBlockEntity.getBlockState().getBlock() instanceof com.fyoyi.betterfood.block.large_pot.LargePotBlock) {
            hasWater = pBlockEntity.getBlockState().getValue(com.fyoyi.betterfood.block.large_pot.LargePotBlock.HAS_WATER);
        }

        int activeCount = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty()) activeCount++;
        }

        int currentRenderIndex = 0;
        long time = pBlockEntity.getLevel().getGameTime();
        float animTime = time + pPartialTick;

        long posSeed = pBlockEntity.getBlockPos().asLong();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            pPoseStack.pushPose();

            RandomSource random = RandomSource.create(posSeed + (long) i * 3129871L);
            float randomOffset = random.nextFloat() * 100.0f;
            float randomTiltSpeed = 0.5f + random.nextFloat() * 0.5f;

            if (!hasWater) {
                // --- 无水状态 ---
                float x = 0.5f + (random.nextFloat() * 2.0f - 1.0f) * 0.05f;
                float z = 0.5f + (random.nextFloat() * 2.0f - 1.0f) * 0.05f;
                float y = 0.07f + (i * 0.04f);

                pPoseStack.translate(x, y, z);
                pPoseStack.scale(0.5f, 0.5f, 0.5f);

                pPoseStack.mulPose(Axis.XP.rotationDegrees(90f));
                float randomRot = random.nextFloat() * 360.0f;
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(randomRot));

            } else {
                // --- 有水状态 ---

                float baseRadius = (activeCount <= 1) ? 0.0f : 0.22f;
                float breathing = Mth.sin(animTime * 0.05f + randomOffset) * 0.04f;
                float currentRadius = Math.max(0, baseRadius + breathing);

                float orbitAngle = (animTime * 0.8f) + ((360.0f / activeCount) * currentRenderIndex) + randomOffset;

                float x = 0.5f + currentRadius * Mth.cos((float)Math.toRadians(orbitAngle));
                float z = 0.5f + currentRadius * Mth.sin((float)Math.toRadians(orbitAngle));

                float bigWave = Mth.sin(animTime * 0.08f + randomOffset) * 0.04f;
                float smallRipple = Mth.cos(animTime * 0.2f + randomOffset * 2.0f) * 0.015f;
                float y = 0.6f + bigWave + smallRipple;

                pPoseStack.translate(x, y, z);

                pPoseStack.scale(0.4f, 0.4f, 0.4f);

                // 自转
                pPoseStack.mulPose(Axis.YP.rotationDegrees(-orbitAngle + (animTime * 1.5f)));

                // 摇摆
                float swayX = Mth.sin(animTime * 0.12f * randomTiltSpeed + randomOffset) * 10.0f;
                float swayZ = Mth.cos(animTime * 0.09f * randomTiltSpeed + randomOffset) * 10.0f;

                pPoseStack.mulPose(Axis.XP.rotationDegrees(swayX));
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
            }

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, pPackedLight, OverlayTexture.NO_OVERLAY, pPoseStack, pBufferSource, pBlockEntity.getLevel(), 0);

            pPoseStack.popPose();
            currentRenderIndex++;
        }
    }
}