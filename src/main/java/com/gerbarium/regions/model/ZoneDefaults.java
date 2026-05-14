package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
        if (zone.version <= 0) {
            zone.version = 1;
        }
        if (zone.name == null || zone.name.isBlank()) {
            zone.name = zone.id;
        }
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
        if (zone.resources == null) {
            zone.resources = new ArrayList<>();
        }
        for (ResourceRule rule : zone.resources) {
            normalizeResourceRule(rule);
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
            if (rule.maxAlive < 0) {
                rule.maxAlive = 1;
            }
            if (rule.spawnCount < 0) {
                rule.spawnCount = 1;
            }
            if (rule.respawnSeconds < 1) {
                rule.respawnSeconds = 86400;
            }
            if (rule.chance < 0.0 || rule.chance > 1.0) {
                rule.chance = 1.0;
            }
            if (rule.failedSpawnRetrySeconds < 0) {
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
        if (rule.maxAlive < 0) {
            rule.maxAlive = 10;
        }
        if (rule.spawnCount < 0) {
            rule.spawnCount = 4;
        }
        if (rule.respawnSeconds < 1) {
            rule.respawnSeconds = 900;
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            rule.chance = 1.0;
        }
        rule.spawnWhenReady = true;
        if (rule.failedSpawnRetrySeconds < 0) {
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
        HashSet<String> mobIds = new HashSet<>();
        for (MobRule mob : zone.mobs) {
            validateMobRule(mob);
            normalizeMobRule(mob);
            String id = mob.id.toLowerCase(Locale.ROOT);
            if (!mobIds.add(id)) {
                throw new IllegalArgumentException("Mob rule id must be unique: " + mob.id);
            }
        }
        HashSet<String> resourceIds = new HashSet<>();
        for (ResourceRule res : zone.resources) {
            validateResourceRule(res);
            normalizeResourceRule(res);
            String id = res.id.toLowerCase(Locale.ROOT);
            if (!resourceIds.add(id)) {
                throw new IllegalArgumentException("Resource rule id must be unique: " + res.id);
            }
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
        if (rule.spawnType == SpawnType.PACK && rule.refillMode == RefillMode.AFTER_DEATH) {
            throw new IllegalArgumentException("PACK supports ON_ACTIVATION or TIMED refillMode");
        }
        if (rule.spawnType == SpawnType.UNIQUE && rule.refillMode != RefillMode.AFTER_DEATH) {
            throw new IllegalArgumentException("UNIQUE supports AFTER_DEATH refillMode");
        }
        if (rule.maxAlive < 0 || rule.spawnCount < 0 || rule.respawnSeconds < 1) {
            throw new IllegalArgumentException("maxAlive/spawnCount must be >= 0 and respawnSeconds must be >= 1");
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            throw new IllegalArgumentException("chance must be 0..1");
        }
        if (rule.failedSpawnRetrySeconds < 0) {
            throw new IllegalArgumentException("failedSpawnRetrySeconds must be >= 0");
        }
        if (rule.timedMaxSpawnsPerActivation != null && rule.timedMaxSpawnsPerActivation < -1) {
            throw new IllegalArgumentException("timedMaxSpawnsPerActivation must be null, -1, or >= 0");
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
        if (rule.count < 0) {
            rule.count = 0;
        }
        if (rule.radius < 0) {
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
        if (rule.count < 0) {
            throw new IllegalArgumentException("Companion count must be >= 0");
        }
        if (rule.radius < 0) {
            throw new IllegalArgumentException("Companion radius must be >= 0");
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

    public static void normalizeResourceRule(ResourceRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            rule.id = ResourceRule.generateId();
        }
        if (rule.name == null || rule.name.isBlank()) {
            rule.name = rule.id;
        }
        if (rule.targetBlocks == null) {
            rule.targetBlocks = new ArrayList<>();
        }
        if (rule.resourceBlocks == null) {
            rule.resourceBlocks = new ArrayList<>();
        }
        if (rule.maxActiveBlocks < 0) {
            rule.maxActiveBlocks = 12;
        }
        if (rule.spawnCount < 0) {
            rule.spawnCount = 3;
        }
        if (rule.respawnSeconds < 1) {
            rule.respawnSeconds = 900;
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            rule.chance = 1.0;
        }
        if (rule.replaceMode == null) {
            rule.replaceMode = ReplaceMode.ONLY_TARGET_BLOCKS;
        }
        if (rule.restoreMode == null) {
            rule.restoreMode = RestoreMode.RESTORE_ORIGINAL;
        }
        if (rule.activationMode == null) {
            rule.activationMode = ResourceActivationMode.REAL_TIME;
        }
        if (rule.restoreDelaySeconds < 0) {
            rule.restoreDelaySeconds = 300;
        }
        if (rule.maxPositionAttempts < 1) {
            rule.maxPositionAttempts = 64;
        }
        if (rule.placementMode == null) {
            rule.placementMode = PlacementMode.RANDOM_SCATTER;
        }
        if (rule.minDistanceBetweenResources < 0) {
            rule.minDistanceBetweenResources = 3;
        }
        for (WeightedBlock wb : rule.resourceBlocks) {
            if (wb.block == null) {
                wb.block = "";
            }
            if (wb.weight < 1) {
                wb.weight = 1;
            }
        }
    }

    public static void validateResourceRule(ResourceRule rule) {
        if (rule.id == null || rule.id.isBlank()) {
            throw new IllegalArgumentException("Resource rule id cannot be empty");
        }
        if (rule.name == null || rule.name.isBlank()) {
            throw new IllegalArgumentException("Resource rule name cannot be empty");
        }
        if (rule.resourceBlocks == null || rule.resourceBlocks.isEmpty()) {
            throw new IllegalArgumentException("resourceBlocks cannot be empty");
        }
        for (WeightedBlock wb : rule.resourceBlocks) {
            if (wb.block == null || wb.block.isBlank()) {
                throw new IllegalArgumentException("resourceBlocks entry block cannot be empty");
            }
            if (!isIdentifierLike(wb.block)) {
                throw new IllegalArgumentException("resourceBlocks entry block must be namespace:path: " + wb.block);
            }
            if (wb.weight < 1) {
                throw new IllegalArgumentException("resourceBlocks weight must be >= 1");
            }
        }
        if (rule.maxActiveBlocks < 0) {
            throw new IllegalArgumentException("maxActiveBlocks must be >= 0");
        }
        if (rule.spawnCount < 0) {
            throw new IllegalArgumentException("spawnCount must be >= 0");
        }
        if (rule.respawnSeconds < 1) {
            throw new IllegalArgumentException("respawnSeconds must be >= 1");
        }
        if (rule.chance < 0.0 || rule.chance > 1.0) {
            throw new IllegalArgumentException("chance must be 0..1");
        }
        if (rule.minY > rule.maxY) {
            throw new IllegalArgumentException("minY must be <= maxY");
        }
        if (rule.restoreDelaySeconds < 0) {
            throw new IllegalArgumentException("restoreDelaySeconds must be >= 0");
        }
        if (rule.maxPositionAttempts < 1) {
            throw new IllegalArgumentException("maxPositionAttempts must be >= 1");
        }
        if (rule.replaceMode == null) {
            throw new IllegalArgumentException("replaceMode cannot be empty");
        }
        if (rule.replaceMode == ReplaceMode.ONLY_TARGET_BLOCKS && (rule.targetBlocks == null || rule.targetBlocks.isEmpty())) {
            throw new IllegalArgumentException("targetBlocks cannot be empty when replaceMode is ONLY_TARGET_BLOCKS");
        }
        if (rule.restoreMode == null) {
            throw new IllegalArgumentException("restoreMode cannot be empty");
        }
        if (rule.activationMode == null) {
            throw new IllegalArgumentException("activationMode cannot be empty");
        }
        if (rule.placementMode == null) {
            throw new IllegalArgumentException("placementMode cannot be empty");
        }
        if (rule.minDistanceBetweenResources < 0) {
            throw new IllegalArgumentException("minDistanceBetweenResources must be >= 0");
        }
        HashSet<String> targetSet = new HashSet<>();
        if (rule.targetBlocks != null) {
            for (String tb : rule.targetBlocks) {
                if (tb == null || tb.isBlank()) {
                    throw new IllegalArgumentException("targetBlocks entry cannot be empty");
                }
                if (!isIdentifierLike(tb)) {
                    throw new IllegalArgumentException("targetBlocks entry must be namespace:path: " + tb);
                }
                if (!targetSet.add(tb.toLowerCase())) {
                    throw new IllegalArgumentException("Duplicate targetBlock: " + tb);
                }
            }
        }
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

    private static boolean isIdentifierLike(String id) {
        return id != null && id.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    }
}
