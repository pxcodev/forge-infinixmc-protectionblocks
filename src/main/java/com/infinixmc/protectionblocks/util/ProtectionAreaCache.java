package com.infinixmc.protectionblocks.util;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache eficiente para áreas de protección organizado por chunks
 * Evita la necesidad de hacer bucles costosos para encontrar protecciones
 */
public class ProtectionAreaCache {
    
    // Mapa de chunk -> lista de protecciones que afectan ese chunk
    private static final Map<String, Map<ChunkPos, Set<BlockPos>>> dimensionChunkCache = new ConcurrentHashMap<>();
    
    /**
     * Registra un bloque de protección en el cache
     */
    public static void addProtectionBlock(Level level, BlockPos protectionPos, int range) {
        String dimensionKey = level.dimension().location().toString();
        
        // Obtener o crear el cache para esta dimensión
        Map<ChunkPos, Set<BlockPos>> chunkCache = dimensionChunkCache.computeIfAbsent(
            dimensionKey, 
            k -> new ConcurrentHashMap<>()
        );
        
        // Calcular todos los chunks que son afectados por esta protección
        Set<ChunkPos> affectedChunks = getAffectedChunks(protectionPos, range);
        
        // Agregar la protección a todos los chunks afectados
        for (ChunkPos chunkPos : affectedChunks) {
            chunkCache.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet())
                     .add(protectionPos);
        }
        
        DebugLogger.log("Protección registrada en cache: " + protectionPos + " afecta " + affectedChunks.size() + " chunks");
    }
    
    /**
     * Remueve un bloque de protección del cache
     */
    public static void removeProtectionBlock(Level level, BlockPos protectionPos, int range) {
        String dimensionKey = level.dimension().location().toString();
        Map<ChunkPos, Set<BlockPos>> chunkCache = dimensionChunkCache.get(dimensionKey);
        
        if (chunkCache == null) return;
        
        // Calcular todos los chunks que eran afectados por esta protección
        Set<ChunkPos> affectedChunks = getAffectedChunks(protectionPos, range);
        
        // Remover la protección de todos los chunks afectados
        for (ChunkPos chunkPos : affectedChunks) {
            Set<BlockPos> protections = chunkCache.get(chunkPos);
            if (protections != null) {
                protections.remove(protectionPos);
                // Si el set está vacío, remover la entrada del chunk
                if (protections.isEmpty()) {
                    chunkCache.remove(chunkPos);
                }
            }
        }
        
        DebugLogger.log("Protección removida del cache: " + protectionPos);
    }
    
    /**
     * Busca eficientemente qué protección (si alguna) cubre una posición dada
     * ¡No usa bucles! Solo busca en el chunk correspondiente
     */
    public static ProtectionBlockEntity findProtectionForPosition(Level level, BlockPos targetPos) {
        String dimensionKey = level.dimension().location().toString();
        Map<ChunkPos, Set<BlockPos>> chunkCache = dimensionChunkCache.get(dimensionKey);
        
        if (chunkCache == null) return null;
        
        // Obtener el chunk de la posición objetivo
        ChunkPos targetChunk = new ChunkPos(targetPos);
        Set<BlockPos> protectionsInChunk = chunkCache.get(targetChunk);
        
        if (protectionsInChunk == null || protectionsInChunk.isEmpty()) {
            return null;
        }
        
        // Crear una copia para evitar ConcurrentModificationException
        Set<BlockPos> protectionsToCheck = new HashSet<>(protectionsInChunk);
        
        // Verificar solo las protecciones en este chunk (muy pocas comparado con el mundo entero)
        for (BlockPos protectionPos : protectionsToCheck) {
            // Verificar que el bloque físico aún existe
            if (level.getBlockEntity(protectionPos) instanceof ProtectionBlockEntity protectionBlock) {
                // Verificar que el bloque no ha sido marcado como removido
                if (!protectionBlock.isRemoved() && protectionBlock.isInProtectionRange(targetPos)) {
                    return protectionBlock;
                }
            } else {
                // El bloque ya no existe, remover del cache
                DebugLogger.log("Removiendo protección huérfana del cache: " + protectionPos);
                protectionsInChunk.remove(protectionPos);
                
                // Limpiar de todos los chunks afectados
                cleanupOrphanedProtection(dimensionKey, protectionPos);
            }
        }
        
        return null;
    }
    
    /**
     * Limpia una protección huérfana de todos los chunks
     */
    private static void cleanupOrphanedProtection(String dimensionKey, BlockPos protectionPos) {
        Map<ChunkPos, Set<BlockPos>> chunkCache = dimensionChunkCache.get(dimensionKey);
        if (chunkCache == null) return;
        
        // Remover de todos los chunks (no conocemos el rango original, así que buscamos)
        chunkCache.forEach((chunkPos, protections) -> {
            protections.remove(protectionPos);
        });
    }
    
    /**
     * Calcula todos los chunks que son afectados por una protección
     */
    private static Set<ChunkPos> getAffectedChunks(BlockPos protectionPos, int range) {
        Set<ChunkPos> chunks = new HashSet<>();
        
        // Calcular los límites del área de protección
        int minX = protectionPos.getX() - range;
        int maxX = protectionPos.getX() + range;
        int minZ = protectionPos.getZ() - range;
        int maxZ = protectionPos.getZ() + range;
        
        // Convertir a coordenadas de chunk
        int minChunkX = minX >> 4; // Dividir por 16
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        
        // Agregar todos los chunks en el rango
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        
        return chunks;
    }
    
    /**
     * Limpia el cache para una dimensión
     */
    public static void clearDimension(String dimensionKey) {
        dimensionChunkCache.remove(dimensionKey);
        DebugLogger.log("Cache limpiado para dimensión: " + dimensionKey);
    }
    
    /**
     * Limpia todo el cache
     */
    public static void clearAll() {
        dimensionChunkCache.clear();
        DebugLogger.log("Cache de protecciones completamente limpiado");
    }
    
    /**
     * Estadísticas del cache para debugging
     */
    public static void printCacheStats() {
        for (Map.Entry<String, Map<ChunkPos, Set<BlockPos>>> entry : dimensionChunkCache.entrySet()) {
            String dimension = entry.getKey();
            int chunkCount = entry.getValue().size();
            int totalProtections = entry.getValue().values().stream()
                .mapToInt(Set::size)
                .sum();
            
            DebugLogger.log("Dimensión " + dimension + ": " + chunkCount + " chunks, " + totalProtections + " protecciones");
        }
    }
}