package com.fyoyi.betterfood.network;

import com.fyoyi.betterfood.client.gui.PotInfoOverlay;
import com.fyoyi.betterfood.client.renderer.PotWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PotMessagePacket {
    private final BlockPos pos;
    private final String message;

    public PotMessagePacket(BlockPos pos, String message) {
        this.pos = pos;
        this.message = message;
    }

    public static void encode(PotMessagePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUtf(pkt.message);
    }

    public static PotMessagePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String message = buf.readUtf();
        return new PotMessagePacket(pos, message);
    }

    public static void handle(PotMessagePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 使用新的世界渲染器，在锅的上方显示消息
            PotWorldRenderer.addTempMessage(pkt.pos, pkt.message);
        });
        ctx.get().setPacketHandled(true);
    }
}