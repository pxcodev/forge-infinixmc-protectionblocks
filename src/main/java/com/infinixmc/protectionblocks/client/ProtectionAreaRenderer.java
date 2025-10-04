package com.infinixmc.protectionblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "protectionblocks", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ProtectionAreaRenderer {
    
    private static final Map<BlockPos, ProtectionArea> activeProtections = new HashMap<>();
    
    // Colores predefinidos para protecciones
    public static final ProtectionColor[] AVAILABLE_COLORS = {
        new ProtectionColor("Azul", 0.2f, 0.8f, 1.0f),
        new ProtectionColor("Verde", 0.2f, 1.0f, 0.2f),
        new ProtectionColor("Rojo", 1.0f, 0.2f, 0.2f),
        new ProtectionColor("Amarillo", 1.0f, 1.0f, 0.2f),
        new ProtectionColor("Púrpura", 1.0f, 0.2f, 1.0f),
        new ProtectionColor("Naranja", 1.0f, 0.6f, 0.2f),
        new ProtectionColor("Cian", 0.2f, 1.0f, 1.0f),
        new ProtectionColor("Rosa", 1.0f, 0.6f, 0.8f)
    };
    
    public static void enableFor(BlockPos centerPos, int range, int colorIndex) {
        ProtectionColor color = AVAILABLE_COLORS[colorIndex % AVAILABLE_COLORS.length];
        activeProtections.put(centerPos, new ProtectionArea(range, color));
    }
    
    public static void enableFor(BlockPos centerPos, int range) {
        enableFor(centerPos, range, 0); // Color azul por defecto
    }
    
    public static void disableFor(BlockPos centerPos) {
        activeProtections.remove(centerPos);
    }
    
    public static boolean isEnabledFor(BlockPos centerPos) {
        return activeProtections.containsKey(centerPos);
    }
    
    public static int getColorIndex(BlockPos centerPos) {
        ProtectionArea area = activeProtections.get(centerPos);
        if (area != null) {
            for (int i = 0; i < AVAILABLE_COLORS.length; i++) {
                if (AVAILABLE_COLORS[i].equals(area.color)) {
                    return i;
                }
            }
        }
        return 0; // Azul por defecto
    }
    
    public static void clearAll() {
        activeProtections.clear();
    }
    
    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        if (activeProtections.isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        // Obtener posición de la cámara para el rendering relativo
        Vec3 cameraPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (Map.Entry<BlockPos, ProtectionArea> entry : activeProtections.entrySet()) {
            BlockPos centerPos = entry.getKey();
            ProtectionArea area = entry.getValue();
            
            renderProtectionArea(poseStack, bufferSource, centerPos, area.range, area.color);
        }
        
        poseStack.popPose();
        bufferSource.endBatch();
    }
    
    private static void renderProtectionArea(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos centerPos, int range, ProtectionColor color) {
        // Crear el área de protección como wireframe grueso (outline de bloques)
        double minX = centerPos.getX() - range;
        double minY = centerPos.getY() - range;
        double minZ = centerPos.getZ() - range;
        double maxX = centerPos.getX() + range + 1;
        double maxY = centerPos.getY() + range + 1;
        double maxZ = centerPos.getZ() + range + 1;
        
        // Usar RenderType para líneas gruesas
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        poseStack.pushPose();
        
        Matrix4f matrix = poseStack.last().pose();
        
        // Renderizar wireframe denso (múltiples líneas para crear efecto de bloque)
        renderProtectionWireframeThick(vertexConsumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, color);
        
        poseStack.popPose();
    }
    
    private static void renderProtectionWireframeThick(VertexConsumer consumer, Matrix4f matrix, 
                                            double minX, double minY, double minZ, 
                                            double maxX, double maxY, double maxZ, 
                                            ProtectionColor color) {
        float red = color.red;
        float green = color.green;
        float blue = color.blue;
        float alpha = 1.0f; // Líneas opacas
        
        // Convertir a float para el rendering
        float fMinX = (float) minX;
        float fMinY = (float) minY;
        float fMinZ = (float) minZ;
        float fMaxX = (float) maxX;
        float fMaxY = (float) maxY;
        float fMaxZ = (float) maxZ;
        
        // NUEVO SISTEMA: TODAS las caras externas con cuadrícula de bloques de Minecraft
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Configuración de cuadrícula: 1 bloque = 1 cuadro (tamaño real de Minecraft)
        float gridSpacing = 1.0f; // Cada cuadro = 1 bloque de Minecraft
        
        // Renderizar TODAS las caras externas con cuadrícula de bloques
        
        // Cara superior (techo)
        renderCompleteGrid(consumer, matrix, fMinX, fMaxX, fMinZ, fMaxZ, fMaxY, gridSpacing, 
                          red, green, blue, alpha, "horizontal");
        
        // Cara inferior (suelo)
        renderCompleteGrid(consumer, matrix, fMinX, fMaxX, fMinZ, fMaxZ, fMinY, gridSpacing, 
                          red, green, blue, alpha, "horizontal");
        
        // Cara norte
        renderCompleteGrid(consumer, matrix, fMinX, fMaxX, fMinY, fMaxY, fMinZ, gridSpacing, 
                          red, green, blue, alpha, "vertical_ns");
        
        // Cara sur
        renderCompleteGrid(consumer, matrix, fMinX, fMaxX, fMinY, fMaxY, fMaxZ, gridSpacing, 
                          red, green, blue, alpha, "vertical_ns");
        
        // Cara este
        renderCompleteGrid(consumer, matrix, fMinZ, fMaxZ, fMinY, fMaxY, fMaxX, gridSpacing, 
                          red, green, blue, alpha, "vertical_ew");
        
        // Cara oeste
        renderCompleteGrid(consumer, matrix, fMinZ, fMaxZ, fMinY, fMaxY, fMinX, gridSpacing, 
                          red, green, blue, alpha, "vertical_ew");
    }
    
    // Método unificado para renderizar cuadrícula completa en cualquier cara
    private static void renderCompleteGrid(VertexConsumer consumer, Matrix4f matrix,
                                         float coord1Min, float coord1Max, 
                                         float coord2Min, float coord2Max, 
                                         float facePosition, float gridSpacing,
                                         float red, float green, float blue, float alpha,
                                         String orientation) {
        
        switch (orientation) {
            case "horizontal":
                // Para caras horizontales (suelo/techo): X y Z como coordenadas, Y fijo
                // Líneas paralelas al eje X (norte-sur)
                for (float x = coord1Min; x <= coord1Max; x += gridSpacing) {
                    addLine(consumer, matrix, x, facePosition, coord2Min, x, facePosition, coord2Max, 
                           red, green, blue, alpha);
                }
                // Líneas paralelas al eje Z (este-oeste)
                for (float z = coord2Min; z <= coord2Max; z += gridSpacing) {
                    addLine(consumer, matrix, coord1Min, facePosition, z, coord1Max, facePosition, z, 
                           red, green, blue, alpha);
                }
                break;
                
            case "vertical_ns":
                // Para caras norte/sur: X y Y como coordenadas, Z fijo
                // Líneas horizontales
                for (float y = coord2Min; y <= coord2Max; y += gridSpacing) {
                    addLine(consumer, matrix, coord1Min, y, facePosition, coord1Max, y, facePosition, 
                           red, green, blue, alpha);
                }
                // Líneas verticales
                for (float x = coord1Min; x <= coord1Max; x += gridSpacing) {
                    addLine(consumer, matrix, x, coord2Min, facePosition, x, coord2Max, facePosition, 
                           red, green, blue, alpha);
                }
                break;
                
            case "vertical_ew":
                // Para caras este/oeste: Z y Y como coordenadas, X fijo
                // Líneas horizontales
                for (float y = coord2Min; y <= coord2Max; y += gridSpacing) {
                    addLine(consumer, matrix, facePosition, y, coord1Min, facePosition, y, coord1Max, 
                           red, green, blue, alpha);
                }
                // Líneas verticales
                for (float z = coord1Min; z <= coord1Max; z += gridSpacing) {
                    addLine(consumer, matrix, facePosition, coord2Min, z, facePosition, coord2Max, z, 
                           red, green, blue, alpha);
                }
                break;
        }
    }
    
    private static void addLine(VertexConsumer consumer, Matrix4f matrix,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
    }
    
    // Clases auxiliares para el sistema de colores
    public static class ProtectionColor {
        public final String name;
        public final float red, green, blue;
        
        public ProtectionColor(String name, float red, float green, float blue) {
            this.name = name;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ProtectionColor)) return false;
            ProtectionColor other = (ProtectionColor) obj;
            return Float.compare(red, other.red) == 0 && 
                   Float.compare(green, other.green) == 0 && 
                   Float.compare(blue, other.blue) == 0;
        }
    }
    
    public static class ProtectionArea {
        public final int range;
        public final ProtectionColor color;
        
        public ProtectionArea(int range, ProtectionColor color) {
            this.range = range;
            this.color = color;
        }
    }
}