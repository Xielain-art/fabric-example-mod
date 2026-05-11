package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneMobCommand {
    private ZoneMobCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildMobRoot(ZoneStorage storage) {
        return literal("mob")
                .executes(context -> {
                    CommandFeedback.send(context.getSource(), "Gerbarium zone mob commands:");
                    CommandFeedback.send(context.getSource(), "/gerb zone mob add <zone> <ruleId> <entity> <maxAlive> <spawnCount> <respawnSeconds> <chance>");
                    CommandFeedback.send(context.getSource(), "/gerb zone mob remove <zone> <ruleId>");
                    CommandFeedback.send(context.getSource(), "/gerb zone mob list <zone>");
                    CommandFeedback.send(context.getSource(), "/gerb zone mob info <zone> <ruleId>");
                    return 1;
                })
                .then(buildAddMob(storage))
                .then(buildRemoveMob(storage))
                .then(buildListMobs(storage))
                .then(buildInfoMob(storage));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddMob(ZoneStorage storage) {
        return literal("add")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .then(argument("entity", IdentifierArgumentType.identifier())
                                        .then(argument("maxAlive", IntegerArgumentType.integer(1))
                                                .then(argument("spawnCount", IntegerArgumentType.integer(1))
                                                        .then(argument("respawnSeconds", IntegerArgumentType.integer(1))
                                                                .then(argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                        .executes(context -> {
                                                                            String zoneId = StringArgumentType.getString(context, "zone");
                                                                            String ruleId = StringArgumentType.getString(context, "ruleId");
                                                                            Identifier entityId = IdentifierArgumentType.getIdentifier(context, "entity");
                                                                            int maxAlive = IntegerArgumentType.getInteger(context, "maxAlive");
                                                                            int spawnCount = IntegerArgumentType.getInteger(context, "spawnCount");
                                                                            int respawnSeconds = IntegerArgumentType.getInteger(context, "respawnSeconds");
                                                                            double chance = DoubleArgumentType.getDouble(context, "chance");

                                                                            Optional<Zone> optionalZone = storage.findZone(zoneId);

                                                                            if (optionalZone.isEmpty()) {
                                                                                CommandFeedback.error(context.getSource(), "Zone not found: " + zoneId);
                                                                                return 0;
                                                                            }

                                                                            Zone zone = optionalZone.get();

                                                                            if (zone.mobs == null) {
                                                                                CommandFeedback.error(context.getSource(), "Zone mob list is null. Reload or recreate zone.");
                                                                                return 0;
                                                                            }

                                                                            zone.mobs.removeIf(mob -> ruleId.equalsIgnoreCase(mob.id));
                                                                            zone.mobs.add(new MobRule(ruleId, entityId.toString(), maxAlive, spawnCount, respawnSeconds, chance));

                                                                            storage.save();

                                                                            CommandFeedback.send(context.getSource(), "Added mob rule '" + ruleId + "' to zone '" + zoneId + "'.");
                                                                            CommandFeedback.send(context.getSource(), "Entity: " + entityId);
                                                                            return 1;
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRemoveMob(ZoneStorage storage) {
        return literal("remove")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .executes(context -> {
                                    String zoneId = StringArgumentType.getString(context, "zone");
                                    String ruleId = StringArgumentType.getString(context, "ruleId");

                                    Optional<Zone> optionalZone = storage.findZone(zoneId);

                                    if (optionalZone.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "Zone not found: " + zoneId);
                                        return 0;
                                    }

                                    Zone zone = optionalZone.get();

                                    if (zone.mobs == null || zone.mobs.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "No mob rules in zone: " + zoneId);
                                        return 0;
                                    }

                                    boolean removed = zone.mobs.removeIf(mob -> ruleId.equalsIgnoreCase(mob.id));

                                    if (!removed) {
                                        CommandFeedback.error(context.getSource(), "Mob rule not found: " + ruleId);
                                        return 0;
                                    }

                                    storage.save();

                                    CommandFeedback.send(context.getSource(), "Removed mob rule '" + ruleId + "' from zone '" + zoneId + "'.");
                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildListMobs(ZoneStorage storage) {
        return literal("list")
                .then(argument("zone", StringArgumentType.word())
                        .executes(context -> {
                            String zoneId = StringArgumentType.getString(context, "zone");
                            Optional<Zone> optionalZone = storage.findZone(zoneId);

                            if (optionalZone.isEmpty()) {
                                CommandFeedback.error(context.getSource(), "Zone not found: " + zoneId);
                                return 0;
                            }

                            Zone zone = optionalZone.get();

                            if (zone.mobs == null || zone.mobs.isEmpty()) {
                                CommandFeedback.send(context.getSource(), "No mob rules in zone: " + zoneId);
                                return 1;
                            }

                            CommandFeedback.send(context.getSource(), "Mob rules in zone '" + zoneId + "':");

                            for (MobRule mob : zone.mobs) {
                                CommandFeedback.send(context.getSource(), "- " + safeRuleId(mob)
                                        + ": " + mob.entity
                                        + " max=" + mob.maxAlive
                                        + " count=" + mob.spawnCount
                                        + " delay=" + mob.respawnSeconds
                                        + " chance=" + mob.chance);
                            }

                            return zone.mobs.size();
                        })
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInfoMob(ZoneStorage storage) {
        return literal("info")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .executes(context -> {
                                    String zoneId = StringArgumentType.getString(context, "zone");
                                    String ruleId = StringArgumentType.getString(context, "ruleId");

                                    Optional<Zone> optionalZone = storage.findZone(zoneId);

                                    if (optionalZone.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "Zone not found: " + zoneId);
                                        return 0;
                                    }

                                    Zone zone = optionalZone.get();

                                    if (zone.mobs == null || zone.mobs.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "No mob rules in zone: " + zoneId);
                                        return 0;
                                    }

                                    Optional<MobRule> optionalRule = zone.mobs.stream()
                                            .filter(rule -> ruleId.equalsIgnoreCase(rule.id))
                                            .findFirst();

                                    if (optionalRule.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "Mob rule not found: " + ruleId);
                                        return 0;
                                    }

                                    MobRule mob = optionalRule.get();

                                    CommandFeedback.send(context.getSource(), "Mob rule: " + safeRuleId(mob));
                                    CommandFeedback.send(context.getSource(), "Entity: " + mob.entity);
                                    CommandFeedback.send(context.getSource(), "Max alive: " + mob.maxAlive);
                                    CommandFeedback.send(context.getSource(), "Spawn count: " + mob.spawnCount);
                                    CommandFeedback.send(context.getSource(), "Respawn seconds: " + mob.respawnSeconds);
                                    CommandFeedback.send(context.getSource(), "Chance: " + mob.chance);

                                    return 1;
                                })
                        )
                );
    }

    private static String safeRuleId(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            return "legacy_" + rule.entity.replace(':', '_');
        }

        return rule.id;
    }
}