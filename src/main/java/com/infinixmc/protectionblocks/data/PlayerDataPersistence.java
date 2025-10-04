package com.infinixmc.protectionblocks.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Clase para manejar la persistencia de datos de jugadores en archivos JSON
 */
public class PlayerDataPersistence {
    
    private static final String CONFIG_DIR = "config/protectionblocks";
    private static final String PLAYER_DATA_FILE = "player_data.json";
    private static final String BACKUP_SUFFIX = ".backup";
    
    // Flag para controlar si el servidor está cerrándose
    private static volatile boolean serverShuttingDown = false;
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    // Estructura de datos para persistencia
    public static class PlayerData {
        public String playerName;
        public String playerUUID;
        public List<BlockPosition> protectionBlocks;
        public long lastUpdated;
        
        public PlayerData() {
            this.protectionBlocks = new ArrayList<>();
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public PlayerData(String playerName, String playerUUID) {
            this();
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }
    }
    
    public static class BlockPosition {
        public int x, y, z;
        public String dimension;
        
        public BlockPosition() {}
        
        public BlockPosition(BlockPos pos, String dimension) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.dimension = dimension;
        }
        
        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }
    
    /**
     * Asegura que el directorio de configuración existe
     */
    private static void ensureConfigDirectory() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                DebugLogger.log("Directorio de configuración creado: " + CONFIG_DIR);
            }
        } catch (IOException e) {
            DebugLogger.log("Error creando directorio de configuración: " + e.getMessage());
        }
    }
    
    /**
     * Guarda los datos de todos los jugadores en archivo JSON
     */
    public static void savePlayerData() {
        DebugLogger.logSection("GUARDANDO DATOS DE JUGADORES");
        
        // Si el servidor está cerrándose, usar el método especial
        if (serverShuttingDown) {
            savePlayerDataOnShutdown();
            return;
        }
        
        // Verificar que el servidor no esté cerrándose
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isStopped()) {
            DebugLogger.logPersistence("SAVE", false, "Servidor cerrándose, usando guardado especial");
            savePlayerDataOnShutdown();
            return;
        }
        
        ensureConfigDirectory();
        
        try {
            // Obtener datos actuales del PlayerProtectionManager
            Map<String, PlayerData> playerDataMap = new HashMap<>();
            
            for (UUID playerUUID : PlayerProtectionManager.getAllPlayersWithProtectionBlocks()) {
                Set<BlockPos> blocks = PlayerProtectionManager.getPlayerProtectionBlocks(playerUUID);
                if (blocks != null && !blocks.isEmpty()) {
                    PlayerData data = new PlayerData();
                    data.playerUUID = playerUUID.toString();
                    data.playerName = getPlayerNameFromUUID(playerUUID);
                    data.lastUpdated = System.currentTimeMillis();
                    
                    // Agregar todas las posiciones de bloques
                    for (BlockPos pos : blocks) {
                        String dimension = getDimensionForBlock(pos);
                        data.protectionBlocks.add(new BlockPosition(pos, dimension));
                    }
                    
                    playerDataMap.put(playerUUID.toString(), data);
                    DebugLogger.logPlayerData(data.playerName, data.protectionBlocks.size(), ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer());
                }
            }
            
            // Crear backup del archivo existente
            File dataFile = new File(CONFIG_DIR, PLAYER_DATA_FILE);
            if (dataFile.exists()) {
                File backupFile = new File(CONFIG_DIR, PLAYER_DATA_FILE + BACKUP_SUFFIX);
                Files.copy(dataFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                DebugLogger.log("Backup creado: " + backupFile.getName());
            }
            
            // Guardar datos actualizados
            try (FileWriter writer = new FileWriter(dataFile)) {
                GSON.toJson(playerDataMap, writer);
                int totalBlocks = playerDataMap.values().stream().mapToInt(d -> d.protectionBlocks.size()).sum();
                DebugLogger.logPersistence("SAVE", true, playerDataMap.size() + " jugadores, " + totalBlocks + " bloques");
            }
            
        } catch (IOException e) {
            DebugLogger.logError("Error guardando datos de jugadores", e);
            DebugLogger.logPersistence("SAVE", false, e.getMessage());
        }
    }
    
    /**
     * Carga los datos de jugadores desde archivo JSON
     */
    public static void loadPlayerData() {
        DebugLogger.logSection("CARGANDO DATOS DE JUGADORES");
        ensureConfigDirectory();
        
        File dataFile = new File(CONFIG_DIR, PLAYER_DATA_FILE);
        if (!dataFile.exists()) {
            DebugLogger.logPersistence("LOAD", false, "Archivo no existe, iniciando con datos vacíos");
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, PlayerData>>(){}.getType();
            Map<String, PlayerData> playerDataMap = GSON.fromJson(reader, type);
            
            if (playerDataMap == null) {
                DebugLogger.logPersistence("LOAD", false, "Archivo vacío o corrupto");
                return;
            }
            
            // Limpiar datos existentes
            PlayerProtectionManager.clearAllData();
            DebugLogger.log("Datos existentes limpiados");
            
            int totalBlocks = 0;
            int validBlocks = 0;
            int playersLoaded = 0;
            
            // Restaurar datos en PlayerProtectionManager
            for (Map.Entry<String, PlayerData> entry : playerDataMap.entrySet()) {
                PlayerData data = entry.getValue();
                UUID playerUUID = UUID.fromString(data.playerUUID);
                int playerValidBlocks = 0;
                
                for (BlockPosition blockPos : data.protectionBlocks) {
                    totalBlocks++;
                    BlockPos pos = blockPos.toBlockPos();
                    
                    // Verificar que el bloque realmente existe en el mundo
                    if (isValidProtectionBlock(pos, blockPos.dimension, playerUUID)) {
                        PlayerProtectionManager.addProtectionBlock(playerUUID, pos);
                        validBlocks++;
                        playerValidBlocks++;
                    } else {
                        DebugLogger.log("Bloque inválido removido: " + pos + " (" + data.playerName + ")");
                    }
                }
                
                if (playerValidBlocks > 0) {
                    playersLoaded++;
                    DebugLogger.logPlayerData(data.playerName, playerValidBlocks, ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer());
                }
            }
            
            DebugLogger.logPersistence("LOAD", true, playersLoaded + " jugadores, " + validBlocks + "/" + totalBlocks + " bloques válidos");
            
            DebugLogger.log("Datos de jugadores cargados: " + playerDataMap.size() + " jugadores, " + 
                validBlocks + "/" + totalBlocks + " bloques válidos restaurados");
                
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            DebugLogger.log("Error cargando datos de jugadores: " + e.getMessage());
            // Intentar cargar backup
            loadBackupData();
        }
    }
    
    /**
     * Intenta cargar datos desde el archivo de backup
     */
    private static void loadBackupData() {
        File backupFile = new File(CONFIG_DIR, PLAYER_DATA_FILE + BACKUP_SUFFIX);
        if (!backupFile.exists()) {
            DebugLogger.log("No hay archivo de backup disponible");
            return;
        }
        
        try (FileReader reader = new FileReader(backupFile)) {
            Type type = new TypeToken<Map<String, PlayerData>>(){}.getType();
            Map<String, PlayerData> playerDataMap = GSON.fromJson(reader, type);
            
            if (playerDataMap != null) {
                DebugLogger.log("Cargando datos desde backup...");
                // Procesar datos del backup de la misma manera
                PlayerProtectionManager.clearAllData();
                
                for (Map.Entry<String, PlayerData> entry : playerDataMap.entrySet()) {
                    PlayerData data = entry.getValue();
                    UUID playerUUID = UUID.fromString(data.playerUUID);
                    
                    for (BlockPosition blockPos : data.protectionBlocks) {
                        BlockPos pos = blockPos.toBlockPos();
                        if (isValidProtectionBlock(pos, blockPos.dimension, playerUUID)) {
                            PlayerProtectionManager.addProtectionBlock(playerUUID, pos);
                        }
                    }
                }
                
                DebugLogger.log("Datos restaurados desde backup exitosamente");
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            DebugLogger.log("Error cargando backup: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si un bloque de protección es válido en el mundo
     */
    private static boolean isValidProtectionBlock(BlockPos pos, String dimension, UUID expectedOwner) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return false;
        }
        
        try {
            for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                String levelDimension = level.dimension().location().toString();
                if (levelDimension.equals(dimension)) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity protectionBlock) {
                        UUID ownerId = protectionBlock.getOwnerId();
                        return expectedOwner.equals(ownerId);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            DebugLogger.log("Error verificando bloque en " + pos + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Obtiene el nombre de un jugador desde su UUID
     */
    private static String getPlayerNameFromUUID(UUID playerUUID) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            var player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "Unknown";
    }
    
    /**
     * Obtiene la dimensión donde está un bloque
     */
    private static String getDimensionForBlock(BlockPos pos) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.isStopped()) {
                for (ServerLevel level : server.getAllLevels()) {
                    try {
                        if (level.getBlockEntity(pos) instanceof com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity) {
                            return level.dimension().location().toString();
                        }
                    } catch (Exception e) {
                        // Si hay error accediendo a un nivel específico, continuar con el siguiente
                        DebugLogger.log("Error accediendo al nivel para bloque " + pos + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Si hay cualquier error, log y usar dimensión por defecto
            DebugLogger.log("Error obteniendo dimensión para bloque " + pos + ": " + e.getMessage());
        }
        return "minecraft:overworld"; // Default
    }
    
    /**
     * Guarda datos automáticamente cada cierto intervalo
     */
    public static void scheduleAutoSave() {
        // Implementar un sistema de auto-guardado si es necesario
        // Por ahora, guardaremos en eventos específicos
    }
    
    /**
     * Marca que el servidor está empezando a cerrar
     */
    public static void setServerShuttingDown() {
        serverShuttingDown = true;
        DebugLogger.log("Servidor marcado como cerrándose");
    }
    
    /**
     * Resetea el flag de cierre (para cuando el servidor se reinicia)
     */
    public static void resetServerState() {
        serverShuttingDown = false;
        DebugLogger.log("Estado del servidor reseteado");
    }
    
    /**
     * Guardado rápido durante el cierre que no accede al mundo
     */
    public static void savePlayerDataOnShutdown() {
        DebugLogger.logSection("GUARDADO RÁPIDO AL CERRAR SERVIDOR");
        ensureConfigDirectory();
        
        try {
            Map<String, PlayerData> playerDataMap = new HashMap<>();
            
            // Solo usar los datos en memoria sin acceder al mundo
            for (UUID playerUUID : PlayerProtectionManager.getAllPlayersWithProtectionBlocks()) {
                Set<BlockPos> blocks = PlayerProtectionManager.getPlayerProtectionBlocks(playerUUID);
                if (blocks != null && !blocks.isEmpty()) {
                    PlayerData data = new PlayerData();
                    data.playerUUID = playerUUID.toString();
                    data.playerName = getPlayerNameFromUUID(playerUUID);
                    data.lastUpdated = System.currentTimeMillis();
                    
                    // Agregar posiciones sin verificar dimensión (usar overworld por defecto)
                    for (BlockPos pos : blocks) {
                        data.protectionBlocks.add(new BlockPosition(pos, "minecraft:overworld"));
                    }
                    
                    playerDataMap.put(playerUUID.toString(), data);
                    DebugLogger.logPlayerData(data.playerName, data.protectionBlocks.size(), ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer());
                }
            }
            
            // Crear backup y guardar
            File dataFile = new File(CONFIG_DIR, PLAYER_DATA_FILE);
            if (dataFile.exists()) {
                File backupFile = new File(CONFIG_DIR, PLAYER_DATA_FILE + BACKUP_SUFFIX);
                Files.copy(dataFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                DebugLogger.log("Backup creado: " + backupFile.getName());
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                GSON.toJson(playerDataMap, writer);
                int totalBlocks = playerDataMap.values().stream().mapToInt(d -> d.protectionBlocks.size()).sum();
                DebugLogger.logPersistence("SHUTDOWN_SAVE", true, playerDataMap.size() + " jugadores, " + totalBlocks + " bloques");
            }
            
        } catch (IOException e) {
            DebugLogger.logError("Error en guardado de cierre", e);
            DebugLogger.logPersistence("SHUTDOWN_SAVE", false, e.getMessage());
        }
    }
    
    /**
     * Limpia archivos de datos antiguos
     */
    public static void cleanupOldData() {
        // Implementar limpieza de archivos antiguos si es necesario
    }
}