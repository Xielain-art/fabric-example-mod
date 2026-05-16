package com.gerbarium.regions.network;

import com.gerbarium.regions.GerbariumRegionsBridge;
import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import com.gerbarium.regions.model.WeightedBlock;
import com.google.gson.Gson;
import com.gerbarium.regions.permission.PermissionUtil;
import com.gerbarium.regions.runtime.BridgeRuntimeReloadDispatcher;
import com.gerbarium.regions.storage.ZoneStorage;
import com.gerbarium.regions.worldedit.WorldEditSelectionReader;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class GerbariumServerNetworking {
    private static final Gson GSON = new Gson();
    private static final ZoneStorage STORAGE = ZoneStorage.getInstance();
    private static final int MAX_STRING_LENGTH = 1_048_576;

    private GerbariumServerNetworking() {
    }

    public static void register() {
        registerRequestZones();
        registerRequestEntities();
        registerRequestBlocks();
        registerAddMobRule();
        registerRemoveMobRule();
        registerUpdateZoneSettings();
        registerAddResourceRule();
        registerUpdateResourceRule();
        registerRemoveResourceRule();
        registerToggleZone();
        registerSelectZone();
        registerDeselectZone();
        registerDeleteZone();
        registerTpToZone();
    }

    private static void registerRequestZones() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REQUEST_ZONES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();
                sendZones(player);
            });
        });
    }

    private static void registerRequestEntities() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REQUEST_ENTITIES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                Registry<EntityType<?>> registry = server.getRegistryManager().get(RegistryKeys.ENTITY_TYPE);

                List<String> ids = new ArrayList<>();

                for (Identifier id : registry.getIds()) {
                    ids.add(id.toString());
                }

                ids.sort(Comparator.naturalOrder());

                PacketByteBuf response = PacketByteBufs.create();
                response.writeVarInt(ids.size());

                for (String id : ids) {
                    response.writeString(id, 512);
                }

                ServerPlayNetworking.send(player, GerbariumPackets.SYNC_ENTITIES, response);
            });
        });
    }

    private static void registerRequestBlocks() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REQUEST_BLOCKS, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                Registry<Block> registry = server.getRegistryManager().get(RegistryKeys.BLOCK);
                List<String> ids = new ArrayList<>();

                for (Identifier id : registry.getIds()) {
                    ids.add(id.toString());
                }

                ids.sort(Comparator.naturalOrder());

                PacketByteBuf response = PacketByteBufs.create();
                response.writeVarInt(ids.size());

                for (String id : ids) {
                    response.writeString(id, 512);
                }

                ServerPlayNetworking.send(player, GerbariumPackets.SYNC_BLOCKS, response);
            });
        });
    }

    private static void registerAddMobRule() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.ADD_MOB_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String mobRuleJson = buf.readString(MAX_STRING_LENGTH);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                MobRule incoming = GSON.fromJson(mobRuleJson, MobRule.class);
                if (incoming == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid mob rule payload.");
                    return;
                }

                Identifier entityId = Identifier.tryParse(incoming.entity);

                if (entityId == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid entity id: " + incoming.entity);
                    return;
                }

                Registry<EntityType<?>> registry = server.getRegistryManager().get(RegistryKeys.ENTITY_TYPE);

                if (!registry.containsId(entityId)) {
                    CommandFeedback.error(player.getCommandSource(), "Unknown entity id: " + entityId);
                    return;
                }

                try {
                    ZoneDefaults.validateMobRule(incoming);
                    ZoneDefaults.normalizeMobRule(incoming);
                } catch (IllegalArgumentException e) {
                    CommandFeedback.error(player.getCommandSource(), e.getMessage());
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);

                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();

                if (zone.mobs == null) {
                    CommandFeedback.error(player.getCommandSource(), "Zone mob list is null. Reload or recreate zone.");
                    sendZones(player);
                    return;
                }

                incoming.entity = entityId.toString();
                zone.mobs.removeIf(rule -> incoming.id.equalsIgnoreCase(rule.id));
                zone.mobs.add(incoming);

                STORAGE.addZone(zone);
                STORAGE.reload();
                BridgeRuntimeReloadDispatcher.reloadIfPresent();

                GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Added mob rule: zone={} rule={} entity={}", zoneId, incoming.id, incoming.entity);
                CommandFeedback.send(player.getCommandSource(), "Saved mob rule '" + incoming.id + "' in zone '" + zoneId + "'.");
                BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                sendZones(player);
            });
        });
    }

    private static void registerUpdateZoneSettings() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.UPDATE_ZONE_SETTINGS, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            int activationRange = buf.readVarInt();
            int deactivateAfter = buf.readVarInt();
            int firstSpawnDelay = buf.readVarInt();
            int reactivationCooldown = buf.readVarInt();
            int minDistance = buf.readVarInt();
            int maxDistance = buf.readVarInt();
            int maxPositionAttempts = buf.readVarInt();
            boolean requireLoadedChunk = buf.readBoolean();
            boolean respectVanilla = buf.readBoolean();

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }
                STORAGE.reload();
                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }
                Zone zone = optionalZone.get();
                ZoneDefaults.normalizeZone(zone);
                zone.activation.range = activationRange;
                zone.activation.deactivateAfterSeconds = deactivateAfter;
                zone.activation.firstSpawnDelaySeconds = firstSpawnDelay;
                zone.activation.reactivationCooldownSeconds = reactivationCooldown;
                zone.spawn.minDistanceFromPlayer = minDistance;
                zone.spawn.maxDistanceFromPlayer = maxDistance;
                zone.spawn.maxPositionAttempts = maxPositionAttempts;
                zone.spawn.requireLoadedChunk = requireLoadedChunk;
                zone.spawn.respectVanillaSpawnRules = respectVanilla;
                try {
                    ZoneDefaults.validateZone(zone);
                } catch (IllegalArgumentException e) {
                    CommandFeedback.error(player.getCommandSource(), e.getMessage());
                    return;
                }
                STORAGE.addZone(zone);
                STORAGE.reload();
                BridgeRuntimeReloadDispatcher.reloadIfPresent();
                BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                sendZones(player);
            });
        });
    }

    private static void registerRemoveMobRule() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REMOVE_MOB_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);

                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();

                if (zone.mobs == null || zone.mobs.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "No mob rules in zone: " + zoneId);
                    sendZones(player);
                    return;
                }

                boolean removed = zone.mobs.removeIf(rule -> ruleId.equalsIgnoreCase(rule.id));

                if (!removed) {
                    CommandFeedback.error(player.getCommandSource(), "Mob rule not found: " + ruleId);
                    sendZones(player);
                    return;
                }

                try {
                    STORAGE.addZone(zone);
                    STORAGE.reload();
                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                } catch (RuntimeException e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to save zone changes: " + e.getMessage());
                    sendZones(player);
                    return;
                }

                GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Removed mob rule: zone={} rule={}", zoneId, ruleId);
                CommandFeedback.send(player.getCommandSource(), "Removed mob rule '" + ruleId + "' from zone '" + zoneId + "'.");
                BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                sendZones(player);
            });
        });
    }

    private static void registerToggleZone() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.TOGGLE_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);

                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();
                zone.enabled = !zone.enabled;

                try {
                    STORAGE.addZone(zone);
                    STORAGE.reload();
                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                } catch (RuntimeException e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to save zone changes: " + e.getMessage());
                    sendZones(player);
                    return;
                }

                CommandFeedback.send(player.getCommandSource(), "Zone '" + zoneId + "' is now " + (zone.enabled ? "enabled" : "disabled") + ".");
                BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                sendZones(player);
            });
        });
    }

    private static void registerSelectZone() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.SELECT_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);

                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();
                String playerDimension = player.getServerWorld()
                        .getRegistryKey()
                        .getValue()
                        .toString();

                if (!zone.dimension.equals(playerDimension)) {
                    CommandFeedback.error(
                            player.getCommandSource(),
                            "Zone is in dimension '" + zone.dimension + "', but you are in '" + playerDimension + "'."
                    );
                    return;
                }

                try {
                    if (!WorldEditSelectionReader.isAvailable()) {
                        CommandFeedback.error(player.getCommandSource(), "WorldEdit mod is not loaded. Zone selection sync is unavailable.");
                        return;
                    }
                    WorldEditSelectionReader.clearSelection(player);
                    WorldEditSelectionReader.applySelection(player, zone);

                    GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Selected zone={} for player={}", zoneId, player.getName().getString());
                    CommandFeedback.send(player.getCommandSource(), "Selected zone in WorldEdit: " + zoneId);
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to select zone: " + e.getMessage());
                }
            });
        });
    }

    private static void registerDeselectZone() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.DESELECT_ZONE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                try {
                    if (!WorldEditSelectionReader.isAvailable()) {
                        CommandFeedback.error(player.getCommandSource(), "WorldEdit mod is not loaded. Zone selection sync is unavailable.");
                        return;
                    }
                    WorldEditSelectionReader.clearSelection(player);
                    CommandFeedback.send(player.getCommandSource(), "WorldEdit selection cleared.");
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to clear selection: " + e.getMessage());
                }
            });
        });
    }

    private static void registerDeleteZone() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.DELETE_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();

                boolean deleted = STORAGE.deleteZone(zoneId);

                if (!deleted) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                STORAGE.reload();
                BridgeRuntimeReloadDispatcher.reloadIfPresent();

                CommandFeedback.send(player.getCommandSource(), "Deleted zone: " + zoneId);
                BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                sendZones(player);
            });
        });
    }

    private static void registerTpToZone() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.TP_TO_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);

                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();

                Identifier dimensionId = Identifier.tryParse(zone.dimension);

                if (dimensionId == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid zone dimension: " + zone.dimension);
                    return;
                }

                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
                ServerWorld world = server.getWorld(worldKey);

                if (world == null) {
                    CommandFeedback.error(player.getCommandSource(), "World not loaded: " + zone.dimension);
                    return;
                }

                double x = (zone.min.x + zone.max.x) / 2.0 + 0.5;
                double y = zone.max.y + 2.0;
                double z = (zone.min.z + zone.max.z) / 2.0 + 0.5;

                player.teleport(world, x, y, z, player.getYaw(), player.getPitch());

                CommandFeedback.send(player.getCommandSource(), "Teleported to zone: " + zone.id);
            });
        });
    }

    private static void registerAddResourceRule() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.ADD_RESOURCE_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleJson = buf.readString(MAX_STRING_LENGTH);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                ResourceRule incoming = GSON.fromJson(ruleJson, ResourceRule.class);
                if (incoming == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid resource rule payload.");
                    return;
                }

                try {
                    ZoneDefaults.normalizeResourceRule(incoming);
                    ZoneDefaults.validateResourceRule(incoming);
                    validateResourceRuleBlocks(server.getRegistryManager().get(RegistryKeys.BLOCK), incoming);
                } catch (IllegalArgumentException e) {
                    CommandFeedback.error(player.getCommandSource(), e.getMessage());
                    return;
                }

                STORAGE.reload();
                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();
                zone.resources.removeIf(r -> r.id != null && r.id.equalsIgnoreCase(incoming.id));
                zone.resources.add(incoming);

                try {
                    STORAGE.addZone(zone);
                    STORAGE.reload();
                    CommandFeedback.send(player.getCommandSource(), "Added resource rule: " + incoming.id);
                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                    BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to save: " + e.getMessage());
                }

                sendZones(player);
            });
        });
    }

    private static void registerUpdateResourceRule() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.UPDATE_RESOURCE_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleJson = buf.readString(MAX_STRING_LENGTH);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                ResourceRule incoming = GSON.fromJson(ruleJson, ResourceRule.class);
                if (incoming == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid resource rule payload.");
                    return;
                }

                try {
                    ZoneDefaults.normalizeResourceRule(incoming);
                    ZoneDefaults.validateResourceRule(incoming);
                    validateResourceRuleBlocks(server.getRegistryManager().get(RegistryKeys.BLOCK), incoming);
                } catch (IllegalArgumentException e) {
                    CommandFeedback.error(player.getCommandSource(), e.getMessage());
                    return;
                }

                STORAGE.reload();
                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();
                zone.resources.removeIf(r -> r.id != null && r.id.equalsIgnoreCase(incoming.id));
                zone.resources.add(incoming);

                try {
                    STORAGE.addZone(zone);
                    STORAGE.reload();
                    CommandFeedback.send(player.getCommandSource(), "Updated resource rule: " + incoming.id);
                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                    BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to save: " + e.getMessage());
                }

                sendZones(player);
            });
        });
    }

    private static void registerRemoveResourceRule() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REMOVE_RESOURCE_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) {
                    return;
                }

                STORAGE.reload();
                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    sendZones(player);
                    return;
                }

                Zone zone = optionalZone.get();
                boolean removed = zone.resources.removeIf(r -> ruleId.equalsIgnoreCase(r.id));

                if (!removed) {
                    CommandFeedback.error(player.getCommandSource(), "Resource rule not found: " + ruleId);
                    sendZones(player);
                    return;
                }

                try {
                    STORAGE.addZone(zone);
                    STORAGE.reload();
                    CommandFeedback.send(player.getCommandSource(), "Removed resource rule: " + ruleId);
                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                    BridgeRuntimeReloadDispatcher.sendSavedHint(player.getCommandSource());
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to save: " + e.getMessage());
                }

                sendZones(player);
            });
        });
    }

    public static void openGui(ServerPlayerEntity player, String zoneId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId == null ? "" : zoneId, 256);

        ServerPlayNetworking.send(player, GerbariumPackets.OPEN_GUI, buf);
    }

    private static final int SAFE_PACKET_STRING = 16000;

    private static void sendZones(ServerPlayerEntity player) {
        String json = STORAGE.toJson();
        int payloadSize = json.length();
        GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Sync zones to client: player={} zones={} payloadSize={}",
                player.getName().getString(),
                STORAGE.getData().zones.size(),
                payloadSize);

        PacketByteBuf response = PacketByteBufs.create();
        if (payloadSize > SAFE_PACKET_STRING) {
            GerbariumRegionsBridge.LOGGER.warn("[GerbariumBridge] Zone sync payload too large ({}), truncating to safe limit", payloadSize);
            response.writeString(json.substring(0, SAFE_PACKET_STRING), SAFE_PACKET_STRING);
        } else {
            response.writeString(json, MAX_STRING_LENGTH);
        }
        ServerPlayNetworking.send(player, GerbariumPackets.SYNC_ZONES, response);
    }

    private static boolean hasAccess(ServerPlayerEntity player) {
        return PermissionUtil.hasAdminPermission(player.getCommandSource());
    }

    private static void validateResourceRuleBlocks(Registry<Block> blockRegistry, ResourceRule rule) {
        for (String targetBlock : rule.targetBlocks) {
            validateExistingBlock(blockRegistry, targetBlock, "targetBlocks");
        }
        for (WeightedBlock resourceBlock : rule.resourceBlocks) {
            validateExistingBlock(blockRegistry, resourceBlock.block, "resourceBlocks");
        }
    }

    private static void validateExistingBlock(Registry<Block> blockRegistry, String blockIdText, String fieldName) {
        Identifier blockId = Identifier.tryParse(blockIdText);
        if (blockId == null) {
            throw new IllegalArgumentException("Invalid " + fieldName + " id: " + blockIdText);
        }
        if (!blockRegistry.containsId(blockId)) {
            throw new IllegalArgumentException("Unknown " + fieldName + " id: " + blockId);
        }
    }
}
