package com.infinixmc.protectionblocks.network.packets;

import com.infinixmc.protectionblocks.client.ClientHandler;
import com.infinixmc.protectionblocks.util.AllyData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncProtectionDataPacket {
    private final BlockPos blockPos;
    private final UUID ownerId;
    private final String ownerName;
    private final List<AllyData> allies;
    private final boolean visualizationActive;
    private final int protectionRange; // Agregar el rango de protección
    private final String welcomeMessage; // Agregar mensaje de bienvenida

    public SyncProtectionDataPacket(BlockPos blockPos, UUID ownerId, String ownerName, List<AllyData> allies, boolean visualizationActive, int protectionRange, String welcomeMessage) {
        this.blockPos = blockPos;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.allies = allies;
        this.visualizationActive = visualizationActive;
        this.protectionRange = protectionRange;
        this.welcomeMessage = welcomeMessage != null ? welcomeMessage : "";
    }

    public static void encode(SyncProtectionDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPos);
        
        // Escribir datos del propietario
        if (packet.ownerId != null) {
            buffer.writeBoolean(true);
            buffer.writeUUID(packet.ownerId);
            buffer.writeUtf(packet.ownerName != null ? packet.ownerName : "");
        } else {
            buffer.writeBoolean(false);
        }
        
        buffer.writeBoolean(packet.visualizationActive);
        buffer.writeInt(packet.protectionRange); // Escribir el rango de protección
        buffer.writeUtf(packet.welcomeMessage); // Escribir mensaje de bienvenida
        
        // Escribir lista de aliados
        buffer.writeInt(packet.allies.size());
        for (AllyData ally : packet.allies) {
            buffer.writeUtf(ally.getPlayerName());
            buffer.writeUUID(ally.getPlayerUUID());
            // Escribir permisos
            buffer.writeInt(ally.getPermissions().size());
            for (com.infinixmc.protectionblocks.util.PermissionType permission : ally.getPermissions()) {
                buffer.writeEnum(permission);
            }
        }
    }

    public static SyncProtectionDataPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        
        // Leer datos del propietario
        UUID ownerId = null;
        String ownerName = "";
        if (buffer.readBoolean()) {
            ownerId = buffer.readUUID();
            ownerName = buffer.readUtf();
        }
        
        boolean visualizationActive = buffer.readBoolean();
        int protectionRange = buffer.readInt(); // Leer el rango de protección
        String welcomeMessage = buffer.readUtf(); // Leer mensaje de bienvenida
        
        // Leer lista de aliados
        int allyCount = buffer.readInt();
        List<AllyData> allies = new ArrayList<>();
        for (int i = 0; i < allyCount; i++) {
            String playerName = buffer.readUtf();
            UUID playerUUID = buffer.readUUID();
            AllyData ally = new AllyData(playerName, playerUUID);
            
            // Leer permisos
            int permissionCount = buffer.readInt();
            for (int j = 0; j < permissionCount; j++) {
                com.infinixmc.protectionblocks.util.PermissionType permission = buffer.readEnum(com.infinixmc.protectionblocks.util.PermissionType.class);
                ally.setPermission(permission, true);
            }
            allies.add(ally);
        }
        
        return new SyncProtectionDataPacket(blockPos, ownerId, ownerName, allies, visualizationActive, protectionRange, welcomeMessage);
    }

    public static void handle(SyncProtectionDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Este código solo se ejecuta en el cliente
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientHandler.updateProtectionGUI(packet.blockPos, packet.ownerId != null ? packet.ownerId.toString() : "", 
                    packet.ownerName != null ? packet.ownerName : "", packet.allies, packet.protectionRange, packet.welcomeMessage);
            });
        });
        context.setPacketHandled(true);
    }
}