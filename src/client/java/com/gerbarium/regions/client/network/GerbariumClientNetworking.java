package com.gerbarium.regions.client.network;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.screen.RegionsScreen;
import com.gerbarium.regions.network.GerbariumPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class GerbariumClientNetworking {
    private static final int MAX_STRING_LENGTH = 262144;
    private static String preferredZoneId = "";

    private GerbariumClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.OPEN_GUI, (client, handler, buf, responseSender) -> {
            preferredZoneId = buf.readString(256);

            client.execute(() -> {
                requestEntities();
                requestZones();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.SYNC_ZONES, (client, handler, buf, responseSender) -> {
            String json = buf.readString(MAX_STRING_LENGTH);

            client.execute(() -> {
                ClientGerbariumData.setZonesJson(json);
                MinecraftClient.getInstance().setScreen(new RegionsScreen(preferredZoneId));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.SYNC_ENTITIES, (client, handler, buf, responseSender) -> {
            int count = buf.readVarInt();
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                ids.add(buf.readString(512));
            }

            client.execute(() -> ClientGerbariumData.setEntityIds(ids));
        });
    }

    public static void requestZones() {
        ClientPlayNetworking.send(GerbariumPackets.REQUEST_ZONES, PacketByteBufs.empty());
    }

    public static void requestEntities() {
        ClientPlayNetworking.send(GerbariumPackets.REQUEST_ENTITIES, PacketByteBufs.empty());
    }

    public static void addMobRule(String zoneId, String ruleId, String entityId, int maxAlive, int spawnCount, int respawnSeconds, double chance) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);
        buf.writeString(ruleId, 256);
        buf.writeString(entityId, 256);
        buf.writeVarInt(maxAlive);
        buf.writeVarInt(spawnCount);
        buf.writeVarInt(respawnSeconds);
        buf.writeDouble(chance);

        ClientPlayNetworking.send(GerbariumPackets.ADD_MOB_RULE, buf);
    }

    public static void removeMobRule(String zoneId, String ruleId) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);
        buf.writeString(ruleId, 256);

        ClientPlayNetworking.send(GerbariumPackets.REMOVE_MOB_RULE, buf);
    }

    public static void toggleZone(String zoneId) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);

        ClientPlayNetworking.send(GerbariumPackets.TOGGLE_ZONE, buf);
    }

    public static void selectZone(String zoneId) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);

        ClientPlayNetworking.send(GerbariumPackets.SELECT_ZONE, buf);
    }
}