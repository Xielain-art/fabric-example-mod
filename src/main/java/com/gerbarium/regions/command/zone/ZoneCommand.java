package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.HelpCommand;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneCommand {
    private ZoneCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("zone")
                .executes(HelpCommand::executeZoneHelp)

                .then(literal("reload")
                        .executes(context -> {
                            storage.reload();
                            com.gerbarium.regions.command.CommandFeedback.send(
                                    context.getSource(),
                                    "Gerbarium zones reloaded from config/gerbarium/regions.json."
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
                .then(ZoneMobCommand.buildAddMob(storage))
                .then(ZoneMobCommand.buildRemoveMob(storage))
                .then(ZoneClearCommand.build(storage))
                .then(literal("delete")
                        .then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                                .<ServerCommandSource, String>argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> {
                                    String id = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "id");

                                    if (!storage.deleteZone(id)) {
                                        com.gerbarium.regions.command.CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                        return 0;
                                    }

                                    com.gerbarium.regions.command.CommandFeedback.send(context.getSource(), "Deleted zone: " + id);
                                    return 1;
                                })
                        )
                );
    }
}