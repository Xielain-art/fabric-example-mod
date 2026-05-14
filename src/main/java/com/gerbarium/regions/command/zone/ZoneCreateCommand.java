package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.runtime.BridgeRuntimeReloadDispatcher;
import com.gerbarium.regions.storage.ZoneStorage;
import com.gerbarium.regions.worldedit.WorldEditSelectionReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
                            if (!WorldEditSelectionReader.isAvailable()) {
                                CommandFeedback.error(context.getSource(), "WorldEdit mod is not loaded. Zone create from selection is unavailable.");
                                return 0;
                            }

                            try {
                                Zone zone = WorldEditSelectionReader.readSelection(player, id);

                                storage.addZone(zone);
                                storage.reload();
                                BridgeRuntimeReloadDispatcher.reloadIfPresent();

                                CommandFeedback.send(context.getSource(), "Created zone '" + id + "' from WorldEdit selection.");
                                CommandFeedback.send(context.getSource(), "Saved to config/gerbarium/zones/" + id + "/zone.json");
                                BridgeRuntimeReloadDispatcher.sendSavedHint(context.getSource());
                                CommandFeedback.send(context.getSource(), "Use /gerb zone gui " + id + " to edit it.");

                                return 1;
                            } catch (Exception e) {
                                String message = e.getMessage() == null ? "unknown error" : e.getMessage();
                                if (message.contains("selection") || message.contains("Region") || message.contains("Incomplete")) {
                                    CommandFeedback.error(context.getSource(), "Selection is incomplete. Use //wand and select pos1/pos2 first.");
                                    return 0;
                                }
                                CommandFeedback.error(context.getSource(), "Failed to create zone: " + message);
                                return 0;
                            }
                        })
                );
    }
}
