package com.infinixmc.protectionblocks.util;

public enum PermissionType {
    BUILD("build", "Construir"),
    BREAK("break", "Romper bloques"),
    INTERACT("interact", "Usar objetos");
    
    private final String id;
    private final String displayName;
    
    PermissionType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static PermissionType fromId(String id) {
        for (PermissionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}