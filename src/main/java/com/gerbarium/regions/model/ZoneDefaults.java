package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

public final class ZoneDefaults {
    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int DEFAULT_BOUNDARY_MAX_OUTSIDE_SECONDS = 10;
    private static final int DEFAULT_BOUNDARY_CHECK_INTERVAL_TICKS = 40;

    private ZoneDefaults() {
    }

    public static ZoneActivationSettings defaultActivation() {
        return new ZoneActivationSettings();
    }

    public static ZoneSpawnSettings defaultSpawn() {
        return new ZoneSpawnSettings();
    }

    public static void normalizeZone(Zone zone) {
        if (zone.activation == null) {
            zone.activation = defaultActivation();
        }
        if (zone.spawn == null) {
            zone.spawn = defaultSpawn();
        }
        if (zone.mobs == null) {
            zone.mobs = new ArrayList<>();
        }
        for (MobRule rule : zone.mobs) {
            normalizeMobRule(rule);
        }
    }

    public static void normalizeMobRule(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            rule.id = MobRule.generateId();
        }
        if (rule.name == null || rule.name.isBlank()) {
            rule.name = rule.id;
        }
        if (rule.enabled == null) {
            rule.enabled = true;
        }
        if (rule.companions == null) {
            rule.companions = new ArrayList<>();
        }
        for (CompanionRule companion : rule.companions) {
            normalizeCompanionRule(companion);
        }
        if (rule.spawnType == null) {
            rule.spawnType = SpawnType.PACK;
        }
        if (rule.spawnType == SpawnType.UNIQUE) {
            if (rule.refillMode == null) {
                rule.refillMode = RefillMode.AFTER_DEATH;
            }
            if (rule.cooldownStart == null) {
                rule.cooldownStart = CooldownStart.AFTER_DEATH;
            }
            if (rule.maxAlive < 1) {
                rule.maxAlive = 1;
            }
            if (rule.spawnCount < 1) {
                rule.spawnCount = 1;
            }
            if (rule.respawnSeconds < 1) {
                rule.respawnSeconds = 86400;
            }
            if (rule.chance < 0.0 || rule.chance > 1.0) {
                rule.chance = 1.0;
            }
            if (rule.failedSpawnRetrySeconds < 1) {
                rule.failedSpawnRetrySeconds = 60;
            }
            normalizeBoundaryFields(rule, true);
            return;
        }

        if (rule.refillMode == null) {
            rule.refillMode = RefillMode.ON_ACTIVATION;
        }
        if (rule.cooldownStart == null) {
            rule.cooldownStart = CooldownStart.AFTER_ACTIVATION;
        }
        rule.cooldownStart = CooldownStart.AFTER_ACTIVATION;
        if (rule.maxAlive < 1) {
            rule.maxAlive = 10;
        }
        if (rule.spawnCount < 1) {
            rule.spawnCount = 4;
        }
        if (rule.respawnSeconds < 1) {
            rule.respawnSeconds = 900;
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            rule.chance = 1.0;
        }
        rule.spawnWhenReady = true;
        if (rule.failedSpawnRetrySeconds < 1) {
            rule.failedSpawnRetrySeconds = 60;
        }
        normalizeBoundaryFields(rule, false);
    }

    public static void validateZone(Zone zone) {
        if (zone.id == null || zone.id.isBlank()) {
            throw new IllegalArgumentException("Zone id cannot be empty");
        }
        if (!SAFE_ID.matcher(zone.id).matches()) {
            throw new IllegalArgumentException("Zone id must contain only latin letters, digits, '_' or '-'");
        }
        if (zone.dimension == null || zone.dimension.isBlank()) {
            throw new IllegalArgumentException("Zone dimension cannot be empty");
        }
        if (zone.min == null || zone.max == null) {
            throw new IllegalArgumentException("Zone min/max cannot be empty");
        }
        normalizeZone(zone);
        if (zone.activation.range <= 0) {
            throw new IllegalArgumentException("activation.range must be > 0");
        }
        if (zone.activation.deactivateAfterSeconds < 0 || zone.activation.firstSpawnDelaySeconds < 0 || zone.activation.reactivationCooldownSeconds < 0) {
            throw new IllegalArgumentException("activation seconds must be >= 0");
        }
        if (zone.spawn.minDistanceFromPlayer < 0) {
            throw new IllegalArgumentException("spawn.minDistanceFromPlayer must be >= 0");
        }
        if (zone.spawn.maxDistanceFromPlayer <= zone.spawn.minDistanceFromPlayer) {
            throw new IllegalArgumentException("spawn.maxDistanceFromPlayer must be > minDistanceFromPlayer");
        }
        if (zone.spawn.maxPositionAttempts <= 0) {
            throw new IllegalArgumentException("spawn.maxPositionAttempts must be > 0");
        }
        for (MobRule mob : zone.mobs) {
            validateMobRule(mob);
            normalizeMobRule(mob);
        }
    }

    public static void validateMobRule(MobRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            throw new IllegalArgumentException("Rule id cannot be empty");
        }
        if (rule.name == null || rule.name.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be empty");
        }
        if (rule.entity == null || rule.entity.isBlank()) {
            throw new IllegalArgumentException("Rule entity cannot be empty");
        }
        if (rule.spawnType == null) {
            throw new IllegalArgumentException("Rule spawnType cannot be empty");
        }
        if (rule.refillMode == null) {
            throw new IllegalArgumentException("Rule refillMode cannot be empty");
        }
        if (rule.maxAlive < 1 || rule.spawnCount < 1 || rule.respawnSeconds < 1) {
            throw new IllegalArgumentException("maxAlive/spawnCount/respawnSeconds must be >= 1");
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            throw new IllegalArgumentException("chance must be 0..1");
        }
        if (rule.failedSpawnRetrySeconds < 1) {
            throw new IllegalArgumentException("failedSpawnRetrySeconds must be >= 1");
        }
        if (!isValidBoundaryMode(rule.boundaryMode)) {
            throw new IllegalArgumentException("boundaryMode must be one of NONE, LEASH, TELEPORT_BACK, REMOVE_OUTSIDE");
        }
        if (rule.boundaryMaxOutsideSeconds < 0) {
            throw new IllegalArgumentException("boundaryMaxOutsideSeconds must be >= 0");
        }
        if (rule.boundaryCheckIntervalTicks < 20) {
            throw new IllegalArgumentException("boundaryCheckIntervalTicks must be >= 20");
        }
        if (rule.companions == null) {
            rule.companions = new ArrayList<>();
        }
        HashSet<String> ids = new HashSet<>();
        for (CompanionRule companion : rule.companions) {
            validateCompanionRule(companion);
            String lowered = companion.id.toLowerCase();
            if (!ids.add(lowered)) {
                throw new IllegalArgumentException("Companion id must be unique inside mob rule: " + companion.id);
            }
        }
    }

    public static void normalizeCompanionRule(CompanionRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            rule.id = CompanionRule.generateId();
        }
        if (rule.name == null || rule.name.isBlank()) {
            rule.name = rule.id;
        }
        if (rule.count < 1) {
            rule.count = 1;
        }
        if (rule.radius < 1) {
            rule.radius = 8;
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            rule.chance = 1.0;
        }
    }

    public static void validateCompanionRule(CompanionRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            throw new IllegalArgumentException("Companion id cannot be empty");
        }
        if (rule.name == null || rule.name.isBlank()) {
            throw new IllegalArgumentException("Companion name cannot be empty");
        }
        if (rule.entity == null || rule.entity.isBlank()) {
            throw new IllegalArgumentException("Companion entity cannot be empty");
        }
        if (rule.count < 1) {
            throw new IllegalArgumentException("Companion count must be >= 1");
        }
        if (rule.radius < 1) {
            throw new IllegalArgumentException("Companion radius must be >= 1");
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            throw new IllegalArgumentException("Companion chance must be 0..1");
        }
    }

    public static boolean isValidBoundaryMode(String mode) {
        return MobRule.BOUNDARY_NONE.equals(mode)
                || MobRule.BOUNDARY_LEASH.equals(mode)
                || MobRule.BOUNDARY_TELEPORT_BACK.equals(mode)
                || MobRule.BOUNDARY_REMOVE_OUTSIDE.equals(mode);
    }

    public static String defaultBoundaryModeFor(SpawnType spawnType) {
        return spawnType == SpawnType.UNIQUE ? MobRule.BOUNDARY_TELEPORT_BACK : MobRule.BOUNDARY_LEASH;
    }

    private static void normalizeBoundaryFields(MobRule rule, boolean uniqueDefaults) {
        rule.boundaryModeWasInvalid = false;
        if (!isValidBoundaryMode(rule.boundaryMode)) {
            rule.boundaryModeWasInvalid = true;
            rule.boundaryMode = defaultBoundaryModeFor(uniqueDefaults ? SpawnType.UNIQUE : rule.spawnType);
        }
        if (rule.boundaryMaxOutsideSeconds < 0) {
            rule.boundaryMaxOutsideSeconds = DEFAULT_BOUNDARY_MAX_OUTSIDE_SECONDS;
        }
        if (rule.boundaryCheckIntervalTicks < 20) {
            rule.boundaryCheckIntervalTicks = DEFAULT_BOUNDARY_CHECK_INTERVAL_TICKS;
        }
    }
}
