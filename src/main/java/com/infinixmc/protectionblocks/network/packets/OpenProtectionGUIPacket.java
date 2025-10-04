package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.client.ClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenProtectionGUIPacket {
    private final BlockPos blockPos;

    public OpenProtectionGUIPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(OpenProtectionGUIPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static OpenProtectionGUIPacket decode(FriendlyByteBuf buffer) {
        return new OpenProtectionGUIPacket(buffer.readBlockPos());
    }

    public static void handle(OpenProtectionGUIPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Este código solo se ejecuta en el cliente
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientHandler.openProtectionGUI(packet.blockPos);
            });
        });
        context.setPacketHandled(true);
    }
}