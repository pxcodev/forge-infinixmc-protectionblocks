package com.infinixmc.protectionblocks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class RenderConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Configuración de cuadrícula para cara externa
    public static final ForgeConfigSpec.IntValue EXTERNAL_GRID_SPACING;
    
    static {
        BUILDER.push("Configuraciones de Renderizado de Cara Externa");
        
        EXTERNAL_GRID_SPACING = BUILDER
            .comment("Espaciado de cuadrícula para cara externa (en bloques)")
            .defineInRange("external_grid_spacing", 8, 2, 32);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}