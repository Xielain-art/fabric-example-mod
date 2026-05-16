package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.GerbariumRegionsBridge;
import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.command.HelpCommand;
import com.gerbarium.regions.network.GerbariumServerNetworking;
import com.gerbarium.regions.runtime.BridgeRuntimeReloadDispatcher;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneCommand {
    private ZoneCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("zone")
                .executes(HelpCommand::executeZoneHelp)

                .then(literal("gui")
                        .then(argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");

                                    if (storage.findZone(id).isEmpty()) {
                                        CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                        return 0;
                                    }

                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    GerbariumServerNetworking.openGui(player, id);
                                    return 1;
                                })
                        )
                )

                .then(literal("reload")
                        .executes(context -> {
                            storage.reload();
                            CommandFeedback.send(
                                    context.getSource(),
                                    "Gerbarium zones reloaded from config/gerbarium/zones/ (modular format)."
                            );
                            return 1;
                        })
                )

                .then(ZoneListCommand.build(storage))
                .then(ZoneCreateCommand.build(storage))
                .then(ZoneInfoCommand.build(storage))
                .then(ZoneSelectCommand.build(storage))
                .then(ZoneToggleCommand.buildEnable(storage))
                .then(ZoneToggleCommand.buildDisable(storage))
                .then(ZoneMobCommand.buildMobRoot(storage))
                .then(ZoneResourceCommand.buildResourceRoot(storage))
                .then(ZoneSettingsCommand.build(storage))

                .then(literal("delete")
                        .then(argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    storage.reload();

                                    if (!storage.deleteZone(id)) {
                                        CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                        return 0;
                                    }

                                    storage.reload();
                                    BridgeRuntimeReloadDispatcher.reloadIfPresent();
                                    CommandFeedback.send(context.getSource(), "Deleted zone: " + id);
                                    BridgeRuntimeReloadDispatcher.sendSavedHint(context.getSource());
                                    return 1;
                                })
                        )
                );
    }
}
