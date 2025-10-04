package com.infinixmc.protectionblocks.events;

import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Event handler para manejar la deshabilitación de recetas
 */
@Mod.EventBusSubscriber
public class RecipeDisableHandler {

    @SubscribeEvent
    public static void onCraftingEvent(PlayerEvent.ItemCraftedEvent event) {
        try {
            // Verificar si las recetas están deshabilitadas
            if (!ProtectionBlocksConfig.areProtectionBlockRecipesEnabled()) {
                
                // Obtener el ResourceLocation del item crafteado
                Item craftedItem = event.getCrafting().getItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(craftedItem);
                
                if (itemId != null && "protectionblocks".equals(itemId.getNamespace()) && 
                    itemId.getPath().contains("protection_block")) {
                    
                    // Cancelar el crafteo
                    DebugLogger.log("Cancelling protection block crafting: " + itemId + " (recipes disabled)");
                    
                    // Remover el item del inventario del jugador
                    event.getCrafting().setCount(0);
                    
                    // Notificar al jugador
                    Player player = event.getEntity();
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.recipes_disabled"));
                    
                    DebugLogger.log("Crafting cancelled for player: " + player.getName().getString());
                }
            }
        } catch (Exception e) {
            DebugLogger.log("Error in RecipeDisableHandler.onCraftingEvent: " + e.getMessage());
        }
    }
}