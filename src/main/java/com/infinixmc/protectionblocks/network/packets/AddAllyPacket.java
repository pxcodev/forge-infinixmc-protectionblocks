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
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class AddAllyPacket {
    private final BlockPos blockPos;
    private final String playerName;

    public AddAllyPacket(BlockPos blockPos, String playerName) {
        this.blockPos = blockPos;
        this.playerName = playerName;
    }

    public static void encode(AddAllyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.playerName);
    }

    public static AddAllyPacket decode(FriendlyByteBuf buffer) {
        return new AddAllyPacket(buffer.readBlockPos(), buffer.readUtf());
    }

    public static void handle(AddAllyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Level level = sender.level();
                BlockEntity blockEntity = level.getBlockEntity(packet.blockPos);
                
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // Verificar que el jugador sea el propietario
                    if (!protectionBlock.isOwner(sender)) {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.only_owner_add"));
                        return;
                    }
                    
                    // Buscar el jugador por nombre
                    ServerPlayer targetPlayer = ServerLifecycleHooks.getCurrentServer()
                            .getPlayerList().getPlayerByName(packet.playerName);
                    
                    if (targetPlayer != null) {
                        UUID targetUUID = targetPlayer.getUUID();
                        String targetName = targetPlayer.getName().getString();
                        
                        // Verificar que no sea el mismo propietario
                        if (targetUUID.equals(sender.getUUID())) {
                            sender.sendSystemMessage(Component.translatable("message.protectionblocks.cannot_add_self"));
                            return;
                        }
                        
                        // Agregar el aliado
                        protectionBlock.addAlly(targetName, targetUUID);
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.ally_added", targetName));
                        targetPlayer.sendSystemMessage(Component.translatable("message.protectionblocks.ally_notification", sender.getName().getString()));
                        
                        // Sincronizar datos con el cliente
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
                            ((ServerPlayer) sender).connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.ally_not_found", packet.playerName));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}