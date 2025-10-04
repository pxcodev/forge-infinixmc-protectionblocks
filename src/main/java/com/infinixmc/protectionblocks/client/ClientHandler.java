package com.infinixmc.protectionblocks.client;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.gui.ProtectionManagementScreen;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import com.infinixmc.protectionblocks.network.packets.RequestSyncPacket;
import com.infinixmc.protectionblocks.util.AllyData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {
    
    public static void openProtectionGUI(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                // Solicitar sincronización de datos antes de abrir la GUI
                NetworkHandler.sendToServer(new RequestSyncPacket(blockPos));
                minecraft.setScreen(new ProtectionManagementScreen(protectionBlock, blockPos, level));
            }
        }
    }
    
    public static void updateProtectionGUI(BlockPos blockPos, String ownerUuid, String ownerName, List<AllyData> allies, int protectionRange, String welcomeMessage) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                // Actualizar datos del propietario
                UUID ownerUUID = ownerUuid.isEmpty() ? null : UUID.fromString(ownerUuid);
                protectionBlock.setOwnerData(ownerUUID, ownerName);
                
                // Actualizar el rango sincronizado desde el servidor
                protectionBlock.setSyncedProtectionRange(protectionRange);
                
                // Actualizar mensaje de bienvenida
                protectionBlock.setWelcomeMessage(welcomeMessage);
                
                // Actualizar lista de aliados
                protectionBlock.syncAllies(allies);
                
                // Marcar como cambiado para actualizar la GUI
                protectionBlock.setChanged();
                
                // Actualizar la GUI si está abierta
                if (minecraft.screen instanceof ProtectionManagementScreen screen) {
                    // Por ahora simplemente cerramos y reabrimos para simplificar
                    screen.onClose();
                    minecraft.setScreen(new ProtectionManagementScreen(protectionBlock, blockPos, level));
                }
            }
        }
    }
}