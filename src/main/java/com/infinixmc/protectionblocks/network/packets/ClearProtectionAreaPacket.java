package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.client.ProtectionAreaRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClearProtectionAreaPacket {
    private final BlockPos blockPos;

    public ClearProtectionAreaPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(ClearProtectionAreaPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static ClearProtectionAreaPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        return new ClearProtectionAreaPacket(blockPos);
    }

    public static void handle(ClearProtectionAreaPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Este código se ejecuta en el lado del cliente
            ProtectionAreaRenderer.disableFor(packet.blockPos);
        });
        context.setPacketHandled(true);
    }
}