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
        CommandFeedback.send(source, "/gerb gui - open admin GUI");
        CommandFeedback.send(source, "/gerb zone - show zone commands");
        CommandFeedback.send(source, "/gerb zone gui <zone> - open GUI for selected zone");
        CommandFeedback.send(source, "/gerb zone create <zone>");
        CommandFeedback.send(source, "/gerb zone delete <zone>");
        CommandFeedback.send(source, "/gerb zone list");
        CommandFeedback.send(source, "/gerb zone info <zone>");
        CommandFeedback.send(source, "/gerb zone select <zone>");
        CommandFeedback.send(source, "/gerb zone enable <zone>");
        CommandFeedback.send(source, "/gerb zone disable <zone>");
        CommandFeedback.send(source, "/gerb zone mob - show mob rule commands");
        CommandFeedback.send(source, "/gerb zone clear <zone>");
        CommandFeedback.send(source, "/gerb zone reload");
    }

    public static void sendZoneHelp(ServerCommandSource source) {
        CommandFeedback.send(source, "Gerbarium zone commands:");
        CommandFeedback.send(source, "/gerb zone gui <zone> - open GUI for zone");
        CommandFeedback.send(source, "/gerb zone create <zone> - save current WorldEdit selection as zone");
        CommandFeedback.send(source, "/gerb zone select <zone> - load saved zone back into WorldEdit selection");
        CommandFeedback.send(source, "/gerb zone list - list zones");
        CommandFeedback.send(source, "/gerb zone info <zone> - inspect zone");
        CommandFeedback.send(source, "/gerb zone delete <zone> - delete zone");
        CommandFeedback.send(source, "/gerb zone enable <zone> - enable zone");
        CommandFeedback.send(source, "/gerb zone disable <zone> - disable zone");
        CommandFeedback.send(source, "/gerb zone mob add-pack <zone> <ruleName> <entity> <maxAlive> <spawnCount> <respawnSeconds> <chance>");
        CommandFeedback.send(source, "/gerb zone mob add-unique <zone> <ruleName> <entity> <respawnSeconds> <chance>");
        CommandFeedback.send(source, "/gerb zone mob remove <zone> <ruleName>");
        CommandFeedback.send(source, "/gerb zone mob list <zone>");
        CommandFeedback.send(source, "/gerb zone mob info <zone> <ruleName>");
        CommandFeedback.send(source, "/gerb zone mob companion add <zone> <ruleName> <companionName> <entity> <count> <radius> <chance>");
        CommandFeedback.send(source, "/gerb zone mob companion remove <zone> <ruleName> <companionName>");
        CommandFeedback.send(source, "/gerb zone mob companion list <zone> <ruleName>");
        CommandFeedback.send(source, "/gerb zone mob companion info <zone> <ruleName> <companionName>");
        CommandFeedback.send(source, "/gerb zone clear <zone> - remove mobs tagged as gerb_zone_<zone>");
        CommandFeedback.send(source, "/gerb zone reload - reload regions.json");
        CommandFeedback.send(source, "UI uses rule names; rule ids are shown in advanced/view mode.");
    }
}
