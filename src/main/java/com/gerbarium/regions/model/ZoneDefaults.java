package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

public final class ZoneDefaults {
    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");

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
        boolean legacyIdentity = rule.uid64 == null || rule.uid64.isBlank();
        if (rule.uid64 == null || rule.uid64.isBlank()) {
            rule.uid64 = rule.id != null && !rule.id.isBlank() ? rule.id : MobRule.generateUid64();
        }
        if (rule.id == null || rule.id.isBlank()) {
            rule.id = rule.uid64;
        }
        if (legacyIdentity && (rule.name == null || rule.name.isBlank())) {
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
        if (rule.uid64 == null || rule.uid64.isBlank()) {
            throw new IllegalArgumentException("Rule uid64 cannot be empty");
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
        if (rule.companions == null) {
            rule.companions = new ArrayList<>();
        }
        HashSet<String> ids = new HashSet<>();
        for (CompanionRule companion : rule.companions) {
            validateCompanionRule(companion);
            String lowered = companion.uid64.toLowerCase();
            if (!ids.add(lowered)) {
                throw new IllegalArgumentException("Companion uid64 must be unique inside mob rule: " + companion.uid64);
            }
        }
    }

    public static void normalizeCompanionRule(CompanionRule rule) {
        boolean legacyIdentity = rule.uid64 == null || rule.uid64.isBlank();
        if (rule.uid64 == null || rule.uid64.isBlank()) {
            rule.uid64 = rule.id != null && !rule.id.isBlank() ? rule.id : CompanionRule.generateUid64();
        }
        if (rule.id == null || rule.id.isBlank()) {
            rule.id = rule.uid64;
        }
        if (legacyIdentity && (rule.name == null || rule.name.isBlank())) {
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
        if (rule.uid64 == null || rule.uid64.isBlank()) {
            throw new IllegalArgumentException("Companion uid64 cannot be empty");
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
}
