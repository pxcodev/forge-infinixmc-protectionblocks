package com.infinixmc.protectionblocks.events;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.blocks.ProtectionBlock;
import com.infinixmc.protectionblocks.util.ProtectionManager;
import com.infinixmc.protectionblocks.util.ProtectionAreaCache;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import com.infinixmc.protectionblocks.network.packets.OpenProtectionGUIPacket;
import com.infinixmc.protectionblocks.util.PermissionType;
import com.infinixmc.protectionblocks.util.DebugLogger;
import com.infinixmc.protectionblocks.data.PlayerProtectionManager;
import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.NetworkDirection;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public class ProtectionEventHandler {
    
    // Mapa para rastrear en qué área de protección está cada jugador
    private static final Map<UUID, BlockPos> playerLastProtectionArea = new HashMap<>();
    // Mapa para rastrear la última posición verificada (para detectar movimiento significativo)
    private static final Map<UUID, BlockPos> playerLastCheckedPosition = new HashMap<>();
    // Mapa para rastrear si ya se hizo la reconstrucción del cache para un jugador
    private static final Map<UUID, Boolean> playerCacheRebuilt = new HashMap<>();
    // Distancia mínima para activar verificación
    private static final int MOVEMENT_CHECK_DISTANCE = 5;

    /**
     * Método helper para enviar títulos de forma segura usando paquetes directos
     */
    private static void sendTitle(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        if (title != null) player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /**
     * Reconstruir el cache de forma segura para un área específica alrededor del jugador
     * Solo busca bloques de protección en un radio limitado
     */
    private static void rebuildCacheAroundPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (playerCacheRebuilt.containsKey(playerId)) {
            return; // Ya se reconstruyó para este jugador
        }

        try {
            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();
            int searchRadius = 50; // Radio de búsqueda limitado
            
            DebugLogger.log("Reconstruyendo cache alrededor del jugador " + player.getName().getString());
            
            // Buscar bloques de protección en un área limitada
            for (int x = playerPos.getX() - searchRadius; x <= playerPos.getX() + searchRadius; x += 16) {
                for (int z = playerPos.getZ() - searchRadius; z <= playerPos.getZ() + searchRadius; z += 16) {
                    // Solo verificar posiciones de chunks para optimizar
                    var chunk = level.getChunkAt(new BlockPos(x, playerPos.getY(), z));
                    if (chunk != null) {
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof ProtectionBlockEntity pbe) {
                                // VALIDAR que el bloque físico realmente existe
                                BlockPos bePos = pbe.getBlockPos();
                                BlockState state = level.getBlockState(bePos);
                                
                                // Solo agregar al cache si el bloque es realmente un bloque de protección
                                if (state.getBlock() instanceof com.infinixmc.protectionblocks.blocks.ProtectionBlock && !pbe.isRemoved()) {
                                    ProtectionAreaCache.addProtectionBlock(level, bePos, pbe.getProtectionRange());
                                    DebugLogger.log("Bloque de protección válido encontrado en " + bePos);
                                } else {
                                    // Bloque huérfano detectado - limpiar
                                    DebugLogger.log("Bloque de protección huérfano detectado y limpiado en " + bePos);
                                    level.removeBlockEntity(bePos);
                                }
                            }
                        }
                    }
                }
            }
            
            playerCacheRebuilt.put(playerId, true);
            DebugLogger.log("Cache reconstruido para jugador " + player.getName().getString());
            
        } catch (Exception e) {
            DebugLogger.log("Error reconstruyendo cache para jugador " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    // **ÚNICO EVENTO PARA CONSTRUCCIÓN** - Con máxima prioridad para ejecutarse primero
        @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && event.getLevel() instanceof Level level && !level.isClientSide) {
            BlockPos pos = event.getPos();
            
            // Verificar si se está colocando un bloque de protección
            if (event.getPlacedBlock().getBlock() instanceof ProtectionBlock) {
                // Verificar límite de bloques por jugador PRIMERO
                if (!PlayerProtectionManager.canPlaceMoreBlocks(player.getUUID())) {
                    event.setCanceled(true);
                    int currentCount = PlayerProtectionManager.getProtectionBlockCount(player.getUUID());
                    int maxAllowed = ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer();
                    
                    if (maxAllowed == -1) {
                        player.sendSystemMessage(Component.literal("§cError interno: límite de bloques mal configurado"));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.protectionblocks.limit_reached", currentCount, maxAllowed));
                    }
                    return;
                }
                
                // Obtener el rango de protección basado en el tipo de bloque y la configuración actual
                BlockState placedBlockState = event.getPlacedBlock();
                String blockName = placedBlockState.getBlock().toString();
                
                int protectionRange;
                if (blockName.contains("basic")) {
                    protectionRange = ProtectionBlocksConfig.getBasicProtectionRange();
                } else if (blockName.contains("advanced")) {
                    protectionRange = ProtectionBlocksConfig.getAdvancedProtectionRange();
                } else if (blockName.contains("superior")) {
                    protectionRange = ProtectionBlocksConfig.getSuperiorProtectionRange();
                } else if (blockName.contains("elite")) {
                    protectionRange = ProtectionBlocksConfig.getEliteProtectionRange();
                } else if (blockName.contains("master")) {
                    protectionRange = ProtectionBlocksConfig.getMasterProtectionRange();
                } else {
                    protectionRange = ProtectionBlocksConfig.getBasicProtectionRange(); // Por defecto usar básico
                }
                
                DebugLogger.log("Colocando bloque " + blockName + " con rango dinámico: " + protectionRange);
                
                // Verificar si hay conflictos con otras protecciones
                ProtectionManager.ConflictInfo conflict = ProtectionManager.getConflictingProtection(level, pos, protectionRange);
                if (conflict != null) {
                    event.setCanceled(true);
                    
                    // Mensajes informativos sobre el conflicto
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.cannot_place_here"));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.protection_too_close", conflict.conflictingProtection.getOwnerName()));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.current_distance", String.format("%.1f", conflict.currentDistance)));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.minimum_distance", String.format("%.1f", conflict.minimumDistance)));
                    
                    // Calcular cuántos bloques más necesita alejarse
                    double blocksNeeded = conflict.minimumDistance - conflict.currentDistance;
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.move_away", String.format("%.1f", blocksNeeded)));
                    
                    return;
                }
                
                // Establecer automáticamente el propietario del bloque recién colocado
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ProtectionBlockEntity protectionBlockEntity) {
                    protectionBlockEntity.setOwner(player);
                    
                    // Registrar el bloque en el gestor de protecciones del jugador
                    PlayerProtectionManager.addProtectionBlock(player.getUUID(), pos);
                    
                    // IMPORTANTE: Registrar en el cache eficiente
                    ProtectionAreaCache.addProtectionBlock(level, pos, protectionRange);
                    
                    int currentCount = PlayerProtectionManager.getProtectionBlockCount(player.getUUID());
                    int maxAllowed = ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer();
                    
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.placed_successfully"));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.protection_range", protectionRange));
                    
                    if (maxAllowed == -1) {
                        player.sendSystemMessage(Component.translatable("message.protectionblocks.blocks_count_unlimited", currentCount));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.protectionblocks.blocks_count", currentCount, maxAllowed));
                    }
                    
                    DebugLogger.log("Protection block placed by " + player.getName().getString() + " at " + pos + " with range " + protectionRange + ". Player now has " + currentCount + " blocks.");
                }
                return; // No necesitamos verificar más si es un bloque de protección
            }
            
            // Primero, forzar limpieza de protecciones huérfanas
            ProtectionManager.forceCleanupOrphanedProtections(level, pos);
            
            // Luego verificar protección normalmente para otros bloques
            ProtectionBlockEntity protectionBlock = ProtectionManager.getProtectingBlock(level, pos);
            
            if (protectionBlock != null && !protectionBlock.hasPermission(player, PermissionType.BUILD)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.translatable("message.protectionblocks.area_protected_by", protectionBlock.getOwnerName()));
            }
        }
    }

    // **ÚNICO EVENTO PARA ROMPER BLOQUES**
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();

        if (player == null || level.isClientSide) return;

        // Verificar si el bloque que se está rompiendo es un bloque de protección
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ProtectionBlockEntity protectionBlockEntity) {
            UUID ownerId = protectionBlockEntity.getOwnerId();
            
            // Verificar si el jugador es administrador (operador del servidor)
            boolean isAdmin = player.hasPermissions(2);
            
            // El propietario o un administrador pueden romper el bloque de protección
            if (ownerId != null && (ownerId.equals(player.getUUID()) || isAdmin)) {
                // Limpiar visualización si está activa antes de remover el bloque
                if (protectionBlockEntity.isVisualizationActive()) {
                    ProtectionManager.hideProtectionArea(player);
                    DebugLogger.log("Visualización limpiada al romper bloque de protección en " + pos);
                }
                
                // Remover el bloque del gestor de protecciones del jugador propietario (no del admin)
                PlayerProtectionManager.removeProtectionBlock(ownerId, pos);
                
                // IMPORTANTE: Remover del cache eficiente
                int range = protectionBlockEntity.getProtectionRange();
                ProtectionAreaCache.removeProtectionBlock(level, pos, range);
                
                int remainingCount = PlayerProtectionManager.getProtectionBlockCount(ownerId);
                
                if (isAdmin && !ownerId.equals(player.getUUID())) {
                    // Un administrador está rompiendo el bloque de otro jugador
                    String ownerName = protectionBlockEntity.getOwnerName();
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.admin_removed_block", ownerName != null ? ownerName : "Desconocido"));
                    DebugLogger.log("Protection block removed by admin " + player.getName().getString() + " at " + pos + " (owner: " + ownerName + ")");
                } else {
                    // El propietario está rompiendo su propio bloque
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.block_removed"));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.blocks_remaining", remainingCount));
                    DebugLogger.log("Protection block removed by " + player.getName().getString() + " at " + pos + ". Player now has " + remainingCount + " blocks.");
                }
                return; // Permitir que se rompa el bloque
            } else if (ownerId != null) {
                // No es el propietario ni administrador, cancelar la acción
                event.setCanceled(true);
                String ownerName = protectionBlockEntity.getOwnerName();
                player.sendSystemMessage(Component.translatable("message.protectionblocks.cannot_break_others_block", ownerName != null ? ownerName : "Desconocido"));
                return;
            }
        }

        // Verificar si el bloque está protegido
        ProtectionBlockEntity protectionBlock = ProtectionManager.getProtectingBlock(level, pos);
        if (protectionBlock != null && !protectionBlock.hasPermission(player, PermissionType.BREAK)) {
            // Verificar si el jugador es administrador
            boolean isAdmin = player.hasPermissions(2);
            
            if (!isAdmin) {
                // No es administrador, cancelar el evento
                event.setCanceled(true);
                player.sendSystemMessage(Component.translatable("message.protectionblocks.no_break_permission", protectionBlock.getOwnerName()));
            }
            // Si es administrador, permitir que rompa el bloque sin cancelar el evento
        }
    }

    // **ÚNICO EVENTO PARA CLICK DERECHO** - Solo para abrir GUI de bloques de protección
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // SOLO manejar clicks directos en bloques de protección para abrir GUI
        if (state.getBlock() instanceof ProtectionBlock) {
            event.setCanceled(true);
            
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    if (protectionBlock.isOwner(player)) {
                        NetworkHandler.INSTANCE.sendTo(
                            new OpenProtectionGUIPacket(pos),
                            ((ServerPlayer) player).connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                        );
                    } else {
                        player.sendSystemMessage(Component.translatable("message.protectionblocks.only_owner_gui"));
                    }
                }
            }
            return;
        }

        // Para otros tipos de interacción (cofres, hornos, etc.) verificar permisos
        if (!level.isClientSide) {
            ProtectionBlockEntity protectionBlock = ProtectionManager.getProtectingBlock(level, pos);
            if (protectionBlock != null) {
                // Si el jugador tiene un bloque en la mano y permiso BUILD, permitir la interacción para colocar
                boolean hasBlockInHand = player.getMainHandItem().getItem() instanceof BlockItem;
                boolean canBuild = protectionBlock.hasPermission(player, PermissionType.BUILD);
                boolean canInteract = protectionBlock.hasPermission(player, PermissionType.INTERACT);
                
                if (!canInteract && !(hasBlockInHand && canBuild)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.area_protected_by", protectionBlock.getOwnerName()));
                }
            }
        }
    }

    // **ÚNICO EVENTO PARA CLICK IZQUIERDO** - Solo para atacar/romper
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        Level level = event.getLevel();

        if (level.isClientSide) return;

        // Verificar si se está atacando un bloque protegido
        ProtectionBlockEntity protectionBlock = ProtectionManager.getProtectingBlock(level, pos);
        if (protectionBlock != null && !protectionBlock.hasPermission(player, PermissionType.BREAK)) {
            // Verificar si el jugador es administrador
            boolean isAdmin = player.hasPermissions(2);
            
            if (!isAdmin) {
                // No es administrador, cancelar el evento
                event.setCanceled(true);
                player.sendSystemMessage(Component.translatable("message.protectionblocks.no_attack_permission", protectionBlock.getOwnerName()));
            }
            // Si es administrador, permitir que ataque el bloque sin cancelar el evento
        }
    }
    
    // Sistema equilibrado de detección de entrada a áreas protegidas
    // Combina eventos nativos + verificación ligera de movimiento
    
    /**
     * Reconstruir el cache cuando se cargan chunks para que los bloques de protección
     * estén indexados inmediatamente después del reinicio del servidor
     * TEMPORALMENTE DESACTIVADO para evitar problemas en la carga inicial
     */
    // @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ProtectionBlockEntity pbe) {
                ProtectionAreaCache.addProtectionBlock(level, pbe.getBlockPos(), pbe.getProtectionRange());
            }
        }
    }

    /**
     * Limpiar el cache cuando se descargan chunks
     * TEMPORALMENTE DESACTIVADO para evitar problemas en la carga inicial
     */
    // @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ProtectionBlockEntity pbe) {
                ProtectionAreaCache.removeProtectionBlock(level, pbe.getBlockPos(), pbe.getProtectionRange());
            }
        }
    }

    /**
     * Limpiar cache de la dimensión cuando se carga el nivel
     * SIMPLIFICADO para evitar problemas durante la carga inicial
     */
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        
        // Solo limpiar cache de la dimensión sin procesamiento adicional
        String dimensionKey = level.dimension().location().toString();
        ProtectionAreaCache.clearDimension(dimensionKey);
        
        DebugLogger.log("Cache de protección limpiado para dimensión: " + dimensionKey);
    }
    
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Limpiar estado cuando el jugador cambia de dimensión
        UUID playerId = event.getEntity().getUUID();
        playerLastProtectionArea.remove(playerId);
        playerLastCheckedPosition.remove(playerId);
        playerCacheRebuilt.remove(playerId); // Forzar reconstrucción en nueva dimensión
        checkPlayerProtectionArea((ServerPlayer) event.getEntity());
    }
    
    @SubscribeEvent  
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Verificar área de protección cuando el jugador se conecta
        UUID playerId = event.getEntity().getUUID();
        ServerPlayer player = (ServerPlayer) event.getEntity();
        
        // Limpiar estado previo
        playerLastCheckedPosition.remove(playerId);
        playerLastProtectionArea.remove(playerId);
        playerCacheRebuilt.remove(playerId); // Forzar reconstrucción del cache
        
        // Retraso para asegurar que el servidor esté completamente cargado
        player.getServer().execute(() -> player.getServer().execute(() -> player.getServer().execute(() -> {
            try {
                // Primero reconstruir el cache alrededor del jugador
                rebuildCacheAroundPlayer(player);
                
                // Verificar si está en una zona pero SIN mostrar mensaje
                ProtectionBlockEntity prot = ProtectionManager.getProtectingBlock(player.level(), player.blockPosition());
                if (prot != null) {
                    String zoneName = prot.getZoneName();

                    // Solo marcar el área actual sin mostrar mensaje de entrada
                    playerLastProtectionArea.put(playerId, prot.getBlockPos());
                    playerLastCheckedPosition.put(playerId, player.blockPosition());
                    
                    DebugLogger.log("Jugador " + player.getName().getString() + " se conectó dentro de zona: " + zoneName + " (sin mensaje)");
                } else {
                    DebugLogger.log("Jugador " + player.getName().getString() + " se conectó fuera de cualquier zona protegida");
                }
            } catch (Exception e) {
                DebugLogger.log("Error al verificar zona al conectar jugador " + player.getName().getString() + ": " + e.getMessage());
            }
        })));
    }
    
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Limpiar datos cuando el jugador se desconecta
        UUID playerId = event.getEntity().getUUID();
        playerLastProtectionArea.remove(playerId);
        playerLastCheckedPosition.remove(playerId);
        playerCacheRebuilt.remove(playerId); // Limpiar estado de cache
    }
    
    // Verificación ligera de movimiento - solo cuando se mueve distancia significativa
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.player;
        UUID playerId = player.getUUID();
        BlockPos currentPos = player.blockPosition();
        
        // Verificar solo si el jugador se movió una distancia significativa
        BlockPos lastCheckedPos = playerLastCheckedPosition.get(playerId);
        if (lastCheckedPos == null || currentPos.distManhattan(lastCheckedPos) >= MOVEMENT_CHECK_DISTANCE) {
            
            // Si es la primera vez que se mueve después de conectarse, reconstruir cache
            if (!playerCacheRebuilt.containsKey(playerId)) {
                rebuildCacheAroundPlayer(player);
            }
            
            // Actualizar la posición verificada y hacer la verificación
            playerLastCheckedPosition.put(playerId, currentPos);
            checkPlayerProtectionArea(player);
        }
    }
    
    /**
     * Método súper optimizado para verificar si un jugador está en un área protegida
     * Usa el cache eficiente por chunks - NO bucles costosos
     */
    private void checkPlayerProtectionArea(ServerPlayer player) {
        UUID playerId = player.getUUID();
        BlockPos currentPos = player.blockPosition();
        
        // Buscar protección usando el cache eficiente
        ProtectionBlockEntity currentProtection = ProtectionManager.getProtectingBlock(player.level(), currentPos);
        BlockPos currentProtectionPos = currentProtection != null ? currentProtection.getBlockPos() : null;
        
        // Verificar si el jugador cambió de área de protección
        BlockPos lastProtectionPos = playerLastProtectionArea.get(playerId);
        
        if (currentProtectionPos != null && !currentProtectionPos.equals(lastProtectionPos)) {
            // El jugador entró a una nueva área protegida
            String zoneName = currentProtection.getZoneName();

            // Solo mostrar el nombre de la zona en el centro (title)
            Component bigCenter = Component.literal(zoneName.isEmpty() ? 
                Component.translatable("message.protectionblocks.protected_zone").getString() : zoneName)
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true));
            sendTitle(player, bigCenter, null, 10, 40, 10);
            
            // Actualizar el área actual del jugador
            playerLastProtectionArea.put(playerId, currentProtectionPos);
        } else if (currentProtectionPos == null && lastProtectionPos != null) {
            // El jugador salió del área protegida - no mostrar mensaje
            playerLastProtectionArea.remove(playerId);
        } else if (currentProtectionPos != null && lastProtectionPos == null) {
            // Caso especial: el jugador ya estaba en una zona al detectarla por primera vez
            String zoneName = currentProtection.getZoneName();

            // Solo mostrar el nombre de la zona en el centro (title)
            Component bigCenter = Component.literal(zoneName.isEmpty() ? 
                Component.translatable("message.protectionblocks.protected_zone").getString() : zoneName)
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true));
            sendTitle(player, bigCenter, null, 10, 40, 10);
            
            playerLastProtectionArea.put(playerId, currentProtectionPos);
        }
    }
}