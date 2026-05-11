package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneInfoCommand {
    private ZoneInfoCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("info")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            Optional<Zone> optionalZone = storage.findZone(id);

                            if (optionalZone.isEmpty()) {
                                CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                return 0;
                            }

                            Zone zone = optionalZone.get();

                            CommandFeedback.send(context.getSource(), "Zone: " + zone.id);
                            CommandFeedback.send(context.getSource(), "Enabled: " + zone.enabled);
                            CommandFeedback.send(context.getSource(), "Dimension: " + zone.dimension);
                            CommandFeedback.send(context.getSource(), "Min: " + zone.min.x + " " + zone.min.y + " " + zone.min.z);
                            CommandFeedback.send(context.getSource(), "Max: " + zone.max.x + " " + zone.max.y + " " + zone.max.z);
                            CommandFeedback.send(context.getSource(), "Mobs: " + zone.mobs.size());

                            if (zone.mobs.isEmpty()) {
                                CommandFeedback.send(context.getSource(), "No mob rules configured.");
                            } else {
                                for (MobRule mob : zone.mobs) {
                                    CommandFeedback.send(context.getSource(), "- " + mob.entity
                                            + " max=" + mob.maxAlive
                                            + " count=" + mob.spawnCount
                                            + " delay=" + mob.respawnSeconds
                                            + " chance=" + mob.chance);
                                }
                            }

                            return 1;
                        })
                );
    }
}