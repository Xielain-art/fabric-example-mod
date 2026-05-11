package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneListCommand {
    private ZoneListCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("list")
                .executes(context -> {
                    List<Zone> zones = storage.getData().zones;

                    if (zones.isEmpty()) {
                        CommandFeedback.send(context.getSource(), "No Gerbarium zones found.");
                        return 1;
                    }

                    CommandFeedback.send(context.getSource(), "Gerbarium zones:");

                    for (Zone zone : zones) {
                        CommandFeedback.send(context.getSource(), "- " + zone.id
                                + " [" + (zone.enabled ? "enabled" : "disabled") + "]"
                                + " dim=" + zone.dimension
                                + " mobs=" + zone.mobs.size());
                    }

                    return zones.size();
                });
    }
}