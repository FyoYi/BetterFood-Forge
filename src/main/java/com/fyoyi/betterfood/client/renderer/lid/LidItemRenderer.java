package com.fyoyi.betterfood.client.renderer.lid;

import com.fyoyi.betterfood.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LidItemRenderer extends BlockEntityWithoutLevelRenderer {

    public LidItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 渲染锅盖方块模型
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.LID.get().defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }
}