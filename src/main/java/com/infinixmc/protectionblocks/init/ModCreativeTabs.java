package com.infinixmc.protectionblocks.init;

import com.infinixmc.protectionblocks.ProtectionBlocksMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProtectionBlocksMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> PROTECTION_BLOCKS_TAB = CREATIVE_MODE_TABS.register("protection_blocks_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BASIC_PROTECTION_BLOCK.get()))
            .title(Component.translatable("itemGroup.protectionblocks"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BASIC_PROTECTION_BLOCK.get());
                output.accept(ModItems.ADVANCED_PROTECTION_BLOCK.get());
                output.accept(ModItems.SUPERIOR_PROTECTION_BLOCK.get());
                output.accept(ModItems.ELITE_PROTECTION_BLOCK.get());
                output.accept(ModItems.MASTER_PROTECTION_BLOCK.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}