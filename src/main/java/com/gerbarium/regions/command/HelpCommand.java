package com.gerbarium.regions.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public final class HelpCommand {
    public static final String ADMIN_PERMISSION = "gerbarium.regions.admin";

    private HelpCommand() {
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> build() {
        return literal("help")
                .executes(HelpCommand::executeMainHelp);
    }

    public static int executeMainHelp(CommandContext<ServerCommandSource> context) {
        sendMainHelp(context.getSource());
        return 1;
    }

    public static int executeZoneHelp(CommandContext<ServerCommandSource> context) {
        sendZoneHelp(context.getSource());
        return 1;
    }

    public static void sendMainHelp(ServerCommandSource source) {
        CommandFeedback.send(source, "Gerbarium Regions Bridge commands:");
        CommandFeedback.send(source, "Required permission: " + ADMIN_PERMISSION);
        CommandFeedback.send(source, "/gerb help");
        CommandFeedback.send(source, "/gerb test");
        CommandFeedback.send(source, "/gerb zone");
        CommandFeedback.send(source, "/gerb zone create <id>");
        CommandFeedback.send(source, "/gerb zone delete <id>");
        CommandFeedback.send(source, "/gerb zone list");
        CommandFeedback.send(source, "/gerb zone info <id>");
        CommandFeedback.send(source, "/gerb zone select <id>");
        CommandFeedback.send(source, "/gerb zone enable <id>");
        CommandFeedback.send(source, "/gerb zone disable <id>");
        CommandFeedback.send(source, "/gerb zone addmob <zone> <entity> <maxAlive> <spawnCount> <respawnSeconds> <chance>");
        CommandFeedback.send(source, "/gerb zone removemob <zone> <entity>");
        CommandFeedback.send(source, "/gerb zone clear <id>");
        CommandFeedback.send(source, "/gerb zone reload");
    }

    public static void sendZoneHelp(ServerCommandSource source) {
        CommandFeedback.send(source, "Gerbarium zone commands:");
        CommandFeedback.send(source, "/gerb zone create <id> - save current WorldEdit selection as zone");
        CommandFeedback.send(source, "/gerb zone select <id> - load saved zone back into WorldEdit selection");
        CommandFeedback.send(source, "/gerb zone list - list zones");
        CommandFeedback.send(source, "/gerb zone info <id> - inspect zone");
        CommandFeedback.send(source, "/gerb zone delete <id> - delete zone");
        CommandFeedback.send(source, "/gerb zone enable <id> - enable zone");
        CommandFeedback.send(source, "/gerb zone disable <id> - disable zone");
        CommandFeedback.send(source, "/gerb zone addmob <zone> <entity> <maxAlive> <spawnCount> <respawnSeconds> <chance>");
        CommandFeedback.send(source, "/gerb zone removemob <zone> <entity>");
        CommandFeedback.send(source, "/gerb zone clear <id> - remove mobs tagged as gerb_zone_<id>");
        CommandFeedback.send(source, "/gerb zone reload - reload regions.json");
    }
}