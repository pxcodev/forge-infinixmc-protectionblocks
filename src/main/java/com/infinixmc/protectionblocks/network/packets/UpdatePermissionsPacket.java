package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.util.PermissionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class UpdatePermissionsPacket {
    private final BlockPos blockPos;
    private final String playerName;
    private final Set<PermissionType> permissions;

    public UpdatePermissionsPacket(BlockPos blockPos, String playerName, Set<PermissionType> permissions) {
        this.blockPos = blockPos;
        this.playerName = playerName;
        this.permissions = permissions;
    }

    public static void encode(UpdatePermissionsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUtf(packet.playerName);
        buffer.writeInt(packet.permissions.size());
        for (PermissionType permission : packet.permissions) {
            buffer.writeEnum(permission);
        }
    }

    public static UpdatePermissionsPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        String playerName = buffer.readUtf();
        int permissionCount = buffer.readInt();
        Set<PermissionType> permissions = new HashSet<>();
        for (int i = 0; i < permissionCount; i++) {
            permissions.add(buffer.readEnum(PermissionType.class));
        }
        return new UpdatePermissionsPacket(blockPos, playerName, permissions);
    }

    public static void handle(UpdatePermissionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Level level = sender.level();
                BlockEntity blockEntity = level.getBlockEntity(packet.blockPos);
                
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // Verificar que el jugador sea el propietario
                    if (!protectionBlock.isOwner(sender)) {
                        sender.sendSystemMessage(Component.translatable("message.protectionblocks.only_owner_permissions"));
                        return;
                    }
                    
                    // Actualizar permisos
                    protectionBlock.updateAllyPermissions(packet.playerName, packet.permissions);
                    sender.sendSystemMessage(Component.translatable("message.protectionblocks.permissions_updated", packet.playerName));
                }
            }
        });
        context.setPacketHandled(true);
    }
}