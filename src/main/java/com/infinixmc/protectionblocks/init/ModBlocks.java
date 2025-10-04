package com.infinixmc.protectionblocks.init;

import com.infinixmc.protectionblocks.ProtectionBlocksMod;
import com.infinixmc.protectionblocks.blocks.ProtectionBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, ProtectionBlocksMod.MOD_ID);

    // Bloque Básico - Rango 5x5
    public static final RegistryObject<Block> BASIC_PROTECTION_BLOCK = BLOCKS.register("basic_protection_block",
        () -> new ProtectionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(0.5F, 0.5F) // Dureza como tierra - se rompe rápido
            .sound(SoundType.GRAVEL), 5, "Básico")); // Sin herramienta requerida

    // Bloque Avanzado - Rango 10x10
    public static final RegistryObject<Block> ADVANCED_PROTECTION_BLOCK = BLOCKS.register("advanced_protection_block",
        () -> new ProtectionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(0.5F, 0.5F) // Dureza como tierra - se rompe rápido
            .sound(SoundType.GRAVEL), 10, "Avanzado")); // Sin herramienta requerida

    // Bloque Superior - Rango 15x15
    public static final RegistryObject<Block> SUPERIOR_PROTECTION_BLOCK = BLOCKS.register("superior_protection_block",
        () -> new ProtectionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIAMOND)
            .strength(0.5F, 0.5F) // Dureza como tierra - se rompe rápido
            .sound(SoundType.GRAVEL), 15, "Superior")); // Sin herramienta requerida

    // Bloque Élite - Rango 20x20
    public static final RegistryObject<Block> ELITE_PROTECTION_BLOCK = BLOCKS.register("elite_protection_block",
        () -> new ProtectionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.EMERALD)
            .strength(0.5F, 0.5F) // Dureza como tierra - se rompe rápido
            .sound(SoundType.GRAVEL), 20, "Élite")); // Sin herramienta requerida

    // Bloque Maestro - Rango 25x25
    public static final RegistryObject<Block> MASTER_PROTECTION_BLOCK = BLOCKS.register("master_protection_block",
        () -> new ProtectionBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .strength(0.5F, 0.5F) // Dureza como tierra - se rompe rápido
            .sound(SoundType.GRAVEL), 25, "Maestro")); // Sin herramienta requerida

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}