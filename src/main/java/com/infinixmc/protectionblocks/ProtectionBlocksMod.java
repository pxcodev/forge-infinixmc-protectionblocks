package com.infinixmc.protectionblocks;

import com.infinixmc.protectionblocks.init.ModBlocks;
import com.infinixmc.protectionblocks.init.ModItems;
import com.infinixmc.protectionblocks.init.ModBlockEntities;
import com.infinixmc.protectionblocks.init.ModCreativeTabs;
import com.infinixmc.protectionblocks.events.ProtectionEventHandler;
import com.infinixmc.protectionblocks.events.ServerEventHandler;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ProtectionBlocksMod.MOD_ID)
public class ProtectionBlocksMod {
    public static final String MOD_ID = "protectionblocks";
    public static final Logger LOGGER = LogManager.getLogger();

    public ProtectionBlocksMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Inicializar debug logger personalizado
        DebugLogger.init();
        DebugLogger.log("Starting Protection Blocks Mod...");
        
        // Registrar configuración del servidor PRIMERO
        DebugLogger.log("Registering server configuration...");
        ProtectionBlocksConfig.register();
        
        // Registrar bloques e items
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        
        // Registrar red
        NetworkHandler.registerPackets();
        
        // Registrar eventos de protección
        MinecraftForge.EVENT_BUS.register(new ProtectionEventHandler());
        
        // Registrar eventos del servidor (para debug y configuración)
        MinecraftForge.EVENT_BUS.register(ServerEventHandler.class);
        
        // Registrar handler de recetas deshabilitadas
        MinecraftForge.EVENT_BUS.register(com.infinixmc.protectionblocks.events.RecipeDisableHandler.class);
        
        LOGGER.info("Protection Blocks Mod loaded successfully!");
        DebugLogger.log("Protection Blocks Mod loaded successfully!");
    }
}