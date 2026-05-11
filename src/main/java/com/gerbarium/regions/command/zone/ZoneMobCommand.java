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

    public static LiteralArgumentBuilder<ServerCommandSource> buildAddMob(ZoneStorage storage) {
        return literal("addmob")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("entity", IdentifierArgumentType.identifier())
                                .then(argument("maxAlive", IntegerArgumentType.integer(1))
                                        .then(argument("spawnCount", IntegerArgumentType.integer(1))
                                                .then(argument("respawnSeconds", IntegerArgumentType.integer(1))
                                                        .then(argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                .executes(context -> {
                                                                    String zoneId = StringArgumentType.getString(context, "zone");
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

                                                                    zone.mobs.removeIf(mob -> mob.entity.equals(entityId.toString()));
                                                                    zone.mobs.add(new MobRule(entityId.toString(), maxAlive, spawnCount, respawnSeconds, chance));

                                                                    storage.save();

                                                                    CommandFeedback.send(context.getSource(), "Added mob rule to zone '" + zoneId + "': " + entityId);
                                                                    CommandFeedback.send(context.getSource(), "maxAlive=" + maxAlive
                                                                            + ", spawnCount=" + spawnCount
                                                                            + ", respawnSeconds=" + respawnSeconds
                                                                            + ", chance=" + chance);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildRemoveMob(ZoneStorage storage) {
        return literal("removemob")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("entity", IdentifierArgumentType.identifier())
                                .executes(context -> {
                                    String zoneId = StringArgumentType.getString(context, "zone");
                                    Identifier entityId = IdentifierArgumentType.getIdentifier(context, "entity");

                                    Optional<Zone> optionalZone = storage.findZone(zoneId);

                                    if (optionalZone.isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "Zone not found: " + zoneId);
                                        return 0;
                                    }

                                    Zone zone = optionalZone.get();
                                    boolean removed = zone.mobs.removeIf(mob -> mob.entity.equals(entityId.toString()));

                                    if (!removed) {
                                        CommandFeedback.error(context.getSource(), "Mob rule not found in zone '" + zoneId + "': " + entityId);
                                        return 0;
                                    }

                                    storage.save();

                                    CommandFeedback.send(context.getSource(), "Removed mob rule from zone '" + zoneId + "': " + entityId);
                                    return 1;
                                })
                        )
                );
    }
}