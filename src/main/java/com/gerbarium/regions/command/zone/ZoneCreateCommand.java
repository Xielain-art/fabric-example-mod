package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.gerbarium.regions.worldedit.WorldEditSelectionReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.IncompleteRegionException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneCreateCommand {
    private ZoneCreateCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("create")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");

                            storage.reload();

                            if (storage.exists(id)) {
                                CommandFeedback.error(context.getSource(), "Zone already exists: " + id);
                                return 0;
                            }

                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

                            try {
                                Zone zone = WorldEditSelectionReader.readSelection(player, id);

                                storage.addZone(zone);
                                storage.reload();

                                CommandFeedback.send(context.getSource(), "Created zone '" + id + "' from WorldEdit selection.");
                                CommandFeedback.send(context.getSource(), "Saved to config/gerbarium/zones/" + id + ".json");
                                CommandFeedback.send(context.getSource(), "Use /gerb zone gui " + id + " to edit it.");

                                return 1;
                            } catch (IncompleteRegionException e) {
                                CommandFeedback.error(context.getSource(), "Selection is incomplete. Use //wand and select pos1/pos2 first.");
                                return 0;
                            } catch (Exception e) {
                                CommandFeedback.error(context.getSource(), "Failed to create zone: " + e.getMessage());
                                return 0;
                            }
                        })
                );
    }
}
