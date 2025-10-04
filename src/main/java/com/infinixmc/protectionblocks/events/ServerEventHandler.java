package com.infinixmc.protectionblocks.events;

import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.config.ServerConfig;
import com.infinixmc.protectionblocks.data.PlayerProtectionManager;
import com.infinixmc.protectionblocks.data.PlayerDataPersistence;
import com.infinixmc.protectionblocks.util.DebugLogger;
import com.infinixmc.protectionblocks.util.ProtectionAreaCache;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber
public class ServerEventHandler {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        DebugLogger.log("Server starting - Checking configuration...");
        
        // Resetear el estado del servidor
        PlayerDataPersistence.resetServerState();
        
        // Forzar registro de configuración si no está registrada
        ProtectionBlocksConfig.register();
        
        // Forzar acceso a la configuración para generar el archivo
        try {
            int maxBlocks = ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer();
            DebugLogger.log("Configuration verified - Player block limit: " + 
                (maxBlocks == -1 ? "Unlimited" : String.valueOf(maxBlocks)));
        } catch (Exception e) {
            DebugLogger.log("Error accessing configuration: " + e.getMessage());
        }
        
        // Sincronizar los datos de protección con el mundo
        PlayerProtectionManager.syncWithWorld();
        
        // Inicializar el cache de áreas de protección
        DebugLogger.log("Initializing protection cache...");
        ProtectionAreaCache.clearAll(); // Limpiar cache anterior
        
        // Agregar hook de shutdown para guardar datos
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DebugLogger.log("Emergency save on JVM shutdown...");
            PlayerDataPersistence.savePlayerData();
        }));
        
        DebugLogger.log("Protection system initialized successfully.");
    }
    
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DebugLogger.log("Server stopping - Marking state and saving data...");
        
        // Limpiar el cache de protecciones
        ProtectionAreaCache.clearAll();
        
        // Marcar que el servidor se está cerrando
        PlayerDataPersistence.setServerShuttingDown();
        
        // Guardar todos los datos usando el método especial para cierre
        PlayerDataPersistence.savePlayerDataOnShutdown();
        
        DebugLogger.log("Player data saved correctly on shutdown.");
    }
    
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals("protectionblocks")) {
            DebugLogger.log("Protection Blocks configuration loaded: " + event.getConfig().getFileName());
            
            // Verificar que los valores se cargaron correctamente
            try {
                int maxBlocks = ServerConfig.MAX_PROTECTION_BLOCKS_PER_PLAYER.get();
                DebugLogger.log("Configuration loaded successfully - Limit: " + maxBlocks);
            } catch (Exception e) {
                DebugLogger.log("Error verifying loaded configuration: " + e.getMessage());
            }
        }
    }
    
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals("protectionblocks")) {
            DebugLogger.log("Configuración de Protection Blocks recargada: " + event.getConfig().getFileName());
            
            // Log de nueva configuración
            try {
                int maxBlocks = ProtectionBlocksConfig.getMaxProtectionBlocksPerPlayer();
                DebugLogger.log("Nueva configuración aplicada - Límite de bloques por jugador: " + 
                    (maxBlocks == -1 ? "Ilimitado" : String.valueOf(maxBlocks)));
                
                // Log de rangos de protección por tipo
                DebugLogger.log("Rangos de protección actualizados:");
                DebugLogger.log("- Básico: " + ProtectionBlocksConfig.getBasicProtectionRange());
                DebugLogger.log("- Avanzado: " + ProtectionBlocksConfig.getAdvancedProtectionRange());
                DebugLogger.log("- Superior: " + ProtectionBlocksConfig.getSuperiorProtectionRange());
                DebugLogger.log("- Élite: " + ProtectionBlocksConfig.getEliteProtectionRange());
                DebugLogger.log("- Maestro: " + ProtectionBlocksConfig.getMasterProtectionRange());
                
                // Forzar actualización de todos los bloques de protección
                // Como los rangos se calculan dinámicamente, no necesitamos hacer nada especial
                // Los bloques ya usarán los nuevos valores automáticamente
                DebugLogger.log("Los bloques existentes usarán automáticamente los nuevos rangos");
                
            } catch (Exception e) {
                DebugLogger.log("Error al aplicar nueva configuración: " + e.getMessage());
            }
        }
    }
}