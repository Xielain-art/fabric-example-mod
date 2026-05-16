package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import com.gerbarium.regions.runtime.BridgeRuntimeReloadDispatcher;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneSettingsCommand {
    private ZoneSettingsCommand() {}

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("settings")
                .then(argument("zone", StringArgumentType.word()).executes(ctx -> show(ctx.getSource(), storage, StringArgumentType.getString(ctx, "zone"))))
                .then(buildInt(storage, "activation-range", (z, v) -> z.activation.range = v))
                .then(buildInt(storage, "deactivate-after", (z, v) -> z.activation.deactivateAfterSeconds = v))
                .then(buildInt(storage, "first-spawn-delay", (z, v) -> z.activation.firstSpawnDelaySeconds = v))
                .then(buildInt(storage, "reactivation-cooldown", (z, v) -> z.activation.reactivationCooldownSeconds = v))
                .then(buildInt(storage, "min-distance", (z, v) -> z.spawn.minDistanceFromPlayer = v))
                .then(buildInt(storage, "max-distance", (z, v) -> z.spawn.maxDistanceFromPlayer = v))
                .then(buildInt(storage, "max-position-attempts", (z, v) -> z.spawn.maxPositionAttempts = v))
                .then(buildBool(storage, "require-loaded-chunk", (z, v) -> z.spawn.requireLoadedChunk = v))
                .then(buildBool(storage, "respect-vanilla-spawn-rules", (z, v) -> z.spawn.respectVanillaSpawnRules = v));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInt(ZoneStorage storage, String name, SetterInt setter) {
        return literal(name).then(argument("zone", StringArgumentType.word()).then(argument("value", IntegerArgumentType.integer(0)).executes(ctx -> {
            return update(storage, ctx.getSource(), StringArgumentType.getString(ctx, "zone"), z -> setter.set(z, IntegerArgumentType.getInteger(ctx, "value")));
        })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildBool(ZoneStorage storage, String name, SetterBool setter) {
        return literal(name).then(argument("zone", StringArgumentType.word()).then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
            return update(storage, ctx.getSource(), StringArgumentType.getString(ctx, "zone"), z -> setter.set(z, BoolArgumentType.getBool(ctx, "value")));
        })));
    }

    private static int show(ServerCommandSource source, ZoneStorage storage, String zoneId) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) {
            CommandFeedback.error(source, "Zone not found: " + zoneId);
            return 0;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);
        CommandFeedback.send(source, "activation.range=" + z.activation.range);
        CommandFeedback.send(source, "activation.deactivateAfterSeconds=" + z.activation.deactivateAfterSeconds);
        CommandFeedback.send(source, "activation.firstSpawnDelaySeconds=" + z.activation.firstSpawnDelaySeconds);
        CommandFeedback.send(source, "activation.reactivationCooldownSeconds=" + z.activation.reactivationCooldownSeconds);
        CommandFeedback.send(source, "spawn.minDistanceFromPlayer=" + z.spawn.minDistanceFromPlayer);
        CommandFeedback.send(source, "spawn.maxDistanceFromPlayer=" + z.spawn.maxDistanceFromPlayer);
        CommandFeedback.send(source, "spawn.maxPositionAttempts=" + z.spawn.maxPositionAttempts);
        CommandFeedback.send(source, "spawn.requireLoadedChunk=" + z.spawn.requireLoadedChunk);
        CommandFeedback.send(source, "spawn.respectVanillaSpawnRules=" + z.spawn.respectVanillaSpawnRules);
        return 1;
    }

    private static int update(ZoneStorage storage, ServerCommandSource source, String zoneId, ZoneMutator mutator) {
        storage.reload();
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) {
            CommandFeedback.error(source, "Zone not found: " + zoneId);
            return 0;
        }
        Zone z = oz.get();
        ZoneDefaults.normalizeZone(z);
        mutator.apply(z);
        try {
            ZoneDefaults.validateZone(z);
        } catch (IllegalArgumentException e) {
            CommandFeedback.error(source, e.getMessage());
            return 0;
        }
        storage.addZone(z);
        CommandFeedback.send(source, "Updated settings for zone '" + zoneId + "'.");
        BridgeRuntimeReloadDispatcher.reloadIfPresent();
        BridgeRuntimeReloadDispatcher.sendSavedHint(source);
        return 1;
    }

    private interface ZoneMutator { void apply(Zone zone); }
    private interface SetterInt { void set(Zone zone, int value); }
    private interface SetterBool { void set(Zone zone, boolean value); }
}
