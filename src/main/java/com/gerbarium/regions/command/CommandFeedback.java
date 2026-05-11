package com.gerbarium.regions.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class CommandFeedback {
    private CommandFeedback() {
    }

    public static void send(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal("[Gerbarium] " + message), false);
    }

    public static void error(ServerCommandSource source, String message) {
        source.sendError(Text.literal("[Gerbarium] " + message));
    }
}