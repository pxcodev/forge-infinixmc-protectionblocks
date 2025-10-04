package com.infinixmc.protectionblocks.gui;

import com.infinixmc.protectionblocks.blockentity.ProtectionBlockEntity;
import com.infinixmc.protectionblocks.client.ProtectionAreaRenderer;
import com.infinixmc.protectionblocks.util.AllyData;
import com.infinixmc.protectionblocks.util.PermissionType;
import com.infinixmc.protectionblocks.network.NetworkHandler;
import com.infinixmc.protectionblocks.network.packets.AddAllyPacket;
import com.infinixmc.protectionblocks.network.packets.RemoveAllyPacket;
import com.infinixmc.protectionblocks.network.packets.UpdatePermissionsPacket;
import com.infinixmc.protectionblocks.network.packets.UpdateWelcomeMessagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ProtectionManagementScreen extends Screen {
    private final ProtectionBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final Level level;
    
    private EditBox playerNameInput;
    private EditBox zoneNameInput;
    private Button addAllyButton;
    private Button removeAllyButton;
    private Button playerDropdownButton;
    private List<AllyListEntry> allyEntries;
    private List<String> availablePlayers;
    private boolean showPlayerDropdown = false;
    private int scrollOffset = 0;
    private int playerDropdownScroll = 0;
    private final int maxVisiblePlayers = 5;
    
    // Variables para el sistema de selección de permisos
    private boolean showPermissionDropdown = false;
    private String selectedAllyForPermissions = "";
    private int permissionDropdownScroll = 0;
    private final int maxVisiblePermissions = 4;
    
    // Variables para el sistema de visualización de bloques protegidos
    private Checkbox showProtectedAreaCheckbox;
    private boolean showProtectedArea = false;
    private Button colorButton;
    private int selectedColorIndex = 0;
    
    // Opciones de permisos predefinidas - se generan dinámicamente con localización
    private List<PermissionOption> permissionOptions;
    
    private Component getColorButtonText(int colorIndex) {
        return Component.literal("§" + getColorCode(colorIndex) + Component.translatable("gui.protectionblocks.management.color_button").getString());
    }
    
    private List<PermissionOption> createPermissionOptions() {
        return List.of(
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.none").getString(), Set.of()),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.build").getString(), Set.of(PermissionType.BUILD)),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.build_break").getString(), Set.of(PermissionType.BUILD, PermissionType.BREAK)),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.interact").getString(), Set.of(PermissionType.INTERACT)),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.build_interact").getString(), Set.of(PermissionType.BUILD, PermissionType.INTERACT)),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.break_interact").getString(), Set.of(PermissionType.BREAK, PermissionType.INTERACT)),
            new PermissionOption(Component.translatable("gui.protectionblocks.management.permissions.all").getString(), Set.of(PermissionType.BUILD, PermissionType.BREAK, PermissionType.INTERACT))
        );
    }
    
    public ProtectionManagementScreen(ProtectionBlockEntity blockEntity, BlockPos blockPos, Level level) {
        super(Component.translatable("gui.protectionblocks.management.title"));
        this.blockEntity = blockEntity;
        this.blockPos = blockPos;
        this.level = level;
        this.allyEntries = new ArrayList<>();
        this.permissionOptions = createPermissionOptions(); // Inicializar las opciones de permisos con localización
        
        // Verificar si la visualización ya está activa para este bloque
        this.showProtectedArea = ProtectionAreaRenderer.isEnabledFor(blockPos);
        this.selectedColorIndex = ProtectionAreaRenderer.getColorIndex(blockPos);
        this.availablePlayers = new ArrayList<>();
    }
    
    private void applyPermissionOption(String playerName, PermissionOption option) {
        // Encontrar el aliado y aplicar los permisos
        for (AllyListEntry entry : allyEntries) {
            if (entry.ally.getPlayerName().equals(playerName)) {
                // Limpiar permisos existentes
                entry.ally.setPermission(PermissionType.BUILD, false);
                entry.ally.setPermission(PermissionType.BREAK, false);
                entry.ally.setPermission(PermissionType.INTERACT, false);
                
                // Aplicar nuevos permisos
                for (PermissionType permission : option.permissions) {
                    entry.ally.setPermission(permission, true);
                }
                
                // Enviar actualización al servidor
                NetworkHandler.sendToServer(new UpdatePermissionsPacket(blockPos, playerName, entry.ally.getPermissions()));
                break;
            }
        }
    }
    
    private void renderPermissionDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Elevar Z para que tape todo
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        
        int centerX = this.width / 2;
        
        // Posición responsiva del dropdown
        int dropdownWidth = Math.min(220, this.width / 3); // Máximo 220px o 33% del ancho
        int dropdownX = Math.min(centerX + 60, this.width - dropdownWidth - 10); // Evitar que se salga de pantalla
        int dropdownY = Math.max(120, this.height / 5); // 20% desde arriba o mínimo 120px
        int dropdownHeight = Math.min(maxVisiblePermissions, permissionOptions.size()) * 18 + 20; // Más espacioso
        
        // Crear múltiples capas de fondo para asegurar opacidad completa
        // Capa base completamente opaca (más grande para sombra)
        guiGraphics.fill(dropdownX - 6, dropdownY - 6, dropdownX + dropdownWidth + 6, dropdownY + dropdownHeight + 6, 0xFF000000);
        
        // Segunda capa para asegurar opacidad
        guiGraphics.fill(dropdownX - 3, dropdownY - 3, dropdownX + dropdownWidth + 3, dropdownY + dropdownHeight + 3, 0xFF000000);
        
        // Fondo principal completamente opaco
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF000000);
        
        // Capa interior con color de fondo
        guiGraphics.fill(dropdownX + 1, dropdownY + 1, dropdownX + dropdownWidth - 1, dropdownY + dropdownHeight - 1, 0xFF1A1A1A);
        
        // Borde más visible
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + 2, 0xFF666666); // Top más grueso
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + 2, dropdownY + dropdownHeight, 0xFF666666); // Left más grueso
        guiGraphics.fill(dropdownX + dropdownWidth - 2, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF666666); // Right más grueso
        guiGraphics.fill(dropdownX, dropdownY + dropdownHeight - 2, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF666666); // Bottom más grueso
        
        // Título con fondo destacado
        guiGraphics.fill(dropdownX + 2, dropdownY + 2, dropdownX + dropdownWidth - 2, dropdownY + 18, 0xFF333333);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.select_permissions").getString(), dropdownX + dropdownWidth/2, dropdownY + 6, 0xFFFFFF);
        
        // Opciones de permisos
        for (int i = 0; i < Math.min(maxVisiblePermissions, permissionOptions.size() - permissionDropdownScroll); i++) {
            int index = i + permissionDropdownScroll;
            if (index < permissionOptions.size()) {
                PermissionOption option = permissionOptions.get(index);
                int optionY = dropdownY + 20 + (i * 18);
                
                // Verificar si el mouse está sobre esta opción
                boolean isHovering = mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                                   mouseY >= optionY && mouseY <= optionY + 18;
                
                // Fondo de la opción
                if (isHovering) {
                    guiGraphics.fill(dropdownX + 2, optionY, dropdownX + dropdownWidth - 2, optionY + 18, 0xFF555555);
                } else {
                    guiGraphics.fill(dropdownX + 2, optionY, dropdownX + dropdownWidth - 2, optionY + 18, 0xFF2A2A2A);
                }
                
                // Texto de la opción con colores apropiados
                String colorCode = isHovering ? "§f" : "§7";
                guiGraphics.drawString(this.font, colorCode + option.name, dropdownX + 8, optionY + 5, 0xFFFFFF);
            }
        }
        
        // Indicadores de scroll si es necesario
        if (permissionOptions.size() > maxVisiblePermissions) {
            if (permissionDropdownScroll > 0) {
                guiGraphics.drawCenteredString(this.font, "§a▲", dropdownX + dropdownWidth/2, dropdownY - 12, 0xFFFFFF);
            }
            if (permissionDropdownScroll + maxVisiblePermissions < permissionOptions.size()) {
                guiGraphics.drawCenteredString(this.font, "§a▼", dropdownX + dropdownWidth/2, dropdownY + dropdownHeight + 5, 0xFFFFFF);
            }
        }
        
        guiGraphics.pose().popPose();
    }
    
    private void renderPlayerDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Elevar Z para que tape todo
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        
        int centerX = this.width / 2;
        
        // Posición responsiva del dropdown
        int dropdownWidth = Math.min(160, this.width / 4); // Máximo 160px o 25% del ancho
        int dropdownX = Math.max(centerX - dropdownWidth/2, 10); // Centrarlo o mínimo 10px del borde
        int dropdownY = Math.max(100, this.height / 8); // 12.5% desde arriba o mínimo 100px
        int dropdownHeight = Math.min(maxVisiblePlayers, availablePlayers.size()) * 18 + 20; // Más espacioso
        
        // Crear múltiples capas de fondo para asegurar opacidad completa
        // Capa base completamente opaca (más grande para sombra)
        guiGraphics.fill(dropdownX - 6, dropdownY - 6, dropdownX + dropdownWidth + 6, dropdownY + dropdownHeight + 6, 0xFF000000);
        
        // Segunda capa para asegurar opacidad
        guiGraphics.fill(dropdownX - 3, dropdownY - 3, dropdownX + dropdownWidth + 3, dropdownY + dropdownHeight + 3, 0xFF000000);
        
        // Fondo principal completamente opaco
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF000000);
        
        // Capa interior con color de fondo
        guiGraphics.fill(dropdownX + 1, dropdownY + 1, dropdownX + dropdownWidth - 1, dropdownY + dropdownHeight - 1, 0xFF1A1A1A);
        
        // Borde más visible
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + 2, 0xFF666666); // Top más grueso
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + 2, dropdownY + dropdownHeight, 0xFF666666); // Left más grueso
        guiGraphics.fill(dropdownX + dropdownWidth - 2, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF666666); // Right más grueso
        guiGraphics.fill(dropdownX, dropdownY + dropdownHeight - 2, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF666666); // Bottom más grueso
        
        // Título con fondo destacado
        guiGraphics.fill(dropdownX + 2, dropdownY + 2, dropdownX + dropdownWidth - 2, dropdownY + 18, 0xFF333333);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.online_players").getString(), dropdownX + dropdownWidth/2, dropdownY + 6, 0xFFFFFF);
        
        // Renderizar jugadores
        for (int i = 0; i < Math.min(maxVisiblePlayers, availablePlayers.size() - playerDropdownScroll); i++) {
            int index = i + playerDropdownScroll;
            if (index < availablePlayers.size()) {
                String playerName = availablePlayers.get(index);
                int playerY = dropdownY + 20 + (i * 18);
                
                // Verificar si el mouse está sobre este jugador
                boolean isHovering = mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                                   mouseY >= playerY && mouseY <= playerY + 18;
                
                // Fondo de la opción
                if (isHovering) {
                    guiGraphics.fill(dropdownX + 2, playerY, dropdownX + dropdownWidth - 2, playerY + 18, 0xFF555555);
                } else {
                    guiGraphics.fill(dropdownX + 2, playerY, dropdownX + dropdownWidth - 2, playerY + 18, 0xFF2A2A2A);
                }
                
                // Texto del jugador con colores apropiados
                String colorCode = isHovering ? "§a" : "§7";
                guiGraphics.drawString(this.font, colorCode + playerName, dropdownX + 8, playerY + 5, 0xFFFFFF);
            }
        }
        
        // Mensaje si no hay jugadores disponibles
        if (availablePlayers.isEmpty()) {
            // Colocar el panel justo debajo del título "Jugadores Online" (después de dropdownY + 18)
            int messageY = dropdownY + 30; // 2 píxeles de separación del título
            int messageHeight = 36; // Para cubrir ambas líneas de texto
            
            // Usar EXACTAMENTE las mismas coordenadas que el dropdown principal para mismo ancho
            // Capas de fondo negro - MISMO ANCHO QUE EL DROPDOWN PRINCIPAL
            guiGraphics.fill(dropdownX - 6, messageY - 6, dropdownX + dropdownWidth + 6, messageY + messageHeight + 6, 0xFF000000);
            guiGraphics.fill(dropdownX - 3, messageY - 3, dropdownX + dropdownWidth + 3, messageY + messageHeight + 3, 0xFF000000);
            guiGraphics.fill(dropdownX, messageY, dropdownX + dropdownWidth, messageY + messageHeight, 0xFF000000);
            
            // Fondo interior - MISMO ANCHO QUE EL DROPDOWN PRINCIPAL
            guiGraphics.fill(dropdownX + 1, messageY + 1, dropdownX + dropdownWidth - 1, messageY + messageHeight - 1, 0xFF2A2A2A);
            
            // Bordes grises - MISMO ANCHO QUE EL DROPDOWN PRINCIPAL
            guiGraphics.fill(dropdownX, messageY, dropdownX + dropdownWidth, messageY + 2, 0xFF666666); // Top
            guiGraphics.fill(dropdownX, messageY, dropdownX + 2, messageY + messageHeight, 0xFF666666); // Left
            guiGraphics.fill(dropdownX + dropdownWidth - 2, messageY, dropdownX + dropdownWidth, messageY + messageHeight, 0xFF666666); // Right
            guiGraphics.fill(dropdownX, messageY + messageHeight - 2, dropdownX + dropdownWidth, messageY + messageHeight, 0xFF666666); // Bottom
            
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.no_players_available").getString(), dropdownX + dropdownWidth/2, messageY + 8, 0xAAAAAA);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.no_players_available2").getString(), dropdownX + dropdownWidth/2, messageY + 20, 0xAAAAAA);
        }
        
        // Indicadores de scroll si es necesario
        if (availablePlayers.size() > maxVisiblePlayers) {
            if (playerDropdownScroll > 0) {
                guiGraphics.drawCenteredString(this.font, "§a▲", dropdownX + dropdownWidth/2, dropdownY - 12, 0xFFFFFF);
            }
            if (playerDropdownScroll + maxVisiblePlayers < availablePlayers.size()) {
                guiGraphics.drawCenteredString(this.font, "§a▼", dropdownX + dropdownWidth/2, dropdownY + dropdownHeight + 5, 0xFFFFFF);
            }
        }
        
        guiGraphics.pose().popPose();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Sistema responsivo basado en porcentajes
        int centerX = this.width / 2;
        int startY = Math.max(40, this.height / 10); // 10% desde arriba, mínimo 40px
        
        // Calcular dimensiones responsivas
        int titleWidth = Math.min(400, this.width - 40); // Máximo 400px, mínimo 40px de margen
        int inputWidth = Math.min(150, this.width / 4); // 25% del ancho o máximo 150px
        int buttonWidth = Math.min(90, this.width / 8); // 12.5% del ancho o máximo 90px
        
        // Título sin el botón X
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.protectionblocks.management.ally_management", blockEntity.getBlockName()),
            button -> {}
        ).bounds(centerX - titleWidth/2, startY, titleWidth - 30, 20).build());
        
        // Botón X independiente al lado derecho del título
        this.addRenderableWidget(Button.builder(
            Component.literal("§c✖"),
            button -> this.onClose()
        ).bounds(centerX + titleWidth/2 - 25, startY, 25, 20).build());
        
        // Campo de entrada para nombre de jugador
        this.playerNameInput = new EditBox(this.font, centerX - inputWidth/2 - 15, startY + 30, inputWidth, 20, Component.translatable("gui.protectionblocks.management.player_name_input"));
        this.playerNameInput.setMaxLength(16);
        this.addRenderableWidget(this.playerNameInput);
        
        // Calcular posición donde termina la lista de aliados
        int listY = Math.max(120, this.height / 4);
        int listHeight = Math.min(200, this.height / 2);
        int zoneInputY = listY + listHeight + 20; // 20px de margen debajo de la lista
        
        // Campo de entrada para nombre de zona (debajo de la lista, alineado a la izquierda)
        int listLeftX = centerX - Math.min(500, this.width - 60) / 2; // Misma posición izquierda que la lista
        this.zoneNameInput = new EditBox(this.font, listLeftX + 10, zoneInputY, 100, 20, Component.translatable("gui.protectionblocks.management.zone_name_input"));
        this.zoneNameInput.setMaxLength(50); // Reducido para nombres de zona más cortos
        this.zoneNameInput.setValue(blockEntity.getZoneName());
        this.addRenderableWidget(this.zoneNameInput);

        // Botón para guardar nombre de zona (al lado del input)
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.protectionblocks.management.save_zone_name"),
            this::saveZoneName
        ).bounds(listLeftX + 120, zoneInputY, 60, 20).build());

        // Botón para mostrar lista de jugadores
        this.playerDropdownButton = this.addRenderableWidget(Button.builder(
            Component.literal("▼"),
            this::togglePlayerDropdown
        ).bounds(centerX + inputWidth/2 - 10, startY + 30, 25, 20).build());
        
        // Botón agregar aliado (restaurado a posición original)
        this.addAllyButton = this.addRenderableWidget(Button.builder(
            Component.translatable("gui.protectionblocks.management.add_ally_button"),
            this::addAlly
        ).bounds(centerX - buttonWidth - 5, startY + 60, buttonWidth, 20).build());
        
        // Botón remover aliado (restaurado a posición original)
        this.removeAllyButton = this.addRenderableWidget(Button.builder(
            Component.translatable("gui.protectionblocks.management.remove_ally_button"),
            this::removeAlly
        ).bounds(centerX + 5, startY + 60, buttonWidth, 20).build());
        
        // Checkbox para mostrar área protegida (restaurado a posición original)
        this.showProtectedAreaCheckbox = this.addRenderableWidget(new Checkbox(
            centerX + buttonWidth + 15, startY + 60, 140, 20,
            Component.translatable("gui.protectionblocks.management.show_area_button"),
            showProtectedArea,
            true // Mostrar label
        ));
        
        // Botón para cambiar color de protección (restaurado a posición original)
        this.colorButton = this.addRenderableWidget(Button.builder(
            getColorButtonText(selectedColorIndex),
            this::cycleColor
        ).bounds(centerX + buttonWidth + 160, startY + 60, 50, 20).build());
        
        // Cargar aliados existentes
        loadAllies();
        
        // Cargar jugadores disponibles
        loadAvailablePlayers();
    }
    
    private void loadAvailablePlayers() {
        availablePlayers.clear();
        if (Minecraft.getInstance().getConnection() != null) {
            // Obtener jugadores online usando getOnlinePlayers()
            availablePlayers = Minecraft.getInstance().getConnection().getOnlinePlayers()
                .stream()
                .map(playerInfo -> playerInfo.getProfile().getName())
                .filter(name -> !name.equals(blockEntity.getOwnerName())) // Excluir al propietario
                .filter(name -> blockEntity.getAllies().stream().noneMatch(ally -> ally.getPlayerName().equals(name))) // Excluir aliados existentes
                .sorted()
                .collect(Collectors.toList());
        }
    }
    
    private void togglePlayerDropdown(Button button) {
        // Toggle explícito del estado
        showPlayerDropdown = !showPlayerDropdown;
        
        if (showPlayerDropdown) {
            // Si se abre, cargar jugadores y cerrar el dropdown de permisos
            loadAvailablePlayers();
            showPermissionDropdown = false;
        } else {
            // Si se cierra, reset scroll
            playerDropdownScroll = 0;
        }
    }
    
    private void toggleProtectedAreaVisibility() {
        showProtectedArea = showProtectedAreaCheckbox.selected();
        // Aquí implementaremos la lógica para activar/desactivar el renderizado
        if (showProtectedArea) {
            // Activar renderizado de área protegida con color seleccionado
            ProtectionAreaRenderer.enableFor(blockPos, blockEntity.getProtectionRange(), selectedColorIndex);
        } else {
            // Desactivar renderizado de área protegida
            ProtectionAreaRenderer.disableFor(blockPos);
        }
    }
    
    private void cycleColor(Button button) {
        selectedColorIndex = (selectedColorIndex + 1) % ProtectionAreaRenderer.AVAILABLE_COLORS.length;
        
        // Actualizar texto del botón
        button.setMessage(getColorButtonText(selectedColorIndex));
        
        // Si está activa la visualización, actualizar el color
        if (showProtectedArea) {
            ProtectionAreaRenderer.enableFor(blockPos, blockEntity.getProtectionRange(), selectedColorIndex);
        }
    }
    
    private String getColorCode(int colorIndex) {
        // Códigos de color de Minecraft que corresponden a nuestros colores
        String[] colorCodes = {"b", "a", "c", "e", "d", "6", "3", "d"}; // azul, verde, rojo, amarillo, púrpura, naranja, cian, rosa
        return colorCodes[colorIndex % colorCodes.length];
    }
    
    private void loadAllies() {
        allyEntries.clear();
        List<AllyData> allies = blockEntity.getAllies();
        for (AllyData ally : allies) {
            allyEntries.add(new AllyListEntry(ally));
        }
    }
    
    // Método público para actualizar la GUI cuando se reciban datos del servidor
    public void refreshData() {
        loadAllies();
        loadAvailablePlayers();
    }
    
    private void addAlly(Button button) {
        String playerName = playerNameInput.getValue().trim();
        if (!playerName.isEmpty()) {
            NetworkHandler.sendToServer(new AddAllyPacket(blockPos, playerName));
            playerNameInput.setValue("");
            // La lista se actualizará cuando recibamos la respuesta del servidor
        }
    }
    
    private void removeAlly(Button button) {
        String playerName = playerNameInput.getValue().trim();
        if (!playerName.isEmpty()) {
            NetworkHandler.sendToServer(new RemoveAllyPacket(blockPos, playerName));
            playerNameInput.setValue("");
        }
    }
    
    private void saveZoneName(Button button) {
        String zoneName = zoneNameInput.getValue().trim();
        NetworkHandler.sendToServer(new UpdateWelcomeMessagePacket(blockPos, zoneName));
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fondo
        this.renderBackground(guiGraphics);
        
        // Renderizar lista de aliados
        renderAllyList(guiGraphics, mouseX, mouseY);
        
        // Renderizar información del bloque
        renderBlockInfo(guiGraphics, mouseX, mouseY);
        
        // Renderizar elementos base de la GUI (botones, campos de texto) ANTES de los dropdowns
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Renderizar dropdowns AL FINAL para que estén por encima de todo (incluyendo botones)
        // Primero el de jugadores
        if (showPlayerDropdown) {
            renderPlayerDropdown(guiGraphics, mouseX, mouseY);
        }
        
        // Después el de permisos (para que esté por encima del de jugadores si ambos están abiertos)
        if (showPermissionDropdown) {
            renderPermissionDropdown(guiGraphics, mouseX, mouseY);
        }
    }
    
    private void renderAllyList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int listY = Math.max(120, this.height / 4); // 25% desde arriba o mínimo 120px
        
        // Dimensiones responsivas
        int listWidth = Math.min(500, this.width - 60); // Máximo 500px, mínimo 60px de margen total
        int listHeight = Math.min(200, this.height / 2); // Máximo 200px o 50% de altura
        
        // Fondo del contenedor de aliados con colores sólidos
        guiGraphics.fill(centerX - listWidth/2, listY, centerX + listWidth/2, listY + listHeight, 0xFF000000);
        guiGraphics.fill(centerX - listWidth/2 + 1, listY + 1, centerX + listWidth/2 - 1, listY + listHeight - 1, 0xFF2A2A2A);
        
        // Título
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.current_members").getString(), centerX, listY + 10, 0xFFFFFF);
        
        // Área de scroll para aliados
        int entryStartY = listY + 30;
        int entryHeight = 25;
        int maxVisibleEntries = Math.max(4, (listHeight - 40) / entryHeight); // Mínimo 4 entradas visibles
        
        if (allyEntries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.no_allies_added").getString(), centerX, entryStartY + 40, 0xAAAAAA);
        } else {
            // Renderizar entradas de aliados con scroll
            for (int i = 0; i < Math.min(maxVisibleEntries, allyEntries.size() - scrollOffset); i++) {
                int index = i + scrollOffset;
                if (index < allyEntries.size()) {
                    AllyListEntry entry = allyEntries.get(index);
                    int entryY = entryStartY + (i * entryHeight);
                    
                    // Dimensiones responsivas para la entrada
                    int entryWidth = listWidth - 20;
                    int nameWidth = Math.max(80, entryWidth / 4);
                    int buttonWidth = Math.max(60, entryWidth / 6);
                    
                    // Fondo de la entrada
                    guiGraphics.fill(centerX - entryWidth/2, entryY, centerX + entryWidth/2, entryY + 20, 0xFF404040);
                    
                    // Nombre del aliado
                    int nameX = centerX - entryWidth/2 + 10;
                    guiGraphics.drawString(this.font, "§a" + entry.ally.getPlayerName(), nameX, entryY + 6, 0xFFFFFF);
                    
                    // Permisos actuales
                    String permissionText = getPermissionString(entry.ally);
                    int permX = centerX - entryWidth/2 + nameWidth + 10;
                    guiGraphics.drawString(this.font, "§7" + permissionText, permX, entryY + 6, 0xFFFFFF);
                    
                    // Botón de permisos (responsivo)
                    int permButtonX = centerX + entryWidth/2 - buttonWidth - 30;
                    int permButtonY = entryY;
                    int permButtonHeight = 20;
                    
                    boolean isHoveringPerm = mouseX >= permButtonX && mouseX <= permButtonX + buttonWidth && 
                                           mouseY >= permButtonY && mouseY <= permButtonY + permButtonHeight;
                    
                    guiGraphics.fill(permButtonX, permButtonY, permButtonX + buttonWidth, permButtonY + permButtonHeight, 
                                   isHoveringPerm ? 0xFF555555 : 0xFF333333);
                    guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.manage_button").getString(), permButtonX + buttonWidth/2, permButtonY + 6, 0xFFFFFF);
                    
                    // Botón de eliminar (responsivo)
                    int deleteButtonX = centerX + entryWidth/2 - 25;
                    int deleteButtonY = entryY;
                    int deleteButtonSize = 20;
                    
                    boolean isHoveringDelete = mouseX >= deleteButtonX && mouseX <= deleteButtonX + deleteButtonSize && 
                                             mouseY >= deleteButtonY && mouseY <= deleteButtonY + deleteButtonSize;
                    
                    guiGraphics.fill(deleteButtonX, deleteButtonY, deleteButtonX + deleteButtonSize, deleteButtonY + deleteButtonSize, 
                                   isHoveringDelete ? 0xFFAA0000 : 0xFF660000);
                    guiGraphics.drawCenteredString(this.font, "§c×", deleteButtonX + deleteButtonSize/2, deleteButtonY + 6, 0xFFFFFF);
                }
            }
            
            // Indicadores de scroll
            if (scrollOffset > 0) {
                guiGraphics.drawCenteredString(this.font, "§7▲ Más arriba", centerX, entryStartY - 10, 0xAAAAAA);
            }
            if (scrollOffset + maxVisibleEntries < allyEntries.size()) {
                guiGraphics.drawCenteredString(this.font, "§7▼ Más abajo", centerX, entryStartY + (maxVisibleEntries * entryHeight) + 5, 0xAAAAAA);
            }
        }
    }
    
    private void renderBlockInfo(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int listY = Math.max(120, this.height / 4); // Posición del contenedor de aliados
        int listHeight = Math.min(200, this.height / 2); // Altura del contenedor de aliados
        int infoY = listY + listHeight + 10; // Comenzar 10px después del contenedor de aliados
        
        // Título
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.block_info_title").getString(), centerX, infoY, 0xFFFFFF);
        
        // Información del propietario
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.block_info_owner", blockEntity.getOwnerName()).getString(), centerX, infoY + 15, 0xFFFFFF);
        
        // Información del rango
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.block_info_range", blockEntity.getProtectionRange()).getString(), centerX, infoY + 30, 0xFFFFFF);
        
        // Información de aliados
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.protectionblocks.management.block_info_allies", allyEntries.size()).getString(), centerX, infoY + 45, 0xFFFFFF);
        
        // Etiqueta para el nombre de zona (alineada a la izquierda con el input)
        int zoneInputY = listY + listHeight + 20;
        int listLeftX = centerX - Math.min(500, this.width - 60) / 2; // Misma posición que en initializeGUI
        guiGraphics.drawString(this.font, Component.translatable("gui.protectionblocks.management.zone_name_label").getString(), listLeftX + 10, zoneInputY - 15, 0xFFFFFF);
    }
    
    private String getPermissionString(AllyData ally) {
        List<String> perms = new ArrayList<>();
        if (ally.hasPermission(PermissionType.BUILD)) perms.add(Component.translatable("gui.protectionblocks.management.permissions.build_single").getString());
        if (ally.hasPermission(PermissionType.BREAK)) perms.add(Component.translatable("gui.protectionblocks.management.permissions.break").getString());
        if (ally.hasPermission(PermissionType.INTERACT)) perms.add(Component.translatable("gui.protectionblocks.management.permissions.interact_single").getString());
        
        return perms.isEmpty() ? Component.translatable("gui.protectionblocks.management.permissions.none").getString() : String.join(", ", perms);
    }
    
    private void togglePermissions(AllyData ally) {
        // Abrir lista desplegable de permisos en lugar de ciclar
        selectedAllyForPermissions = ally.getPlayerName();
        showPermissionDropdown = true;
        showPlayerDropdown = false; // Cerrar la otra lista si está abierta
        permissionDropdownScroll = 0;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click izquierdo
            int centerX = this.width / 2;
            
            // Verificar click en lista desplegable de permisos primero
            if (showPermissionDropdown) {
                int dropdownWidth = Math.min(220, this.width / 3);
                int dropdownX = Math.min(centerX + 60, this.width - dropdownWidth - 10);
                int dropdownY = Math.max(120, this.height / 5);
                int dropdownHeight = Math.min(maxVisiblePermissions, permissionOptions.size()) * 18 + 20;
                
                // Verificar click dentro del dropdown
                if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                    mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                    
                    for (int i = 0; i < Math.min(maxVisiblePermissions, permissionOptions.size() - permissionDropdownScroll); i++) {
                        int index = i + permissionDropdownScroll;
                        if (index < permissionOptions.size()) {
                            int optionY = dropdownY + 20 + (i * 18);
                            
                            if (mouseY >= optionY && mouseY <= optionY + 18) {
                                // Aplicar permisos seleccionados
                                applyPermissionOption(selectedAllyForPermissions, permissionOptions.get(index));
                                showPermissionDropdown = false;
                                return true;
                            }
                        }
                    }
                    return true; // Consumir el click si es dentro del dropdown
                } else {
                    // Click fuera del dropdown - cerrarlo
                    showPermissionDropdown = false;
                }
            }
            
            // Verificar click en lista desplegable de jugadores
            if (showPlayerDropdown) {
                int dropdownWidth = Math.min(160, this.width / 4);
                int dropdownX = Math.max(centerX - dropdownWidth/2, 10);
                int dropdownY = Math.max(100, this.height / 8);
                int dropdownHeight = Math.min(maxVisiblePlayers, availablePlayers.size()) * 18 + 20;
                
                // Verificar click dentro del dropdown
                if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                    mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                    
                    for (int i = 0; i < Math.min(maxVisiblePlayers, availablePlayers.size() - playerDropdownScroll); i++) {
                        int index = i + playerDropdownScroll;
                        if (index < availablePlayers.size()) {
                            int playerY = dropdownY + 20 + (i * 18);
                            
                            if (mouseY >= playerY && mouseY <= playerY + 18) {
                                // Seleccionar jugador
                                playerNameInput.setValue(availablePlayers.get(index));
                                showPlayerDropdown = false;
                                return true;
                            }
                        }
                    }
                    return true; // Consumir el click si es dentro del dropdown
                } else {
                    // Click fuera del dropdown - cerrarlo
                    showPlayerDropdown = false;
                }
            }
            
            // Si no hay dropdowns abiertos o se hizo click fuera, manejar clicks normales en la lista de aliados
            if (!showPermissionDropdown && !showPlayerDropdown) {
                int listY = Math.max(120, this.height / 4);
                int listStartY = listY + 30;
                int listWidth = Math.min(500, this.width - 60);
                int maxVisibleEntries = Math.max(4, (Math.min(200, this.height / 2) - 40) / 25);
                
                for (int i = 0; i < Math.min(maxVisibleEntries, allyEntries.size() - scrollOffset); i++) {
                    int index = i + scrollOffset;
                    if (index < allyEntries.size()) {
                        AllyListEntry entry = allyEntries.get(index);
                        int entryY = listStartY + (i * 25);
                        
                        // Dimensiones responsivas para botones
                        int entryWidth = listWidth - 20;
                        int buttonWidth = Math.max(60, entryWidth / 6);
                        
                        // Verificar click en botón de eliminar
                        int deleteButtonX = centerX + entryWidth/2 - 25;
                        int deleteButtonY = entryY;
                        int deleteButtonSize = 20;
                        
                        if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + deleteButtonSize && 
                            mouseY >= deleteButtonY && mouseY <= deleteButtonY + deleteButtonSize) {
                            // Eliminar aliado
                            NetworkHandler.sendToServer(new RemoveAllyPacket(blockPos, entry.ally.getPlayerName()));
                            return true;
                        }
                        
                        // Verificar click en botón de permisos
                        int permButtonX = centerX + entryWidth/2 - buttonWidth - 30;
                        int permButtonY = entryY;
                        int permButtonHeight = 20;
                        
                        if (mouseX >= permButtonX && mouseX <= permButtonX + buttonWidth && 
                            mouseY >= permButtonY && mouseY <= permButtonY + permButtonHeight) {
                            togglePermissions(entry.ally);
                            return true;
                        }
                    }
                }
            }
        }
        
        // Verificar click en checkbox de área protegida
        if (showProtectedAreaCheckbox.mouseClicked(mouseX, mouseY, button)) {
            toggleProtectedAreaVisibility();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int centerX = this.width / 2;
        
        // Scroll en lista desplegable de permisos
        if (showPermissionDropdown) {
            int dropdownWidth = Math.min(220, this.width / 3);
            int dropdownX = Math.min(centerX + 60, this.width - dropdownWidth - 10);
            int dropdownY = Math.max(120, this.height / 5);
            int dropdownHeight = Math.min(maxVisiblePermissions, permissionOptions.size()) * 18 + 20;
            
            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                if (permissionOptions.size() > maxVisiblePermissions) {
                    if (delta > 0 && permissionDropdownScroll > 0) {
                        permissionDropdownScroll--;
                        return true;
                    } else if (delta < 0 && permissionDropdownScroll + maxVisiblePermissions < permissionOptions.size()) {
                        permissionDropdownScroll++;
                        return true;
                    }
                }
                return true;
            }
        }
        
        // Scroll en lista desplegable de jugadores
        if (showPlayerDropdown) {
            int dropdownWidth = Math.min(160, this.width / 4);
            int dropdownX = Math.max(centerX - dropdownWidth/2, 10);
            int dropdownY = Math.max(100, this.height / 8);
            int dropdownHeight = Math.min(maxVisiblePlayers, availablePlayers.size()) * 18 + 20;
            
            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && 
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                if (availablePlayers.size() > maxVisiblePlayers) {
                    if (delta > 0 && playerDropdownScroll > 0) {
                        playerDropdownScroll--;
                        return true;
                    } else if (delta < 0 && playerDropdownScroll + maxVisiblePlayers < availablePlayers.size()) {
                        playerDropdownScroll++;
                        return true;
                    }
                }
                return true;
            }
        }
        
        // Scroll en lista de aliados
        int listY = Math.max(120, this.height / 4);
        int listWidth = Math.min(500, this.width - 60);
        int listHeight = Math.min(200, this.height / 2);
        int maxVisibleEntries = Math.max(4, (listHeight - 40) / 25);
        
        if (mouseX >= centerX - listWidth/2 && mouseX <= centerX + listWidth/2 && 
            mouseY >= listY && mouseY <= listY + listHeight) {
            if (allyEntries.size() > maxVisibleEntries) {
                if (delta > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (delta < 0 && scrollOffset + maxVisibleEntries < allyEntries.size()) {
                    scrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    public void refreshAllies() {
        loadAllies();
    }
    
    @Override
    public void onClose() {
        // NO desactivar automáticamente - solo si el usuario lo desactivó manualmente
        // La visualización persiste hasta que el usuario la desactive explícitamente
        super.onClose();
    }

    private static class AllyListEntry {
        public final AllyData ally;

        public AllyListEntry(AllyData ally) {
            this.ally = ally;
        }
    }

    private static class PermissionOption {
        public final String name;
        public final Set<PermissionType> permissions;

        public PermissionOption(String name, Set<PermissionType> permissions) {
            this.name = name;
            this.permissions = permissions;
        }
    }
}