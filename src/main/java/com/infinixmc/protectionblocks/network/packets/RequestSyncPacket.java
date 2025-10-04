package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

public class RequestSyncPacket {
    private final BlockPos blockPos;

    public RequestSyncPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(RequestSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
    }

    public static RequestSyncPacket decode(FriendlyByteBuf buffer) {
        return new RequestSyncPacket(buffer.readBlockPos());
    }

    public static void handle(RequestSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Level level = sender.level();
                BlockEntity blockEntity = level.getBlockEntity(packet.blockPos);
                
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // Enviar datos sincronizados al cliente
                    NetworkHandler.INSTANCE.sendTo(
                        new SyncProtectionDataPacket(
                            packet.blockPos,
                            protectionBlock.getOwnerId(),
                            protectionBlock.getOwnerName(),
                            protectionBlock.getAllies(),
                            false, // visualizationActive - se puede obtener del bloque si es necesario
                            protectionBlock.getProtectionRange(), // Agregar el rango calculado en el servidor
                            protectionBlock.getWelcomeMessage() // Agregar mensaje de bienvenida
                        ),
                        sender.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }
}