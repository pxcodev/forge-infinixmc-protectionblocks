package com.infinixmc.protectionblocks.util;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ProtectionManager {
    private static final Map<UUID, Set<BlockPos>> playerVisualizationBlocks = new HashMap<>();

    /**
     * Busca si existe un bloque de protección que cubra la posición dada
     */
    public static ProtectionBlockEntity getProtectingBlock(Level level, BlockPos targetPos) {
        // Buscar en un área amplia alrededor de la posición objetivo
        int searchRadius = 25; // Radio máximo de protección
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = targetPos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    
                    if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                        if (protectionBlock.isInProtectionRange(targetPos)) {
                            return protectionBlock;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Muestra el área de protección al jugador usando bloques temporales
     */
    public static void showProtectionArea(Player player, BlockPos centerPos, int range) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        hideProtectionArea(player); // Limpiar visualización anterior

        Set<BlockPos> visualizationBlocks = new HashSet<>();
        int halfRange = range / 2;

        // Crear el contorno del área de protección
        for (int x = -halfRange; x <= halfRange; x++) {
            for (int z = -halfRange; z <= halfRange; z++) {
                for (int y = -halfRange; y <= halfRange; y++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    
                    // Solo mostrar los bordes del área
                    boolean isEdge = (Math.abs(x) == halfRange || Math.abs(z) == halfRange || Math.abs(y) == halfRange);
                    
                    if (isEdge && player.level().getBlockState(pos).isAir()) {
                        // Enviar bloque temporal de vidrio al cliente
                        BlockState glassState = Blocks.GLASS.defaultBlockState();
                        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, glassState));
                        visualizationBlocks.add(pos);
                    }
                }
            }
        }

        playerVisualizationBlocks.put(player.getUUID(), visualizationBlocks);
    }

    /**
     * Oculta el área de protección restaurando los bloques originales
     */
    public static void hideProtectionArea(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Set<BlockPos> visualizationBlocks = playerVisualizationBlocks.get(player.getUUID());
        if (visualizationBlocks != null) {
            for (BlockPos pos : visualizationBlocks) {
                // Restaurar el bloque original
                BlockState originalState = player.level().getBlockState(pos);
                serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, originalState));
            }
            playerVisualizationBlocks.remove(player.getUUID());
        }
    }

    /**
     * Limpia la visualización cuando el jugador se desconecta
     */
    public static void cleanupPlayerVisualization(UUID playerId) {
        playerVisualizationBlocks.remove(playerId);
    }

    /**
     * Clase interna para información de conflictos entre protecciones
     */
    public static class ConflictInfo {
        public final ProtectionBlockEntity conflictingProtection;
        public final double currentDistance;
        public final double minimumDistance;
        private final String message;

        public ConflictInfo(ProtectionBlockEntity conflictingBlock, double currentDistance, double minimumDistance, String message) {
            this.conflictingProtection = conflictingBlock;
            this.currentDistance = currentDistance;
            this.minimumDistance = minimumDistance;
            this.message = message;
        }

        // Constructor alternativo simple
        public ConflictInfo(ProtectionBlockEntity conflictingBlock, String message) {
            this.conflictingProtection = conflictingBlock;
            this.currentDistance = 0;
            this.minimumDistance = 0;
            this.message = message;
        }

        public ProtectionBlockEntity getConflictingBlock() {
            return conflictingProtection;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Verifica si hay conflictos con otras protecciones en el área
     */
    public static ConflictInfo getConflictingProtection(Level level, BlockPos newPos, int newRange) {
        int searchRadius = 50; // Radio de búsqueda amplio
        int halfNewRange = newRange / 2;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = newPos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);

                    if (blockEntity instanceof ProtectionBlockEntity existingBlock) {
                        // No comparar consigo mismo
                        if (checkPos.equals(newPos)) continue;

                        int existingRange = existingBlock.getProtectionRange();
                        int halfExistingRange = existingRange / 2;

                        // Calcular la distancia real entre los centros
                        double currentDistance = Math.sqrt(checkPos.distSqr(newPos));
                        
                        // La distancia mínima es la suma de los radios
                        double minimumDistance = halfNewRange + halfExistingRange;

                        // Verificar si las áreas se superponen
                        boolean xOverlap = Math.abs(newPos.getX() - checkPos.getX()) < (halfNewRange + halfExistingRange);
                        boolean yOverlap = Math.abs(newPos.getY() - checkPos.getY()) < (halfNewRange + halfExistingRange);
                        boolean zOverlap = Math.abs(newPos.getZ() - checkPos.getZ()) < (halfNewRange + halfExistingRange);

                        if (xOverlap && yOverlap && zOverlap) {
                            String message = String.format("Conflicto con protección de %s en [%d, %d, %d]",
                                    existingBlock.getOwnerName(),
                                    checkPos.getX(), checkPos.getY(), checkPos.getZ());
                            return new ConflictInfo(existingBlock, currentDistance, minimumDistance, message);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Fuerza la limpieza de protecciones huérfanas (sin bloque físico)
     */
    public static void forceCleanupOrphanedProtections(Level level, BlockPos pos) {
        // Buscar en un área alrededor de la posición
        int searchRadius = 30;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);

                    if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                        // Verificar si el bloque físico aún existe
                        BlockState state = level.getBlockState(checkPos);
                        if (state.isAir() || !state.hasBlockEntity()) {
                            // Remover la entidad de bloque huérfana
                            level.removeBlockEntity(checkPos);
                        }
                    }
                }
            }
        }
    }

    /**
     * Limpia la visualización del bloque de protección
     */
    public static void cleanupProtectionBlockVisualization(Level level, BlockPos pos) {
        // Buscar todos los jugadores que puedan tener visualización activa
        for (UUID playerId : new HashSet<>(playerVisualizationBlocks.keySet())) {
            Set<BlockPos> visualBlocks = playerVisualizationBlocks.get(playerId);
            if (visualBlocks != null && !visualBlocks.isEmpty()) {
                // Verificar si alguno de los bloques de visualización está cerca de la posición
                boolean isNearby = visualBlocks.stream()
                        .anyMatch(vPos -> vPos.distSqr(pos) < 1000); // Distancia arbitraria

                if (isNearby) {
                    playerVisualizationBlocks.remove(playerId);
                }
            }
        }
    }
}