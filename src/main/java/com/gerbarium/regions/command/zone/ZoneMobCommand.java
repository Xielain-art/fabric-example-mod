package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.CompanionRule;
import com.gerbarium.regions.model.MobRule;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneMobCommand {
    private ZoneMobCommand() {}

    public static LiteralArgumentBuilder<ServerCommandSource> buildMobRoot(ZoneStorage storage) {
        return literal("mob")
                .then(buildAddPack(storage))
                .then(buildAddUnique(storage))
                .then(buildRemove(storage))
                .then(buildList(storage))
                .then(buildInfo(storage))
                .then(buildEnable(storage, true))
                .then(buildEnable(storage, false))
                .then(buildCompanionRoot(storage));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddPack(ZoneStorage storage) {
        return literal("add-pack")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .then(argument("entity", IdentifierArgumentType.identifier())
                                        .then(argument("maxAlive", IntegerArgumentType.integer(1))
                                                .then(argument("spawnCount", IntegerArgumentType.integer(1))
                                                        .then(argument("respawnSeconds", IntegerArgumentType.integer(1))
                                                                .then(argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                        .executes(ctx -> {
                                                                            String zoneId = StringArgumentType.getString(ctx, "zone");
                                                                            String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                                                            Identifier entity = IdentifierArgumentType.getIdentifier(ctx, "entity");
                                                                            MobRule rule = MobRule.packDefaults(ruleId, entity.toString());
                                                                            rule.maxAlive = IntegerArgumentType.getInteger(ctx, "maxAlive");
                                                                            rule.spawnCount = IntegerArgumentType.getInteger(ctx, "spawnCount");
                                                                            rule.respawnSeconds = IntegerArgumentType.getInteger(ctx, "respawnSeconds");
                                                                            rule.chance = DoubleArgumentType.getDouble(ctx, "chance");
                                                                            return upsertRule(ctx.getSource(), storage, zoneId, rule);
                                                                        })
                                                                )))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddUnique(ZoneStorage storage) {
        return literal("add-unique")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .then(argument("entity", IdentifierArgumentType.identifier())
                                        .then(argument("respawnSeconds", IntegerArgumentType.integer(1))
                                                .then(argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                        .executes(ctx -> {
                                                            String zoneId = StringArgumentType.getString(ctx, "zone");
                                                            String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                                            Identifier entity = IdentifierArgumentType.getIdentifier(ctx, "entity");
                                                            MobRule rule = MobRule.uniqueDefaults(ruleId, entity.toString());
                                                            rule.respawnSeconds = IntegerArgumentType.getInteger(ctx, "respawnSeconds");
                                                            rule.chance = DoubleArgumentType.getDouble(ctx, "chance");
                                                            return upsertRule(ctx.getSource(), storage, zoneId, rule);
                                                        })
                                                )))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRemove(ZoneStorage storage) {
        return literal("remove").then(argument("zone", StringArgumentType.word()).then(argument("ruleId", StringArgumentType.word())
                .executes(ctx -> {
                    Optional<Zone> oz = storage.findZone(StringArgumentType.getString(ctx, "zone"));
                    if (oz.isEmpty()) {
                        CommandFeedback.error(ctx.getSource(), "Zone not found.");
                        return 0;
                    }
                    Zone zone = oz.get();
                    String id = StringArgumentType.getString(ctx, "ruleId");
                    boolean removed = zone.mobs.removeIf(r -> matchesRule(r, id));
                    if (!removed) {
                        CommandFeedback.error(ctx.getSource(), "Mob rule not found: " + id);
                        return 0;
                    }
                    storage.addZone(zone);
                    CommandFeedback.send(ctx.getSource(), "Removed mob rule '" + id + "'.");
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildEnable(ZoneStorage storage, boolean enabled) {
        return literal(enabled ? "enable" : "disable")
                .then(argument("zone", StringArgumentType.word())
                        .then(argument("ruleId", StringArgumentType.word())
                                .executes(ctx -> {
                                    Optional<Zone> oz = storage.findZone(StringArgumentType.getString(ctx, "zone"));
                                    if (oz.isEmpty()) {
                                        CommandFeedback.error(ctx.getSource(), "Zone not found.");
                                        return 0;
                                    }
                                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                    Optional<MobRule> r = oz.get().mobs.stream().filter(x -> matchesRule(x, ruleId)).findFirst();
                                    if (r.isEmpty()) {
                                        CommandFeedback.error(ctx.getSource(), "Mob rule not found: " + ruleId);
                                        return 0;
                                    }
                                    r.get().enabled = enabled;
                                    storage.addZone(oz.get());
                                    CommandFeedback.send(ctx.getSource(), (enabled ? "Enabled" : "Disabled") + " rule '" + ruleId + "'.");
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildList(ZoneStorage storage) {
        return literal("list").then(argument("zone", StringArgumentType.word()).executes(ctx -> {
            Optional<Zone> oz = storage.findZone(StringArgumentType.getString(ctx, "zone"));
            if (oz.isEmpty()) {
                CommandFeedback.error(ctx.getSource(), "Zone not found.");
                return 0;
            }
            for (MobRule r : oz.get().mobs) {
                CommandFeedback.send(ctx.getSource(), "- " + r.spawnType + " " + displayName(r) + " -> " + r.entity + " respawn=" + r.respawnSeconds + "s chance=" + r.chance);
            }
            return 1;
        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInfo(ZoneStorage storage) {
        return literal("info").then(argument("zone", StringArgumentType.word()).then(argument("ruleId", StringArgumentType.word()).executes(ctx -> {
            Optional<Zone> oz = storage.findZone(StringArgumentType.getString(ctx, "zone"));
            if (oz.isEmpty()) {
                CommandFeedback.error(ctx.getSource(), "Zone not found.");
                return 0;
            }
            String ruleId = StringArgumentType.getString(ctx, "ruleId");
            Optional<MobRule> r = oz.get().mobs.stream().filter(x -> matchesRule(x, ruleId)).findFirst();
            if (r.isEmpty()) {
                CommandFeedback.error(ctx.getSource(), "Mob rule not found: " + ruleId);
                return 0;
            }
            MobRule m = r.get();
            CommandFeedback.send(ctx.getSource(), "Rule: " + displayName(m) + " (" + m.spawnType + ")");
            CommandFeedback.send(ctx.getSource(), "Entity: " + m.entity + ", enabled=" + Boolean.TRUE.equals(m.enabled));
            CommandFeedback.send(ctx.getSource(), "refill=" + m.refillMode + ", maxAlive=" + m.maxAlive + ", spawnCount=" + m.spawnCount);
            CommandFeedback.send(ctx.getSource(), "respawn=" + m.respawnSeconds + "s, chance=" + m.chance + ", retry=" + m.failedSpawnRetrySeconds + "s");
            return 1;
        })));
    }

    private static int upsertRule(ServerCommandSource source, ZoneStorage storage, String zoneId, MobRule rule) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) {
            CommandFeedback.error(source, "Zone not found: " + zoneId);
            return 0;
        }
        try {
            ZoneDefaults.validateMobRule(rule);
            ZoneDefaults.normalizeMobRule(rule);
        } catch (IllegalArgumentException ex) {
            CommandFeedback.error(source, ex.getMessage());
            return 0;
        }
        Zone zone = oz.get();
        zone.mobs.removeIf(x -> x.id.equalsIgnoreCase(rule.id));
        zone.mobs.add(rule);
        storage.addZone(zone);
        CommandFeedback.send(source, "Saved rule '" + displayName(rule) + "' in zone '" + zoneId + "'.");
        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildCompanionRoot(ZoneStorage storage) {
        return literal("companion")
                .then(literal("add")
                        .then(argument("zone", StringArgumentType.word())
                                .then(argument("ruleId", StringArgumentType.word())
                                        .then(argument("companionId", StringArgumentType.word())
                                                .then(argument("entity", IdentifierArgumentType.identifier())
                                                        .then(argument("count", IntegerArgumentType.integer(1))
                                                                .then(argument("radius", IntegerArgumentType.integer(1))
                                                                        .then(argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                                .executes(ctx -> {
                                                                                    String zoneId = StringArgumentType.getString(ctx, "zone");
                                                                                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                                                                    String companionId = StringArgumentType.getString(ctx, "companionId");
                                                                                    Identifier entity = IdentifierArgumentType.getIdentifier(ctx, "entity");
                                                                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                                                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                                                    double chance = DoubleArgumentType.getDouble(ctx, "chance");
                                                                                    return upsertCompanion(ctx.getSource(), storage, zoneId, ruleId, companionId, entity.toString(), count, radius, chance);
                                                                                })))))))))
                .then(literal("remove")
                        .then(argument("zone", StringArgumentType.word())
                                .then(argument("ruleId", StringArgumentType.word())
                                        .then(argument("companionId", StringArgumentType.word())
                                                .executes(ctx -> removeCompanion(ctx.getSource(), storage, StringArgumentType.getString(ctx, "zone"), StringArgumentType.getString(ctx, "ruleId"), StringArgumentType.getString(ctx, "companionId")))))))
                .then(literal("list")
                        .then(argument("zone", StringArgumentType.word())
                                .then(argument("ruleId", StringArgumentType.word())
                                        .executes(ctx -> listCompanions(ctx.getSource(), storage, StringArgumentType.getString(ctx, "zone"), StringArgumentType.getString(ctx, "ruleId"))))))
                .then(literal("info")
                        .then(argument("zone", StringArgumentType.word())
                                .then(argument("ruleId", StringArgumentType.word())
                                        .then(argument("companionId", StringArgumentType.word())
                                                .executes(ctx -> infoCompanion(ctx.getSource(), storage, StringArgumentType.getString(ctx, "zone"), StringArgumentType.getString(ctx, "ruleId"), StringArgumentType.getString(ctx, "companionId")))))));
    }

    private static Optional<MobRule> findRule(Zone zone, String ruleId) {
        return zone.mobs.stream().filter(r -> ruleId.equalsIgnoreCase(r.id)).findFirst();
    }

    private static int upsertCompanion(ServerCommandSource source, ZoneStorage storage, String zoneId, String ruleId, String companionId, String entity, int count, int radius, double chance) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) { CommandFeedback.error(source, "Zone not found: " + zoneId); return 0; }
        Optional<MobRule> or = findRule(oz.get(), ruleId);
        if (or.isEmpty()) { CommandFeedback.error(source, "Mob rule not found: " + ruleId); return 0; }
        CompanionRule c = new CompanionRule();
        c.uid64 = CompanionRule.generateUid64();
        c.id = c.uid64;
        c.name = companionId;
        c.entity = entity;
        c.count = count;
        c.radius = radius;
        c.chance = chance;
        try {
            ZoneDefaults.validateCompanionRule(c);
            ZoneDefaults.normalizeCompanionRule(c);
        } catch (IllegalArgumentException ex) {
            CommandFeedback.error(source, ex.getMessage());
            return 0;
        }
        MobRule rule = or.get();
        rule.companions.removeIf(x -> matchesCompanion(x, companionId));
        rule.companions.add(c);
        storage.addZone(oz.get());
        CommandFeedback.send(source, "Saved companion '" + companionId + "' in rule '" + ruleId + "'.");
        return 1;
    }

    private static int removeCompanion(ServerCommandSource source, ZoneStorage storage, String zoneId, String ruleId, String companionId) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) { CommandFeedback.error(source, "Zone not found: " + zoneId); return 0; }
        Optional<MobRule> or = findRule(oz.get(), ruleId);
        if (or.isEmpty()) { CommandFeedback.error(source, "Mob rule not found: " + ruleId); return 0; }
        boolean removed = or.get().companions.removeIf(c -> matchesCompanion(c, companionId));
        if (!removed) { CommandFeedback.error(source, "Companion not found: " + companionId); return 0; }
        storage.addZone(oz.get());
        CommandFeedback.send(source, "Removed companion '" + companionId + "'.");
        return 1;
    }

    private static int listCompanions(ServerCommandSource source, ZoneStorage storage, String zoneId, String ruleId) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) { CommandFeedback.error(source, "Zone not found: " + zoneId); return 0; }
        Optional<MobRule> or = findRule(oz.get(), ruleId);
        if (or.isEmpty()) { CommandFeedback.error(source, "Mob rule not found: " + ruleId); return 0; }
        for (CompanionRule c : or.get().companions) {
            CommandFeedback.send(source, "- " + displayName(c) + " -> " + c.entity + " count=" + c.count + " radius=" + c.radius + " chance=" + c.chance);
        }
        return 1;
    }

    private static int infoCompanion(ServerCommandSource source, ZoneStorage storage, String zoneId, String ruleId, String companionId) {
        Optional<Zone> oz = storage.findZone(zoneId);
        if (oz.isEmpty()) { CommandFeedback.error(source, "Zone not found: " + zoneId); return 0; }
        Optional<MobRule> or = findRule(oz.get(), ruleId);
        if (or.isEmpty()) { CommandFeedback.error(source, "Mob rule not found: " + ruleId); return 0; }
        Optional<CompanionRule> oc = or.get().companions.stream().filter(c -> matchesCompanion(c, companionId)).findFirst();
        if (oc.isEmpty()) { CommandFeedback.error(source, "Companion not found: " + companionId); return 0; }
        CompanionRule c = oc.get();
        CommandFeedback.send(source, "Companion: " + displayName(c));
        CommandFeedback.send(source, "Entity: " + c.entity);
        CommandFeedback.send(source, "Count: " + c.count + ", Radius: " + c.radius + ", Chance: " + c.chance);
        return 1;
    }

    private static boolean matchesRule(MobRule rule, String key) {
        return key != null && (
                key.equalsIgnoreCase(rule.id)
                        || key.equalsIgnoreCase(rule.uid64)
                        || key.equalsIgnoreCase(rule.name)
        );
    }

    private static boolean matchesCompanion(CompanionRule rule, String key) {
        return key != null && (
                key.equalsIgnoreCase(rule.id)
                        || key.equalsIgnoreCase(rule.uid64)
                        || key.equalsIgnoreCase(rule.name)
        );
    }

    private static String displayName(MobRule rule) {
        return rule.name == null || rule.name.isBlank() ? rule.id : rule.name;
    }

    private static String displayName(CompanionRule rule) {
        return rule.name == null || rule.name.isBlank() ? rule.id : rule.name;
    }
}
