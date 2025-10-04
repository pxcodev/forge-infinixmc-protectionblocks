package com.infinixmc.protectionblocks.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-specific configuration for Protection Blocks
 */
public class ServerConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Server configurations
    public static final ForgeConfigSpec.IntValue MAX_PROTECTION_BLOCKS_PER_PLAYER;
    
    // Protection block types configuration
    public static final ForgeConfigSpec.IntValue BASIC_PROTECTION_RANGE;
    public static final ForgeConfigSpec.IntValue ADVANCED_PROTECTION_RANGE;
    public static final ForgeConfigSpec.IntValue SUPERIOR_PROTECTION_RANGE;
    public static final ForgeConfigSpec.IntValue ELITE_PROTECTION_RANGE;
    public static final ForgeConfigSpec.IntValue MASTER_PROTECTION_RANGE;
    
    // Behavior configuration
    public static final ForgeConfigSpec.BooleanValue ALLOW_OVERLAPPING_PROTECTION;
    public static final ForgeConfigSpec.IntValue MIN_DISTANCE_BETWEEN_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PROTECTION_VISUALIZATION;
    public static final ForgeConfigSpec.IntValue MAX_ALLIES_PER_BLOCK;
    
    // Recipe configuration
    public static final ForgeConfigSpec.BooleanValue ENABLE_PROTECTION_BLOCK_RECIPES;
    
    static {
        BUILDER.comment("Protection Blocks Server Configuration")
               .comment("Configure the limits and behavior of protection blocks");
        
        BUILDER.push("general");
        
        // Protection blocks per player limit
        MAX_PROTECTION_BLOCKS_PER_PLAYER = BUILDER
                .comment("Maximum number of protection blocks a player can place")
                .comment("Set to -1 for unlimited")
                .defineInRange("maxProtectionBlocksPerPlayer", 10, -1, 100);
        
        BUILDER.pop();
        
        BUILDER.push("protection_types");
        BUILDER.comment("Protection range configuration for each block type");
        BUILDER.comment("IMPORTANT: These values define the protection range in blocks");
        BUILDER.comment("The protected area will be (range*2+1) x (range*2+1) x (range*2+1) centered on the block");
        
        // Range configuration by block type
        BASIC_PROTECTION_RANGE = BUILDER
                .comment("Protection range of the Basic Block in blocks")
                .comment("Protected area: 13x13x13 (range 6)")
                .defineInRange("basicProtectionRange", 6, 1, 50);
        
        ADVANCED_PROTECTION_RANGE = BUILDER
                .comment("Protection range of the Advanced Block in blocks")
                .comment("Protected area: 21x21x21 (range 10)")
                .defineInRange("advancedProtectionRange", 10, 1, 50);
        
        SUPERIOR_PROTECTION_RANGE = BUILDER
                .comment("Protection range of the Superior Block in blocks")
                .comment("Protected area: 31x31x31 (range 15)")
                .defineInRange("superiorProtectionRange", 15, 1, 50);
        
        ELITE_PROTECTION_RANGE = BUILDER
                .comment("Protection range of the Elite Block in blocks")
                .comment("Protected area: 41x41x41 (range 20)")
                .defineInRange("eliteProtectionRange", 20, 1, 50);
        
        MASTER_PROTECTION_RANGE = BUILDER
                .comment("Protection range of the Master Block in blocks")
                .comment("Protected area: 51x51x51 (range 25)")
                .defineInRange("masterProtectionRange", 25, 1, 50);
        
        BUILDER.pop();
        
        BUILDER.push("behavior");
        
        // Allow overlapping protection areas
        ALLOW_OVERLAPPING_PROTECTION = BUILDER
                .comment("Allow protection areas to overlap")
                .define("allowOverlappingProtection", false);
        
        // Minimum distance between protection blocks
        MIN_DISTANCE_BETWEEN_BLOCKS = BUILDER
                .comment("Minimum distance in blocks between protection blocks of the same player")
                .comment("Set to 0 to allow any distance")
                .defineInRange("minDistanceBetweenBlocks", 0, 0, 1000);
        
        // Enable protection visualization
        ENABLE_PROTECTION_VISUALIZATION = BUILDER
                .comment("Allow players to visualize protection areas")
                .define("enableProtectionVisualization", true);
        
        // Maximum allies per block
        MAX_ALLIES_PER_BLOCK = BUILDER
                .comment("Maximum number of allies that can be added to a protection block")
                .comment("Set to -1 for unlimited")
                .defineInRange("maxAlliesPerBlock", 10, -1, 50);

        BUILDER.pop();
        
        BUILDER.push("recipes");
        
        // Enable/disable protection block recipes
        ENABLE_PROTECTION_BLOCK_RECIPES = BUILDER
                .comment("Enable crafting recipes for protection blocks")
                .comment("Set to false to disable all protection block recipes")
                .define("enableProtectionBlockRecipes", true);

        BUILDER.pop();        SPEC = BUILDER.build();
    }
}