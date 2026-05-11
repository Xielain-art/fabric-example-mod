package com.gerbarium.regions.network;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.permission.PermissionUtil;
import com.gerbarium.regions.storage.ZoneStorage;
import com.gerbarium.regions.worldedit.WorldEditSelectionReader;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class GerbariumServerNetworking {
    private static final ZoneStorage STORAGE = new ZoneStorage();
    private static final int MAX_STRING_LENGTH = 262144;

    private GerbariumServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REQUEST_ZONES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) return;

                STORAGE.reload();
                sendZones(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REQUEST_ENTITIES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!hasAccess(player)) return;

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

        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.ADD_MOB_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleId = buf.readString(256);
            String entityIdRaw = buf.readString(256);
            int maxAlive = buf.readVarInt();
            int spawnCount = buf.readVarInt();
            int respawnSeconds = buf.readVarInt();
            double chance = buf.readDouble();

            server.execute(() -> {
                if (!hasAccess(player)) return;

                Identifier entityId = Identifier.tryParse(entityIdRaw);
                if (entityId == null) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid entity id: " + entityIdRaw);
                    return;
                }

                Registry<EntityType<?>> registry = server.getRegistryManager().get(RegistryKeys.ENTITY_TYPE);
                if (!registry.containsId(entityId)) {
                    CommandFeedback.error(player.getCommandSource(), "Unknown entity id: " + entityId);
                    return;
                }

                if (ruleId.isBlank()) {
                    CommandFeedback.error(player.getCommandSource(), "Rule ID cannot be empty.");
                    return;
                }

                if (maxAlive < 1 || spawnCount < 1 || respawnSeconds < 1 || chance < 0.0 || chance > 1.0) {
                    CommandFeedback.error(player.getCommandSource(), "Invalid mob rule values.");
                    return;
                }

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    return;
                }

                Zone zone = optionalZone.get();

                zone.mobs.removeIf(rule -> ruleId.equalsIgnoreCase(rule.id));
                zone.mobs.add(new MobRule(ruleId, entityId.toString(), maxAlive, spawnCount, respawnSeconds, chance));

                STORAGE.save();

                CommandFeedback.send(player.getCommandSource(), "Saved mob rule '" + ruleId + "' in zone '" + zoneId + "'.");
                sendZones(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.REMOVE_MOB_RULE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);
            String ruleId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) return;

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    return;
                }

                Zone zone = optionalZone.get();
                boolean removed = zone.mobs.removeIf(rule -> ruleId.equalsIgnoreCase(rule.id));

                if (!removed) {
                    CommandFeedback.error(player.getCommandSource(), "Mob rule not found: " + ruleId);
                    return;
                }

                STORAGE.save();

                CommandFeedback.send(player.getCommandSource(), "Removed mob rule '" + ruleId + "' from zone '" + zoneId + "'.");
                sendZones(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.TOGGLE_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) return;

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    return;
                }

                Zone zone = optionalZone.get();
                zone.enabled = !zone.enabled;

                STORAGE.save();

                CommandFeedback.send(player.getCommandSource(), "Zone '" + zoneId + "' is now " + (zone.enabled ? "enabled" : "disabled") + ".");
                sendZones(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GerbariumPackets.SELECT_ZONE, (server, player, handler, buf, responseSender) -> {
            String zoneId = buf.readString(256);

            server.execute(() -> {
                if (!hasAccess(player)) return;

                STORAGE.reload();

                Optional<Zone> optionalZone = STORAGE.findZone(zoneId);
                if (optionalZone.isEmpty()) {
                    CommandFeedback.error(player.getCommandSource(), "Zone not found: " + zoneId);
                    return;
                }

                try {
                    WorldEditSelectionReader.applySelection(player, optionalZone.get());
                    CommandFeedback.send(player.getCommandSource(), "Selected zone in WorldEdit: " + zoneId);
                } catch (Exception e) {
                    CommandFeedback.error(player.getCommandSource(), "Failed to select zone: " + e.getMessage());
                }
            });
        });
    }

    public static void openGui(ServerPlayerEntity player, String zoneId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(zoneId == null ? "" : zoneId, 256);

        ServerPlayNetworking.send(player, GerbariumPackets.OPEN_GUI, buf);
    }

    private static void sendZones(ServerPlayerEntity player) {
        PacketByteBuf response = PacketByteBufs.create();
        response.writeString(STORAGE.toJson(), MAX_STRING_LENGTH);

        ServerPlayNetworking.send(player, GerbariumPackets.SYNC_ZONES, response);
    }

    private static boolean hasAccess(ServerPlayerEntity player) {
        return PermissionUtil.hasAdminPermission(player.getCommandSource());
    }
}