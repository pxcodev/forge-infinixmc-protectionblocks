package com.infinixmc.protectionblocks.init;

import com.infinixmc.protectionblocks.ProtectionBlocksMod;
import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ProtectionBlocksMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ProtectionBlockEntity>> PROTECTION_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("protection_block_entity", () ->
            BlockEntityType.Builder.of(ProtectionBlockEntity::new,
                ModBlocks.BASIC_PROTECTION_BLOCK.get(),
                ModBlocks.ADVANCED_PROTECTION_BLOCK.get(),
                ModBlocks.SUPERIOR_PROTECTION_BLOCK.get(),
                ModBlocks.ELITE_PROTECTION_BLOCK.get(),
                ModBlocks.MASTER_PROTECTION_BLOCK.get()
            ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}