package com.fyoyi.betterfood.client.renderer.cutting_board;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.block.cutting_board.CuttingBoardBlock;
import com.fyoyi.betterfood.block.entity.CuttingBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CuttingBoardItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final CuttingBoardBlockEntity dummyEntity;
    private final CuttingBoardBlockRenderer renderer;

    public CuttingBoardItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());

        // 创建一个用于渲染的假实体
        BlockState defaultState = ModBlocks.CUTTING_BOARD.get().defaultBlockState()
                .setValue(CuttingBoardBlock.FACING, Direction.NORTH);
        this.dummyEntity = new CuttingBoardBlockEntity(BlockPos.ZERO, defaultState);

        // 复用方块的渲染器
        this.renderer = new CuttingBoardBlockRenderer(null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // 1. 确保有 Level 实例，否则渲染可能报错
        if (dummyEntity.getLevel() == null) {
            dummyEntity.setLevel(Minecraft.getInstance().level);
        }

        // 2. 读取物品 NBT 数据到假实体
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            dummyEntity.load(tag);
        } else {
            dummyEntity.clearContent();
        }

        // 3. 渲染菜板方块本身
        poseStack.pushPose();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.CUTTING_BOARD.get().defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        // 4. 调用 BE 渲染器渲染上面的食物
        // 注意：这里 partialTick 传 0，因为物品栏里不需要播放"压扁"动画
        renderer.render(dummyEntity, 0.0f, poseStack, buffer, packedLight, packedOverlay);
    }
}