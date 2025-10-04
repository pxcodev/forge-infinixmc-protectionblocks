package com.infinixmc.protectionblocks.util;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class AllyData {
    private String playerName;
    private UUID playerUUID;
    private Set<PermissionType> permissions;
    
    public AllyData(String playerName, UUID playerUUID) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.permissions = EnumSet.noneOf(PermissionType.class);
    }
    
    public AllyData(CompoundTag tag) {
        this.playerName = tag.getString("PlayerName");
        this.playerUUID = tag.getUUID("PlayerUUID");
        this.permissions = EnumSet.noneOf(PermissionType.class);
        
        // Cargar permisos
        if (tag.getBoolean("CanBuild")) permissions.add(PermissionType.BUILD);
        if (tag.getBoolean("CanBreak")) permissions.add(PermissionType.BREAK);
        if (tag.getBoolean("CanInteract")) permissions.add(PermissionType.INTERACT);
    }
    
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerName", playerName);
        tag.putUUID("PlayerUUID", playerUUID);
        tag.putBoolean("CanBuild", permissions.contains(PermissionType.BUILD));
        tag.putBoolean("CanBreak", permissions.contains(PermissionType.BREAK));
        tag.putBoolean("CanInteract", permissions.contains(PermissionType.INTERACT));
        return tag;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public boolean hasPermission(PermissionType permission) {
        return permissions.contains(permission);
    }
    
    public void setPermission(PermissionType permission, boolean value) {
        if (value) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }
    
    public Set<PermissionType> getPermissions() {
        return EnumSet.copyOf(permissions);
    }
    
    public void setPermissions(Set<PermissionType> permissions) {
        this.permissions = EnumSet.copyOf(permissions);
    }
    
    public boolean hasAnyPermission() {
        return !permissions.isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AllyData allyData = (AllyData) obj;
        return playerUUID.equals(allyData.playerUUID);
    }
    
    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
    
    @Override
    public String toString() {
        return "AllyData{" +
                "playerName='" + playerName + '\'' +
                ", playerUUID=" + playerUUID +
                ", permissions=" + permissions +
                '}';
    }
}