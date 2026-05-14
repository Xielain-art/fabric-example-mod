package com.gerbarium.regions.client.network;

import com.gerbarium.regions.client.data.ClientGerbariumData;
import com.gerbarium.regions.client.screen.GerbariumRefreshableScreen;
import com.gerbarium.regions.client.screen.RegionsScreen;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.network.GerbariumPackets;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class GerbariumClientNetworking {
    private static final Gson GSON = new Gson();
    private static final int MAX_STRING_LENGTH = 1_048_576;
    private static String preferredZoneId = "";
    private static volatile boolean openGuiAfterZonesSync = false;

    private GerbariumClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.OPEN_GUI, (client, handler, buf, responseSender) -> {
            preferredZoneId = normalizeZoneId(buf.readString(256));
            openGuiAfterZonesSync = true;

            client.execute(() -> {
                requestEntities();
                requestBlocks();
                requestZones();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.SYNC_ZONES, (client, handler, buf, responseSender) -> {
            String json = buf.readString(MAX_STRING_LENGTH);

            client.execute(() -> {
                ClientGerbariumData.setZonesJson(json);
                if (openGuiAfterZonesSync) {
                    openGuiAfterZonesSync = false;
                    MinecraftClient.getInstance().setScreen(new RegionsScreen(preferredZoneId));
                    return;
                }

                if (MinecraftClient.getInstance().currentScreen instanceof GerbariumRefreshableScreen refreshable) {
                    refreshable.refreshFromSync();
                }
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

        ClientPlayNetworking.registerGlobalReceiver(GerbariumPackets.SYNC_BLOCKS, (client, handler, buf, responseSender) -> {
            int count = buf.readVarInt();
            List<String> ids = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                ids.add(buf.readString(512));
            }

            client.execute(() -> ClientGerbariumData.setBlockIds(ids));
        });
    }

    public static void requestZones() {
        ClientPlayNetworking.send(GerbariumPackets.REQUEST_ZONES, PacketByteBufs.empty());
    }

    public static void requestEntities() {
        ClientPlayNetworking.send(GerbariumPackets.REQUEST_ENTITIES, PacketByteBufs.empty());
    }

    public static void requestBlocks() {
        ClientPlayNetworking.send(GerbariumPackets.REQUEST_BLOCKS, PacketByteBufs.empty());
    }

    public static void addMobRule(String zoneId, MobRule rule) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);
        buf.writeString(GSON.toJson(rule), MAX_STRING_LENGTH);

        ClientPlayNetworking.send(GerbariumPackets.ADD_MOB_RULE, buf);
    }

    public static void sendUpdateZoneSettings(
            String zoneId,
            int activationRange,
            int deactivateAfterSeconds,
            int firstSpawnDelaySeconds,
            int reactivationCooldownSeconds,
            int minDistanceFromPlayer,
            int maxDistanceFromPlayer,
            int maxPositionAttempts,
            boolean requireLoadedChunk,
            boolean respectVanillaSpawnRules
    ) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId, 256);
        buf.writeVarInt(activationRange);
        buf.writeVarInt(deactivateAfterSeconds);
        buf.writeVarInt(firstSpawnDelaySeconds);
        buf.writeVarInt(reactivationCooldownSeconds);
        buf.writeVarInt(minDistanceFromPlayer);
        buf.writeVarInt(maxDistanceFromPlayer);
        buf.writeVarInt(maxPositionAttempts);
        buf.writeBoolean(requireLoadedChunk);
        buf.writeBoolean(respectVanillaSpawnRules);
        ClientPlayNetworking.send(GerbariumPackets.UPDATE_ZONE_SETTINGS, buf);
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

    public static void deselectZone() {
        ClientPlayNetworking.send(GerbariumPackets.DESELECT_ZONE, PacketByteBufs.empty());
    }

    public static void deleteZone(String zoneId) {
        preferredZoneId = "";

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId, 256);

        ClientPlayNetworking.send(GerbariumPackets.DELETE_ZONE, buf);
    }

    public static void tpToZone(String zoneId) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(zoneId, 256);

        ClientPlayNetworking.send(GerbariumPackets.TP_TO_ZONE, buf);
    }

    public static void addResourceRule(String zoneId, ResourceRule rule) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId, 256);
        buf.writeString(GSON.toJson(rule), MAX_STRING_LENGTH);
        ClientPlayNetworking.send(GerbariumPackets.ADD_RESOURCE_RULE, buf);
    }

    public static void updateResourceRule(String zoneId, ResourceRule rule) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId, 256);
        buf.writeString(GSON.toJson(rule), MAX_STRING_LENGTH);
        ClientPlayNetworking.send(GerbariumPackets.UPDATE_RESOURCE_RULE, buf);
    }

    public static void removeResourceRule(String zoneId, String ruleId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId, 256);
        buf.writeString(ruleId, 256);
        ClientPlayNetworking.send(GerbariumPackets.REMOVE_RESOURCE_RULE, buf);
    }

    public static void setPreferredZoneId(String zoneId) {
        preferredZoneId = normalizeZoneId(zoneId);
    }

    private static String normalizeZoneId(String zoneId) {
        if (zoneId == null) {
            return "";
        }
        String normalized = zoneId.trim();
        return normalized.equalsIgnoreCase("null") ? "" : normalized;
    }
}

