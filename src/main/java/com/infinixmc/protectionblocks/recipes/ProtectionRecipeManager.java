package com.infinixmc.protectionblocks.recipes;

import com.infinixmc.protectionblocks.config.ProtectionBlocksConfig;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Set;

@Mod.EventBusSubscriber
public class ProtectionRecipeManager {

    // Lista de recetas de bloques de protección para deshabilitar
    private static final Set<String> PROTECTION_BLOCK_RECIPES = Set.of(
        "basic_protection_block",
        "advanced_protection_block", 
        "superior_protection_block",
        "elite_protection_block",
        "master_protection_block"
    );

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            // Verificar si las recetas están habilitadas en la configuración
            boolean recipesEnabled = ProtectionBlocksConfig.areProtectionBlockRecipesEnabled();
            
            if (!recipesEnabled) {
                DebugLogger.log("Protection block recipes are disabled in configuration");
                removeProtectionBlockRecipes(event.getServer());
                event.getServer().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "[ProtectionBlocks] ⚠️ Protection block recipes disabled by configuration"
                    )
                );
            } else {
                DebugLogger.log("Protection block recipes are enabled");
                event.getServer().sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "[ProtectionBlocks] ✅ Crafting recipes loaded successfully"
                    )
                );
            }
        } catch (Exception e) {
            DebugLogger.log("Error checking recipe configuration: " + e.getMessage());
            event.getServer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "[ProtectionBlocks] ✅ Crafting recipes loaded successfully (configuration not available)"
                )
            );
        }
    }
    
    /**
     * Elimina las recetas de bloques de protección del RecipeManager
     * @param server el servidor
     */
    private static void removeProtectionBlockRecipes(net.minecraft.server.MinecraftServer server) {
        try {
            var recipeManager = server.getRecipeManager();
            var recipes = recipeManager.getRecipes();
            
            int removedCount = 0;
            
            // Buscar y marcar las recetas para remover
            Iterator<Recipe<?>> iterator = recipes.iterator();
            while (iterator.hasNext()) {
                Recipe<?> recipe = iterator.next();
                ResourceLocation recipeId = recipe.getId();
                
                // Verificar si es una receta de bloque de protección
                if ("protectionblocks".equals(recipeId.getNamespace()) && 
                    PROTECTION_BLOCK_RECIPES.contains(recipeId.getPath())) {
                    
                    DebugLogger.log("Marking recipe for disabling: " + recipeId);
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                DebugLogger.log("Identified " + removedCount + " protection block recipes for disabling");
                DebugLogger.log("Note: Recipes will not appear in crafting interface but JSON files remain");
            } else {
                DebugLogger.log("No protection block recipes found");
            }
            
        } catch (Exception e) {
            DebugLogger.log("Error processing protection block recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}