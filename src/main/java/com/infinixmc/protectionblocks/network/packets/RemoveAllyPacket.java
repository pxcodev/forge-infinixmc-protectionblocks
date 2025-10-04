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

public class RemoveAllyPacket {
    private final BlockPos blockPos;
    private final String playerName;

    public RemoveAllyPacket(BlockPos blockPos, String playerName) {
        this.blockPos = blockPos;
        this.playerName = playerName;
    }

    public static void encode(RemoveAllyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.playerName);
    }

    public static RemoveAllyPacket decode(FriendlyByteBuf buffer) {
        return new RemoveAllyPacket(buffer.readBlockPos(), buffer.readUtf());
    }

    public static void handle(RemoveAllyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Level level = sender.level();
                BlockEntity blockEntity = level.getBlockEntity(packet.blockPos);
                
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // Verificar que el jugador sea el propietario
                    if (!protectionBlock.isOwner(sender)) {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.only_owner_remove"));
                        return;
                    }
                    
                    // Verificar que el aliado existe
                    if (protectionBlock.getAlly(packet.playerName) != null) {
                        protectionBlock.removeAlly(packet.playerName);
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.ally_removed", packet.playerName));
                        
                        // Sincronizar datos con el cliente
                        NetworkHandler.INSTANCE.sendTo(
                            new SyncProtectionDataPacket(
                                packet.blockPos,
                                protectionBlock.getOwnerId(),
                                protectionBlock.getOwnerName(),
                                protectionBlock.getAllies(),
                                false,
                                protectionBlock.getProtectionRange(), // Agregar el rango calculado en el servidor
                                protectionBlock.getWelcomeMessage() // Agregar mensaje de bienvenida
                            ),
                            ((ServerPlayer) sender).connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.ally_not_exists", packet.playerName));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}