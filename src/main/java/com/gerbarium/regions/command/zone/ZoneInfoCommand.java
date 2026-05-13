package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
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
                            ZoneDefaults.normalizeZone(zone);

                            CommandFeedback.send(context.getSource(), "Zone: " + zone.id);
                            CommandFeedback.send(context.getSource(), "Enabled: " + zone.enabled);
                            CommandFeedback.send(context.getSource(), "Dimension: " + zone.dimension);
                            CommandFeedback.send(context.getSource(), "Min: " + zone.min.x + " " + zone.min.y + " " + zone.min.z);
                            CommandFeedback.send(context.getSource(), "Max: " + zone.max.x + " " + zone.max.y + " " + zone.max.z);
                            CommandFeedback.send(context.getSource(), "Mobs: " + zone.mobs.size());
                            CommandFeedback.send(context.getSource(), "Activation range=" + zone.activation.range + ", deactivateAfter=" + zone.activation.deactivateAfterSeconds + "s");
                            CommandFeedback.send(context.getSource(), "Spawn distance=" + zone.spawn.minDistanceFromPlayer + "-" + zone.spawn.maxDistanceFromPlayer + ", attempts=" + zone.spawn.maxPositionAttempts);

                            if (zone.mobs.isEmpty()) {
                                CommandFeedback.send(context.getSource(), "No mob rules configured.");
                            } else {
                                for (MobRule mob : zone.mobs) {
                                    CommandFeedback.send(context.getSource(), "- [" + mob.spawnType + "] " + mob.entity + " max=" + mob.maxAlive + " count=" + mob.spawnCount + " cooldown=" + mob.respawnSeconds + "s chance=" + mob.chance + " boundary=" + boundarySummary(mob));
                                }
                            }

                            return 1;
                        })
                );
    }

    private static String boundarySummary(MobRule mob) {
        String mode = mob.boundaryMode == null || mob.boundaryMode.isBlank() ? MobRule.BOUNDARY_LEASH : mob.boundaryMode;
        return mob.boundaryMaxOutsideSeconds > 0 ? mode + ", " + mob.boundaryMaxOutsideSeconds + "s" : mode;
    }
}
