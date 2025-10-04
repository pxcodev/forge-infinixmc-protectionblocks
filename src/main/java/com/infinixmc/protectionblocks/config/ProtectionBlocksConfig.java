package com.infinixmc.protectionblocks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ProtectionBlocksConfig {
    
    // Variable para rastrear si la configuración ya fue registrada
    private static boolean registered = false;
    
    /**
     * Registra la configuración del mod
     */
    public static void register() {
        if (!registered) {
            try {
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC, "protectionblocks/protectionblocks-server.toml");
                registered = true;
                System.out.println("[ProtectionBlocks] Server configuration registered: config/protectionblocks/protectionblocks-server.toml");
            } catch (Exception e) {
                System.err.println("[ProtectionBlocks] Failed to register server configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Obtiene el límite máximo de bloques de protección por jugador
     * @return el límite, o -1 si es ilimitado
     */
    public static int getMaxProtectionBlocksPerPlayer() {
        try {
            return ServerConfig.MAX_PROTECTION_BLOCKS_PER_PLAYER.get();
        } catch (Exception e) {
            System.err.println("[ProtectionBlocks] Error accessing config, using default value: " + e.getMessage());
            return 2; // Valor por defecto actualizado para coincidir con configuración
        }
    }
    
    /**
     * Verifica si un jugador puede colocar más bloques de protección
     * @param currentCount número actual de bloques que tiene el jugador
     * @return true si puede colocar más bloques
     */
    public static boolean canPlaceMoreBlocks(int currentCount) {
        int maxBlocks = getMaxProtectionBlocksPerPlayer();
        return maxBlocks == -1 || currentCount < maxBlocks;
    }
    
    /**
     * Obtiene el rango de protección para un tipo específico de bloque
     * @param blockType el tipo de bloque ("basic", "advanced", "superior", "elite", "master")
     * @return el rango de protección configurado
     */
    public static int getProtectionRangeForType(String blockType) {
        try {
            switch (blockType.toLowerCase()) {
                case "basic":
                    return ServerConfig.BASIC_PROTECTION_RANGE.get();
                case "advanced":
                    return ServerConfig.ADVANCED_PROTECTION_RANGE.get();
                case "superior":
                    return ServerConfig.SUPERIOR_PROTECTION_RANGE.get();
                case "elite":
                    return ServerConfig.ELITE_PROTECTION_RANGE.get();
                case "master":
                    return ServerConfig.MASTER_PROTECTION_RANGE.get();
                default:
                    return 6; // Valor por defecto (básico) actualizado para coincidir con configuración
            }
        } catch (Exception e) {
            return 6; // Valor por defecto actualizado para coincidir con configuración
        }
    }
    
    /**
     * Obtiene el rango de protección del Bloque Básico
     * @return el rango del bloque básico
     */
    public static int getBasicProtectionRange() {
        try {
            return ServerConfig.BASIC_PROTECTION_RANGE.get();
        } catch (Exception e) {
            return 6; // Valor por defecto actualizado para coincidir con configuración
        }
    }
    
    /**
     * Obtiene el rango de protección del Bloque Avanzado
     * @return el rango del bloque avanzado
     */
    public static int getAdvancedProtectionRange() {
        try {
            return ServerConfig.ADVANCED_PROTECTION_RANGE.get();
        } catch (Exception e) {
            return 10; // Valor por defecto
        }
    }
    
    /**
     * Obtiene el rango de protección del Bloque Superior
     * @return el rango del bloque superior
     */
    public static int getSuperiorProtectionRange() {
        try {
            return ServerConfig.SUPERIOR_PROTECTION_RANGE.get();
        } catch (Exception e) {
            return 15; // Valor por defecto
        }
    }
    
    /**
     * Obtiene el rango de protección del Bloque Élite
     * @return el rango del bloque élite
     */
    public static int getEliteProtectionRange() {
        try {
            return ServerConfig.ELITE_PROTECTION_RANGE.get();
        } catch (Exception e) {
            return 20; // Valor por defecto
        }
    }
    
    /**
     * Obtiene el rango de protección del Bloque Maestro
     * @return el rango del bloque maestro
     */
    public static int getMasterProtectionRange() {
        try {
            return ServerConfig.MASTER_PROTECTION_RANGE.get();
        } catch (Exception e) {
            return 25; // Valor por defecto
        }
    }
    
    /**
     * Verifica si las áreas de protección pueden superponerse
     * @return true si se permite la superposición
     */
    public static boolean isOverlappingAllowed() {
        try {
            return ServerConfig.ALLOW_OVERLAPPING_PROTECTION.get();
        } catch (Exception e) {
            return false; // Valor por defecto
        }
    }
    
    /**
     * Obtiene la distancia mínima entre bloques de protección
     * @return la distancia mínima
     */
    public static int getMinDistanceBetweenBlocks() {
        try {
            return ServerConfig.MIN_DISTANCE_BETWEEN_BLOCKS.get();
        } catch (Exception e) {
            return 0; // Valor por defecto
        }
    }
    
    /**
     * Verifica si la visualización de protección está habilitada
     * @return true si está habilitada
     */
    public static boolean isVisualizationEnabled() {
        try {
            return ServerConfig.ENABLE_PROTECTION_VISUALIZATION.get();
        } catch (Exception e) {
            return true; // Valor por defecto
        }
    }
    
    /**
     * Obtiene el número máximo de aliados por bloque
     * @return el máximo número de aliados, o -1 si es ilimitado
     */
    public static int getMaxAlliesPerBlock() {
        try {
            return ServerConfig.MAX_ALLIES_PER_BLOCK.get();
        } catch (Exception e) {
            return 10; // Valor por defecto
        }
    }
    
    /**
     * Verifica si se pueden agregar más aliados a un bloque
     * @param currentCount número actual de aliados
     * @return true si se pueden agregar más aliados
     */
    public static boolean canAddMoreAllies(int currentCount) {
        int maxAllies = getMaxAlliesPerBlock();
        return maxAllies == -1 || currentCount < maxAllies;
    }
    
    /**
     * Verifica si las recetas de bloques de protección están habilitadas
     * @return true si las recetas están habilitadas
     */
    public static boolean areProtectionBlockRecipesEnabled() {
        try {
            return ServerConfig.ENABLE_PROTECTION_BLOCK_RECIPES.get();
        } catch (Exception e) {
            System.err.println("[ProtectionBlocks] Error accessing recipe config, using default value: " + e.getMessage());
            return true; // Valor por defecto - recetas habilitadas
        }
    }
}