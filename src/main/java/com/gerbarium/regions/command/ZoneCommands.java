package com.gerbarium.regions.command;

import com.gerbarium.regions.command.zone.ZoneCommand;
import com.gerbarium.regions.network.GerbariumServerNetworking;
import com.gerbarium.regions.permission.PermissionUtil;
import com.gerbarium.regions.storage.ZoneStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class ZoneCommands {
    private static final ZoneStorage STORAGE = new ZoneStorage();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("gerb")
                        .requires(PermissionUtil::hasAdminPermission)

                        .executes(HelpCommand::executeMainHelp)

                        .then(HelpCommand.build())

                        .then(literal("test")
                                .executes(context -> {
                                    CommandFeedback.send(context.getSource(), "Gerbarium Regions Bridge works!");
                                    CommandFeedback.send(context.getSource(), "Permission node: " + HelpCommand.ADMIN_PERMISSION);
                                    return 1;
                                })
                        )

                        .then(literal("gui")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    GerbariumServerNetworking.openGui(player, "");
                                    return 1;
                                })
                        )

                        .then(ZoneCommand.build(STORAGE))
        ));
    }
}