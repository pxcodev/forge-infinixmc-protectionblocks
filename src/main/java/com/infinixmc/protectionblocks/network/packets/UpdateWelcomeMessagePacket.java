package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

public class UpdateWelcomeMessagePacket {
    private final BlockPos blockPos;
    private final String welcomeMessage;

    public UpdateWelcomeMessagePacket(BlockPos blockPos, String welcomeMessage) {
        this.blockPos = blockPos;
        this.welcomeMessage = welcomeMessage != null ? welcomeMessage : "";
    }

    public static void encode(UpdateWelcomeMessagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.welcomeMessage);
    }

    public static UpdateWelcomeMessagePacket decode(FriendlyByteBuf buffer) {
        return new UpdateWelcomeMessagePacket(buffer.readBlockPos(), buffer.readUtf());
    }

    public static void handle(UpdateWelcomeMessagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Level level = sender.level();
                BlockEntity blockEntity = level.getBlockEntity(packet.blockPos);
                
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // Verificar que el jugador sea el propietario
                    if (!protectionBlock.isOwner(sender)) {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.only_owner_modify"));
                        return;
                    }
                    
                    // Actualizar el mensaje de bienvenida
                    protectionBlock.setWelcomeMessage(packet.welcomeMessage);
                    
                    // Sincronizar datos con el cliente para actualizar la GUI
                    NetworkHandler.INSTANCE.sendTo(
                        new SyncProtectionDataPacket(
                            packet.blockPos,
                            protectionBlock.getOwnerId(),
                            protectionBlock.getOwnerName(),
                            protectionBlock.getAllies(),
                            false,
                            protectionBlock.getProtectionRange(),
                            protectionBlock.getWelcomeMessage()
                        ),
                        sender.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                    );
                    
                    // Enviar confirmación al jugador
                    if (packet.welcomeMessage.isEmpty()) {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.zone_name_cleared"));
                    } else {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.zone_name_updated"));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}