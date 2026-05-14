package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.gerbarium.regions.worldedit.WorldEditSelectionReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneSelectCommand {
    private ZoneSelectCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("select")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            Optional<Zone> optionalZone = storage.findZone(id);

                            if (optionalZone.isEmpty()) {
                                CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                return 0;
                            }

                            try {
                                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                if (!WorldEditSelectionReader.isAvailable()) {
                                    CommandFeedback.error(context.getSource(), "WorldEdit mod is not loaded. Zone selection sync is unavailable.");
                                    return 0;
                                }
                                Zone zone = optionalZone.get();

                                String playerDimension = player.getServerWorld()
                                        .getRegistryKey()
                                        .getValue()
                                        .toString();

                                if (!zone.dimension.equals(playerDimension)) {
                                    CommandFeedback.error(
                                            context.getSource(),
                                            "Zone is in dimension '" + zone.dimension + "', but you are in '" + playerDimension + "'."
                                    );
                                    return 0;
                                }

                                WorldEditSelectionReader.applySelection(player, zone);

                                CommandFeedback.send(context.getSource(), "Selected zone in WorldEdit: " + zone.id);
                                CommandFeedback.send(context.getSource(), "WorldEditCUI should now show the saved region.");
                                CommandFeedback.send(context.getSource(), "Min: " + zone.min.x + " " + zone.min.y + " " + zone.min.z);
                                CommandFeedback.send(context.getSource(), "Max: " + zone.max.x + " " + zone.max.y + " " + zone.max.z);

                                return 1;
                            } catch (Exception e) {
                                CommandFeedback.error(context.getSource(), "Failed to select zone: " + e.getMessage());
                                return 0;
                            }
                        })
                );
    }
}
