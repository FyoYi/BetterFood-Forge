package com.fyoyi.betterfood.client.renderer.large_pot;

import com.fyoyi.betterfood.block.ModBlocks;
import com.fyoyi.betterfood.block.entity.PotBlockEntity;
import com.fyoyi.betterfood.block.large_pot.LargePotBlock;
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

public class LargePotItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final PotBlockEntity dummyEntity;
    private final LargePotBlockRenderer renderer;

    public LargePotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());

        // 初始化一个默认状态的实体
        BlockState defaultState = ModBlocks.LARGE_POT.get().defaultBlockState();
        this.dummyEntity = new PotBlockEntity(BlockPos.ZERO, defaultState);

        // 实例化方块渲染器
        this.renderer = new LargePotBlockRenderer(null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // 1. 设置世界环境 (防止空指针)
        if (dummyEntity.getLevel() == null) {
            dummyEntity.setLevel(Minecraft.getInstance().level);
        }

        // 2. 解析 NBT 数据
        // 先准备一个基础的 BlockState
        BlockState renderState = ModBlocks.LARGE_POT.get().defaultBlockState()
                .setValue(LargePotBlock.FACING, Direction.SOUTH); // 物品栏通常朝南比较好看

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        boolean hasWater = false;
        boolean hasLid = false;

        if (tag != null) {
            // 加载物品列表到实体中
            dummyEntity.load(tag);

            // 读取是否有水
            if (tag.contains("has_water") && tag.getBoolean("has_water")) {
                hasWater = true;
            }
            // 读取是否有盖子
            if (tag.contains("has_lid") && tag.getBoolean("has_lid")) {
                hasLid = true;
            }
        } else {
            // 如果没有 tag，确保实体也是空的
            dummyEntity.load(new CompoundTag());
        }

        // 3. 构建正确的 BlockState 并赋予实体 (关键步骤！)
        if (hasWater) {
            renderState = renderState.setValue(LargePotBlock.HAS_WATER, true);
        }
        if (hasLid) {
            renderState = renderState.setValue(LargePotBlock.HAS_LID, true);
        }

        // 【核心修改】告诉 dummyEntity："你现在是有水的"
        // 这样 LargePotBlockRenderer 在读取 pBlockEntity.getBlockState() 时才能拿到正确的值
        dummyEntity.setBlockState(renderState);


        // 4. 渲染方块模型 (锅的外壳)
        poseStack.pushPose();
        // 渲染外壳
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                renderState,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();


        // 5. 渲染食物 (调用你的 BlockEntityRenderer)
        // 注意：如果盖子盖着，你可能不想渲染里面的食物？
        // 如果你想透过盖子缝隙看，或者盖子是透明的，就保留。
        // 如果想盖上就不渲染食物，加上: if (!hasLid) { ... }

        renderer.render(
                dummyEntity,
                0.0f,  // partialTick 在物品里通常给 0，或者给 Minecraft.getInstance().getPartialTick() 让它动起来
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }
}