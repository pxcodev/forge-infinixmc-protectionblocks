package com.infinixmc.protectionblocks.init;

import com.infinixmc.protectionblocks.ProtectionBlocksMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, ProtectionBlocksMod.MOD_ID);

    // Items de los bloques de protección
    public static final RegistryObject<Item> BASIC_PROTECTION_BLOCK = ITEMS.register("basic_protection_block",
        () -> new BlockItem(ModBlocks.BASIC_PROTECTION_BLOCK.get(), 
            new Item.Properties()));

    public static final RegistryObject<Item> ADVANCED_PROTECTION_BLOCK = ITEMS.register("advanced_protection_block",
        () -> new BlockItem(ModBlocks.ADVANCED_PROTECTION_BLOCK.get(), 
            new Item.Properties()));

    public static final RegistryObject<Item> SUPERIOR_PROTECTION_BLOCK = ITEMS.register("superior_protection_block",
        () -> new BlockItem(ModBlocks.SUPERIOR_PROTECTION_BLOCK.get(), 
            new Item.Properties()));

    public static final RegistryObject<Item> ELITE_PROTECTION_BLOCK = ITEMS.register("elite_protection_block",
        () -> new BlockItem(ModBlocks.ELITE_PROTECTION_BLOCK.get(), 
            new Item.Properties()));

    public static final RegistryObject<Item> MASTER_PROTECTION_BLOCK = ITEMS.register("master_protection_block",
        () -> new BlockItem(ModBlocks.MASTER_PROTECTION_BLOCK.get(), 
            new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}