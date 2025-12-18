package com.fyoyi.betterfood.client.renderer.cooking_pan;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.client.renderer.PotBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class PotItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final PotBlockEntity dummyEntity;
    private final PotBlockRenderer renderer;

    public PotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        BlockState defaultState = ModBlocks.COOKING_PAN.get().defaultBlockState();
        this.dummyEntity = new PotBlockEntity(BlockPos.ZERO, defaultState);
        // 手动创建渲染器，参数给 null 即可
        this.renderer = new PotBlockRenderer(null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // 1. 设置世界，保证光照正常
        if (dummyEntity.getLevel() == null) {
            dummyEntity.setLevel(Minecraft.getInstance().level);
        }

        // 2. 加载数据
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            dummyEntity.load(tag);
        } else {
            dummyEntity.load(new CompoundTag());
        }

        // 3. 画锅 (调用方块渲染器画外壳)
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.COOKING_PAN.get().defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        // 4. 画食物 (强制调用我们的渲染器)
        renderer.render(
                dummyEntity,
                0.0f,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }
}