package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.runtime.BridgeRuntimeReloadDispatcher;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneToggleCommand {
    private ZoneToggleCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildEnable(ZoneStorage storage) {
        return literal("enable")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> setEnabled(
                                context.getSource(),
                                storage,
                                StringArgumentType.getString(context, "id"),
                                true
                        ))
                );
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildDisable(ZoneStorage storage) {
        return literal("disable")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> setEnabled(
                                context.getSource(),
                                storage,
                                StringArgumentType.getString(context, "id"),
                                false
                        ))
                );
    }

    private static int setEnabled(ServerCommandSource source, ZoneStorage storage, String id, boolean enabled) {
        storage.reload();
        Optional<Zone> optionalZone = storage.findZone(id);

        if (optionalZone.isEmpty()) {
            CommandFeedback.error(source, "Zone not found: " + id);
            return 0;
        }

        Zone zone = optionalZone.get();
        zone.enabled = enabled;
        storage.addZone(zone);
        storage.reload();

        CommandFeedback.send(source, (enabled ? "Enabled" : "Disabled") + " zone: " + id);
        BridgeRuntimeReloadDispatcher.reloadIfPresent();
        BridgeRuntimeReloadDispatcher.sendSavedHint(source);
        return 1;
    }
}
