package com.fyoyi.betterfood.client.renderer.cooking_pan;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class PotItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final PotBlockEntity dummyEntity;
    private final CookingPanBlockRenderer renderer;

    private static final ResourceLocation OIL_TEXTURE = new ResourceLocation("better_food", "textures/block/oil_static.png");

    // === 静态贴图，不需要帧数 ===
    private static final float FRAME_COUNT = 1.0f;

    public PotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        BlockState defaultState = ModBlocks.COOKING_PAN.get().defaultBlockState();
        this.dummyEntity = new PotBlockEntity(net.minecraft.core.BlockPos.ZERO, defaultState);
        this.renderer = new CookingPanBlockRenderer(null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (dummyEntity.getLevel() == null) {
            dummyEntity.setLevel(Minecraft.getInstance().level);
        }

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        boolean hasOil = false;

        if (tag != null) {
            dummyEntity.load(tag);
            if (tag.contains("HasOil")) {
                hasOil = tag.getBoolean("HasOil");
                dummyEntity.setHasOil(hasOil);
            }
        } else {
            dummyEntity.clearContent();
            dummyEntity.setHasOil(false);
        }

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.COOKING_PAN.get().defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        if (hasOil) {
            renderOilLayer(poseStack, buffer, packedLight);
        }

        renderer.render(
                dummyEntity,
                0.0f,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    // === 手动绘制静态第一帧 ===
    private void renderOilLayer(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. 直接加载原始图片 (不通过 Atlas，所以是静态的)
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(OIL_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        // 2. 空间坐标 (from/to / 16.0f)
        float minX = 2.8f / 16.0f;
        float maxX = 12.5f / 16.0f;
        float minZ = 2.8f / 16.0f;
        float maxZ = 12.5f / 16.0f;
        float y = 1.05f / 16.0f;

        // 3. 纹理坐标计算 (关键！)
        // JSON设定: [0, 0, 4.8, 4.8] -> 截取左上角 4.8 的区域放大

        // U轴 (宽度): 0 到 1 代表整张图片的宽。
        // 我们要截取 4.8/16.0 的宽度
        float uMin = 0.0f;
        float uMax = 4.8f / 16.0f;

        // V轴 (高度): 0 到 1 代表整张图片的高。
        // 对于静态贴图，直接使用 4.8/16.0 的高度
        float vMin = 0.0f;
        float vMax = 4.8f / 16.0f;

        // 4. 绘制
        vertexConsumer.vertex(pose, minX, y, minZ).color(255, 255, 255, 255).uv(uMin, vMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(pose, minX, y, maxZ).color(255, 255, 255, 255).uv(uMin, vMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(pose, maxX, y, maxZ).color(255, 255, 255, 255).uv(uMax, vMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(pose, maxX, y, minZ).color(255, 255, 255, 255).uv(uMax, vMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();

        poseStack.popPose();
    }
}