package com.fyoyi.betterfood.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PotWorldRenderer {

    private static final Map<BlockPos, TempMessage> MESSAGES = new ConcurrentHashMap<>();

    public static class TempMessage {
        public final String text;
        public final long startTime;
        public final long duration;

        public TempMessage(String text, long startTime, long duration) {
            this.text = text;
            this.startTime = startTime;
            this.duration = duration;
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(PotWorldRenderer.class);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        net.minecraft.client.Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        long time = level.getGameTime();

        MESSAGES.entrySet().removeIf(e ->
                time - e.getValue().startTime > e.getValue().duration
        );

        for (Map.Entry<BlockPos, TempMessage> entry : MESSAGES.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.distToCenterSqr(camPos) > 256.0) continue;

            renderText(poseStack, buffer, pos, entry.getValue().text, camera);
        }

        buffer.endBatch();
    }

    public static void addTempMessage(BlockPos pos, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MESSAGES.put(
                pos,
                new TempMessage(text, mc.level.getGameTime(), 100)
        );
    }

    private static void renderText(
            PoseStack poseStack,
            MultiBufferSource buffer,
            BlockPos pos,
            String text,
            net.minecraft.client.Camera camera
    ) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();

        poseStack.translate(
                pos.getX() + 0.5D - camPos.x,
                pos.getY() + 1.2D - camPos.y,
                pos.getZ() + 0.5D - camPos.z
        );

        // billboard
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.015F, -0.015F, 0.015F);

        Component c = Component.literal(text);
        Matrix4f mat = poseStack.last().pose();
        float x = -mc.font.width(c) / 2f;

        mc.font.drawInBatch(
                c,
                x,
                0,
                0xFFFFFFFF,
                false,
                mat,
                buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,  // 透明背景
                0xF000F0
        );

        poseStack.popPose();
    }
}
