package com.infinixmc.protectionblocks.blocks;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.init.ModBlockEntities;
import com.infinixmc.protectionblocks.util.ProtectionManager;
import com.infinixmc.protectionblocks.util.DebugLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ProtectionBlock extends BaseEntityBlock {
    private final int protectionRange;
    private final String blockName;

    public ProtectionBlock(Properties properties, int protectionRange, String blockName) {
        super(properties);
        this.protectionRange = protectionRange;
        this.blockName = blockName;
    }

    public int getProtectionRange() {
        return protectionRange;
    }

    public String getBlockName() {
        return blockName;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProtectionBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ProtectionBlockEntity protectionBlockEntity) {
                if (player.isShiftKeyDown()) {
                    // Cambiar modo de visualización
                    protectionBlockEntity.toggleVisualizationMode(player);
                } else {
                    // Mostrar información del bloque usando el rango dinámico
                    int currentRange = protectionBlockEntity.getProtectionRange();
                    int totalAreaSize = (currentRange * 2) + 1; // El área total del cubo
                    DebugLogger.log("Mostrando info del bloque - Rango fijo del constructor: " + protectionRange + ", Rango dinámico: " + currentRange);
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.block_info", blockName));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.block_info_range", currentRange, totalAreaSize, totalAreaSize));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.block_info_owner", protectionBlockEntity.getOwnerName()));
                    player.sendSystemMessage(Component.translatable("message.protectionblocks.block_info_toggle"));
                    // Debug adicional para verificar el valor que se está mostrando
                    DebugLogger.log("Mensaje enviado al jugador - Valor del rango: " + currentRange + ", Área total: " + totalAreaSize + "x" + totalAreaSize);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ProtectionBlockEntity protectionBlockEntity) {
                // El propietario se establecerá cuando un jugador coloque el bloque
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // El bloque está siendo completamente removido (no solo cambiando de estado)
            
            if (!level.isClientSide) {
                // En el servidor: obtener la entidad del bloque antes de que sea removida
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ProtectionBlockEntity protectionBlock) {
                    // IMPORTANTE: Remover del cache antes de eliminar el bloque
                    int range = protectionBlock.getProtectionRange();
                    com.infinixmc.protectionblocks.util.ProtectionAreaCache.removeProtectionBlock(level, pos, range);
                    
                    // Marcar la entidad como removida explícitamente
                    protectionBlock.setRemoved();
                    
                    // Limpiar cualquier visualización asociada
                    ProtectionManager.cleanupProtectionBlockVisualization(level, pos);
                    
                    // Enviar packet a todos los clientes para limpiar el área coloreada
                    com.infinixmc.protectionblocks.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.DIMENSION.with(level::dimension),
                        new com.infinixmc.protectionblocks.network.packets.ClearProtectionAreaPacket(pos)
                    );
                }
            } else {
                // En el cliente: limpiar directamente el área coloreada
                com.infinixmc.protectionblocks.client.ProtectionAreaRenderer.disableFor(pos);
            }
        }
        
        // Llamar al método padre que limpiará la entidad del bloque
        super.onRemove(state, level, pos, newState, isMoving);
    }
}