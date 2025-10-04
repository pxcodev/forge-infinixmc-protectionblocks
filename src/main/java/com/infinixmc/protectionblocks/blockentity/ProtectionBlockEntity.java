package com.infinixmc.protectionblocks.blockentity;

import com.infinixmc.protectionblocks.init.ModBlockEntities;
import com.infinixmc.protectionblocks.util.AllyData;
import com.infinixmc.protectionblocks.util.PermissionType;
import com.infinixmc.protectionblocks.util.ProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ProtectionBlockEntity extends BlockEntity {
    private UUID ownerId;
    private String ownerName = "";
    private final int protectionRange;
    private int syncedProtectionRange; // Rango sincronizado desde el servidor
    private boolean visualizationActive = false;
    private List<AllyData> allies = new ArrayList<>();
    private String zoneName = ""; // Nombre de la zona de protección
    private String welcomeMessage = ""; // Mensaje de bienvenida

    public ProtectionBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PROTECTION_BLOCK_ENTITY.get(), pos, blockState);
        // Determinar el rango basado en el tipo de bloque
        String blockName = blockState.getBlock().toString();
        if (blockName.contains("basic")) {
            this.protectionRange = 5;
        } else if (blockName.contains("advanced")) {
            this.protectionRange = 10;
        } else if (blockName.contains("superior")) {
            this.protectionRange = 15;
        } else if (blockName.contains("elite")) {
            this.protectionRange = 20;
        } else if (blockName.contains("master")) {
            this.protectionRange = 25;
        } else {
            this.protectionRange = 5; // Por defecto
        }
    }

    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
    }

    // Nuevo método para establecer datos del propietario desde el paquete de sincronización
    public void setOwnerData(UUID ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    // Nuevo método para sincronizar la lista de aliados
    public void syncAllies(List<AllyData> newAllies) {
        this.allies.clear();
        this.allies.addAll(newAllies);
    }

    // Nuevo método para establecer el estado de visualización
    public void setVisualizationActive(boolean active) {
        this.visualizationActive = active;
    }

    public boolean isVisualizationActive() {
        return visualizationActive;
    }

    // Métodos para el nombre de la zona
    public String getZoneName() {
        return zoneName.isEmpty() ? "Zona sin nombre" : zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
        setChanged();
    }

    // Métodos para el mensaje de bienvenida
    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        setChanged();
    }

    // Métodos para el rango sincronizado
    public int getSyncedProtectionRange() {
        return syncedProtectionRange > 0 ? syncedProtectionRange : protectionRange;
    }

    public void setSyncedProtectionRange(int range) {
        this.syncedProtectionRange = range;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName.isEmpty() ? "Desconocido" : ownerName;
    }

    public int getProtectionRange() {
        return protectionRange;
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public boolean isOwnerOrAlly(Player player) {
        if (isOwner(player)) return true;
        return allies.stream().anyMatch(ally -> ally.getPlayerUUID().equals(player.getUUID()));
    }

    public boolean hasPermission(Player player, PermissionType permission) {
        if (isOwner(player)) return true;
        return allies.stream()
                .filter(ally -> ally.getPlayerUUID().equals(player.getUUID()))
                .anyMatch(ally -> ally.hasPermission(permission));
    }

    // Métodos de gestión de aliados
    public void addAlly(String playerName, UUID playerUUID) {
        AllyData newAlly = new AllyData(playerName, playerUUID);
        if (!allies.contains(newAlly)) {
            allies.add(newAlly);
            setChanged();
        }
    }

    public void removeAlly(String playerName) {
        allies.removeIf(ally -> ally.getPlayerName().equalsIgnoreCase(playerName));
        setChanged();
    }

    public void removeAlly(UUID playerUUID) {
        allies.removeIf(ally -> ally.getPlayerUUID().equals(playerUUID));
        setChanged();
    }

    public List<AllyData> getAllies() {
        return new ArrayList<>(allies);
    }

    public AllyData getAlly(String playerName) {
        return allies.stream()
                .filter(ally -> ally.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);
    }

    public AllyData getAlly(UUID playerUUID) {
        return allies.stream()
                .filter(ally -> ally.getPlayerUUID().equals(playerUUID))
                .findFirst()
                .orElse(null);
    }

    public void updateAllyPermissions(String playerName, Set<PermissionType> permissions) {
        AllyData ally = getAlly(playerName);
        if (ally != null) {
            ally.setPermissions(permissions);
            setChanged();
        }
    }

    public String getBlockName() {
        String blockName = getBlockState().getBlock().toString();
        if (blockName.contains("basic")) return "Básico";
        if (blockName.contains("advanced")) return "Avanzado";
        if (blockName.contains("superior")) return "Superior";
        if (blockName.contains("elite")) return "Elite";
        if (blockName.contains("master")) return "Maestro";
        return "Protección";
    }

    public boolean isInProtectionRange(BlockPos targetPos) {
        int halfRange = protectionRange / 2;
        return Math.abs(targetPos.getX() - worldPosition.getX()) <= halfRange &&
               Math.abs(targetPos.getZ() - worldPosition.getZ()) <= halfRange &&
               Math.abs(targetPos.getY() - worldPosition.getY()) <= halfRange;
    }

    public void toggleVisualizationMode(Player player) {
        if (isOwner(player)) {
            visualizationActive = !visualizationActive;
            if (visualizationActive) {
                ProtectionManager.showProtectionArea(player, worldPosition, protectionRange);
                player.sendSystemMessage(Component.literal("§aVisualización de protección activada"));
            } else {
                ProtectionManager.hideProtectionArea(player);
                player.sendSystemMessage(Component.literal("§cVisualización de protección desactivada"));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cSolo el propietario puede cambiar la visualización"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerId != null) {
            tag.putUUID("OwnerId", ownerId);
            tag.putString("OwnerName", ownerName);
        }
        tag.putBoolean("VisualizationActive", visualizationActive);
        tag.putString("ZoneName", zoneName);
        tag.putString("WelcomeMessage", welcomeMessage);
        tag.putInt("SyncedProtectionRange", syncedProtectionRange);
        
        // Guardar aliados
        ListTag alliesTag = new ListTag();
        for (AllyData ally : allies) {
            alliesTag.add(ally.save());
        }
        tag.put("Allies", alliesTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerId")) {
            this.ownerId = tag.getUUID("OwnerId");
            this.ownerName = tag.getString("OwnerName");
        }
        this.visualizationActive = tag.getBoolean("VisualizationActive");
        this.zoneName = tag.getString("ZoneName");
        this.welcomeMessage = tag.getString("WelcomeMessage");
        this.syncedProtectionRange = tag.getInt("SyncedProtectionRange");
        
        // Cargar aliados
        allies.clear();
        if (tag.contains("Allies", Tag.TAG_LIST)) {
            ListTag alliesTag = tag.getList("Allies", Tag.TAG_COMPOUND);
            for (int i = 0; i < alliesTag.size(); i++) {
                CompoundTag allyTag = alliesTag.getCompound(i);
                allies.add(new AllyData(allyTag));
            }
        }
    }
}