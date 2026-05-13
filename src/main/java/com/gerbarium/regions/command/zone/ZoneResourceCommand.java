package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.ResourceRule;
import com.gerbarium.regions.model.WeightedBlock;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneResourceCommand {
    private ZoneResourceCommand() {}

    public static LiteralArgumentBuilder<ServerCommandSource> buildResourceRoot(ZoneStorage storage) {
        return literal("resource")
                .then(buildList(storage))
                .then(buildInfo(storage))
                .then(buildRemove(storage));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildList(ZoneStorage storage) {
        return literal("list")
                .then(argument("zone", StringArgumentType.word())
                        .executes(ctx -> {
                            String zoneId = StringArgumentType.getString(ctx, "zone");
                            storage.reload();
                            Optional<Zone> oz = storage.findZone(zoneId);
                            if (oz.isEmpty()) {
                                CommandFeedback.error(ctx.getSource(), "Zone not found: " + zoneId);
                                return 0;
                            }
                            Zone zone = oz.get();
                            if (zone.resources.isEmpty()) {
                                CommandFeedback.send(ctx.getSource(), "No resource rules in zone: " + zoneId);
                                return 1;
                            }
                            for (ResourceRule r : zone.resources) {
                                String name = r.name == null || r.name.isBlank() ? r.id : r.name;
                                String targets = r.targetBlocks == null ? "0" : String.valueOf(r.targetBlocks.size());
                                String res = r.resourceBlocks == null ? "0" : String.valueOf(r.resourceBlocks.size());
                                CommandFeedback.send(ctx.getSource(),
                                        "- " + name + " [" + r.id + "] " + (r.enabled ? "ON" : "OFF")
                                                + " | targets:" + targets + " resources:" + res
                                                + " max:" + r.maxActiveBlocks + " respawn:" + r.respawnSeconds + "s"
                                                + " chance:" + (int)(r.chance * 100) + "% " + r.activationMode);
                            }
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInfo(ZoneStorage storage) {
        return literal("info")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String zoneId = StringArgumentType.getString(ctx, "zone");
                                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                    storage.reload();
                                    Optional<Zone> oz = storage.findZone(zoneId);
                                    if (oz.isEmpty()) {
                                        CommandFeedback.error(ctx.getSource(), "Zone not found: " + zoneId);
                                        return 0;
                                    }
                                    Optional<ResourceRule> or = oz.get().resources.stream()
                                            .filter(r -> ruleId.equalsIgnoreCase(r.id)).findFirst();
                                    if (or.isEmpty()) {
                                        CommandFeedback.error(ctx.getSource(), "Resource rule not found: " + ruleId);
                                        return 0;
                                    }
                                    ResourceRule r = or.get();
                                    CommandFeedback.send(ctx.getSource(), "Resource Rule: " + (r.name == null ? r.id : r.name));
                                    CommandFeedback.send(ctx.getSource(), "ID: " + r.id + " | Enabled: " + r.enabled);
                                    CommandFeedback.send(ctx.getSource(), "Activation: " + r.activationMode + " | Chance: " + r.chance);
                                    CommandFeedback.send(ctx.getSource(), "Max Active: " + r.maxActiveBlocks + " | Spawn: " + r.spawnCount + " | Respawn: " + r.respawnSeconds + "s");
                                    CommandFeedback.send(ctx.getSource(), "Y Range: " + r.minY + " to " + r.maxY);
                                    CommandFeedback.send(ctx.getSource(), "Replace: " + r.replaceMode + " | Restore: " + r.restoreMode);
                                    CommandFeedback.send(ctx.getSource(), "Target blocks: " + (r.targetBlocks == null ? 0 : r.targetBlocks.size()));
                                    if (r.resourceBlocks != null) {
                                        for (WeightedBlock wb : r.resourceBlocks) {
                                            CommandFeedback.send(ctx.getSource(), "  - " + wb.block + " x" + wb.weight);
                                        }
                                    }
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRemove(ZoneStorage storage) {
        return literal("remove")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String zoneId = StringArgumentType.getString(ctx, "zone");
                                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                    storage.reload();
                                    Optional<Zone> oz = storage.findZone(zoneId);
                                    if (oz.isEmpty()) {
                                        CommandFeedback.error(ctx.getSource(), "Zone not found: " + zoneId);
                                        return 0;
                                    }
                                    Zone zone = oz.get();
                                    boolean removed = zone.resources.removeIf(r -> ruleId.equalsIgnoreCase(r.id));
                                    if (!removed) {
                                        CommandFeedback.error(ctx.getSource(), "Resource rule not found: " + ruleId);
                                        return 0;
                                    }
                                    storage.addZone(zone);
                                    CommandFeedback.send(ctx.getSource(), "Removed resource rule: " + ruleId);
                                    return 1;
                                })));
    }
}
