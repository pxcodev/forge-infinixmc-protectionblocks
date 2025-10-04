package com.infinixmc.protectionblocks.network;

import com.infinixmc.protectionblocks.ProtectionBlocksMod;
import com.infinixmc.protectionblocks.network.packets.AddAllyPacket;
import com.infinixmc.protectionblocks.network.packets.ClearProtectionAreaPacket;
import com.infinixmc.protectionblocks.network.packets.OpenProtectionGUIPacket;
import com.infinixmc.protectionblocks.network.packets.RemoveAllyPacket;
import com.infinixmc.protectionblocks.network.packets.RequestSyncPacket;
import com.infinixmc.protectionblocks.network.packets.SyncProtectionDataPacket;
import com.infinixmc.protectionblocks.network.packets.UpdatePermissionsPacket;
import com.infinixmc.protectionblocks.network.packets.UpdateWelcomeMessagePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ProtectionBlocksMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void registerPackets() {
        INSTANCE.registerMessage(packetId++, OpenProtectionGUIPacket.class,
                OpenProtectionGUIPacket::encode,
                OpenProtectionGUIPacket::decode,
                OpenProtectionGUIPacket::handle);

        INSTANCE.registerMessage(packetId++, AddAllyPacket.class,
                AddAllyPacket::encode,
                AddAllyPacket::decode,
                AddAllyPacket::handle);

        INSTANCE.registerMessage(packetId++, RemoveAllyPacket.class,
                RemoveAllyPacket::encode,
                RemoveAllyPacket::decode,
                RemoveAllyPacket::handle);

        INSTANCE.registerMessage(packetId++, UpdatePermissionsPacket.class,
                UpdatePermissionsPacket::encode,
                UpdatePermissionsPacket::decode,
                UpdatePermissionsPacket::handle);

        INSTANCE.registerMessage(packetId++, SyncProtectionDataPacket.class,
                SyncProtectionDataPacket::encode,
                SyncProtectionDataPacket::decode,
                SyncProtectionDataPacket::handle);

        INSTANCE.registerMessage(packetId++, RequestSyncPacket.class,
                RequestSyncPacket::encode,
                RequestSyncPacket::decode,
                RequestSyncPacket::handle);

        INSTANCE.registerMessage(packetId++, ClearProtectionAreaPacket.class,
                ClearProtectionAreaPacket::encode,
                ClearProtectionAreaPacket::decode,
                ClearProtectionAreaPacket::handle);

        INSTANCE.registerMessage(packetId++, UpdateWelcomeMessagePacket.class,
                UpdateWelcomeMessagePacket::encode,
                UpdateWelcomeMessagePacket::decode,
                UpdateWelcomeMessagePacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}