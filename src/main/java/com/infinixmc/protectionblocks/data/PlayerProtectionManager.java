package com.infinixmc.protectionblocks.data;

import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase para rastrear los bloques de protección de cada jugador
 */
public class PlayerProtectionManager {
    
    // Mapa que almacena los bloques de protección por UUID del jugador
    private static final Map<UUID, Set<BlockPos>> playerProtectionBlocks = new ConcurrentHashMap<>();
    
    /**
     * Registra un nuevo bloque de protección para un jugador
     * @param playerUUID UUID del jugador
     * @param blockPos posición del bloque
     */
    public static void addProtectionBlock(UUID playerUUID, BlockPos blockPos) {
        playerProtectionBlocks.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(blockPos);
        
        // Log de la operación
        int totalBlocks = getProtectionBlockCount(playerUUID);
        DebugLogger.log("Bloque agregado: " + blockPos + " (Total: " + totalBlocks + ")");
        
        // Guardar datos inmediatamente después de agregar
        PlayerDataPersistence.savePlayerData();
    }
    
    /**
     * Remueve un bloque de protección de un jugador
     * @param playerUUID UUID del jugador
     * @param blockPos posición del bloque
     */
    public static void removeProtectionBlock(UUID playerUUID, BlockPos blockPos) {
        Set<BlockPos> blocks = playerProtectionBlocks.get(playerUUID);
        if (blocks != null) {
            blocks.remove(blockPos);
            if (blocks.isEmpty()) {
                playerProtectionBlocks.remove(playerUUID);
            }
            
            // Log de la operación
            int totalBlocks = getProtectionBlockCount(playerUUID);
            DebugLogger.log("Bloque removido: " + blockPos + " (Total: " + totalBlocks + ")");
            
            // Guardar datos inmediatamente después de remover
            PlayerDataPersistence.savePlayerData();
        }
    }
    
    /**
     * Obtiene el número de bloques de protección que tiene un jugador
     * @param playerUUID UUID del jugador
     * @return número de bloques de protección
     */
    public static int getProtectionBlockCount(UUID playerUUID) {
        Set<BlockPos> blocks = playerProtectionBlocks.get(playerUUID);
        return blocks != null ? blocks.size() : 0;
    }
    
    /**
     * Verifica si un jugador puede colocar más bloques de protección
     * @param playerUUID UUID del jugador
     * @return true si puede colocar más bloques
     */
    public static boolean canPlaceMoreBlocks(UUID playerUUID) {
        int currentCount = getProtectionBlockCount(playerUUID);
        return ProtectionBlocksConfig.canPlaceMoreBlocks(currentCount);
    }
    
    /**
     * Obtiene todos los bloques de protección de un jugador
     * @param playerUUID UUID del jugador
     * @return conjunto de posiciones de bloques (puede ser null)
     */
    public static Set<BlockPos> getPlayerProtectionBlocks(UUID playerUUID) {
        return playerProtectionBlocks.get(playerUUID);
    }
    
    /**
     * Verifica si un bloque está registrado como protección de un jugador
     * @param playerUUID UUID del jugador
     * @param blockPos posición del bloque
     * @return true si el bloque está registrado
     */
    public static boolean isPlayerProtectionBlock(UUID playerUUID, BlockPos blockPos) {
        Set<BlockPos> blocks = playerProtectionBlocks.get(playerUUID);
        return blocks != null && blocks.contains(blockPos);
    }
    
    /**
     * Obtiene todos los jugadores que tienen bloques de protección
     * @return conjunto de UUIDs de jugadores
     */
    public static Set<UUID> getAllPlayersWithProtectionBlocks() {
        return new HashSet<>(playerProtectionBlocks.keySet());
    }
    
    /**
     * Limpia todos los datos (útil para reinicios del servidor)
     */
    public static void clearAllData() {
        playerProtectionBlocks.clear();
    }
    
    /**
     * Remueve bloques inválidos de la lista (bloques que ya no existen en el mundo)
     * Se debe llamar periódicamente para mantener la integridad de los datos
     */
    public static void cleanupInvalidBlocks() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        
        playerProtectionBlocks.entrySet().removeIf(entry -> {
            Set<BlockPos> blocks = entry.getValue();
            blocks.removeIf(blockPos -> {
                // Verificar si el bloque todavía existe en todas las dimensiones
                for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                    BlockEntity blockEntity = level.getBlockEntity(blockPos);
                    if (blockEntity instanceof com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity protectionBlock) {
                        // Si el bloque existe y pertenece al jugador, mantenerlo
                        if (entry.getKey().equals(protectionBlock.getOwnerId())) {
                            return false; // No remover
                        }
                    }
                }
                return true; // Remover el bloque porque no existe o no es válido
            });
            
            return blocks.isEmpty(); // Remover la entrada del jugador si no tiene bloques
        });
    }
    
    /**
     * Sincroniza los datos con el estado actual del mundo
     * Útil para la inicialización del servidor
     * Este método carga los datos desde archivos de persistencia
     */
    public static void syncWithWorld() {
        System.out.println("[ProtectionBlocks] Loading player data from persistence files...");
        
        // Cargar datos desde archivos JSON
        PlayerDataPersistence.loadPlayerData();
        
        // Estadísticas finales
        int totalPlayers = playerProtectionBlocks.size();
        int totalBlocks = playerProtectionBlocks.values().stream().mapToInt(Set::size).sum();
        
        System.out.println("[ProtectionBlocks] Data loaded: " + totalPlayers + " players, " + totalBlocks + " protection blocks");
    }
    
    /**
     * Obtiene estadísticas del sistema de protección
     * @return mapa con estadísticas
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", playerProtectionBlocks.size());
        stats.put("totalBlocks", playerProtectionBlocks.values().stream().mapToInt(Set::size).sum());
        stats.put("averageBlocksPerPlayer", 
            playerProtectionBlocks.isEmpty() ? 0 : 
            playerProtectionBlocks.values().stream().mapToInt(Set::size).average().orElse(0));
        return stats;
    }
}