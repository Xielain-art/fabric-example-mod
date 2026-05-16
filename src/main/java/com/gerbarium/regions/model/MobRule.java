package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobRule {
    public static final String BOUNDARY_NONE = "NONE";
    public static final String BOUNDARY_LEASH = "LEASH";
    public static final String BOUNDARY_TELEPORT_BACK = "TELEPORT_BACK";
    public static final String BOUNDARY_REMOVE_OUTSIDE = "REMOVE_OUTSIDE";
    public static final String SPAWN_MODE_RANDOM_VALID_POSITION = "RANDOM_VALID_POSITION";
    public static final String SPAWN_MODE_CENTER = "CENTER";
    public static final String SPAWN_MODE_NEAR_CENTER = "NEAR_CENTER";
    public static final String SPAWN_MODE_FIXED_POINT = "FIXED_POINT";
    public static final String SPAWN_MODE_PLAYER_NEARBY = "PLAYER_NEARBY";
    public static final String SPAWN_MODE_BOSS_ROOM = "BOSS_ROOM";

    public static final String SPAWN_TRIGGER_TIMER = "TIMER";
    public static final String SPAWN_TRIGGER_AFTER_DEATH = "AFTER_DEATH";
    public static final String SPAWN_TRIGGER_ON_ACTIVATION = "ON_ACTIVATION";
    public static final String SPAWN_TRIGGER_MANUAL = "MANUAL";

    public String id;
    public String name;
    public String entity;
    public Boolean enabled = true;
    public SpawnType spawnType = SpawnType.PACK;
    public RefillMode refillMode = RefillMode.ON_ACTIVATION;
    public int maxAlive = 10;
    public int spawnCount = 4;
    public int respawnSeconds = 900;
    public double chance = 1.0;
    public CooldownStart cooldownStart = CooldownStart.AFTER_ACTIVATION;
    public boolean spawnWhenReady = true;
    public int failedSpawnRetrySeconds = 60;
    public boolean despawnWhenZoneInactive = false;
    public boolean announceOnSpawn = false;
    public Integer timedMaxSpawnsPerActivation = null;
    public String boundaryMode = BOUNDARY_LEASH;
    public int boundaryMaxOutsideSeconds = 10;
    public int boundaryCheckIntervalTicks = 40;
    public boolean boundaryTeleportBack = true;
    public String spawnMode = SPAWN_MODE_RANDOM_VALID_POSITION;
    public String spawnTrigger = SPAWN_TRIGGER_TIMER;
    public int afterDeathDelaySeconds = 30;
    public boolean respawnAfterDeath = false;
    public boolean respawnAfterDespawn = false;
    public Integer fixedX;
    public Integer fixedY;
    public Integer fixedZ;
    public boolean allowSmallRoom = true;
    public int positionAttempts = 128;
    public int minDistanceBetweenSpawns = 2;
    public boolean spreadSpawns = true;
    public boolean requirePlayerNearby = false;
    public int playerActivationRange = 64;
    public boolean requireChunkLoaded = true;
    public boolean allowForceLoad = false;
    public transient boolean boundaryModeWasInvalid = false;
    public List<CompanionRule> companions = new ArrayList<>();

    public MobRule() {
    }

    public MobRule(String id, String entity, int maxAlive, int spawnCount, int respawnSeconds, double chance) {
        this.id = id;
        this.entity = entity;
        this.maxAlive = maxAlive;
        this.spawnCount = spawnCount;
        this.respawnSeconds = respawnSeconds;
        this.chance = chance;
    }

    public static MobRule packDefaults(String id, String entity) {
        MobRule rule = new MobRule();
        rule.id = id == null || id.isBlank() ? generateId() : id;
        rule.name = displayNameFromId(rule.id);
        rule.entity = entity;
        rule.enabled = true;
        rule.spawnType = SpawnType.PACK;
        rule.refillMode = RefillMode.ON_ACTIVATION;
        rule.maxAlive = 10;
        rule.spawnCount = 4;
        rule.respawnSeconds = 900;
        rule.chance = 1.0;
        rule.cooldownStart = CooldownStart.AFTER_ACTIVATION;
        rule.spawnWhenReady = true;
        rule.failedSpawnRetrySeconds = 60;
        rule.despawnWhenZoneInactive = false;
        rule.announceOnSpawn = false;
        rule.boundaryMode = BOUNDARY_LEASH;
        rule.boundaryMaxOutsideSeconds = 10;
        rule.boundaryCheckIntervalTicks = 40;
        rule.boundaryTeleportBack = true;
        rule.spawnMode = SPAWN_MODE_RANDOM_VALID_POSITION;
        rule.spawnTrigger = SPAWN_TRIGGER_TIMER;
        rule.afterDeathDelaySeconds = 30;
        rule.respawnAfterDeath = false;
        rule.respawnAfterDespawn = false;
        rule.allowSmallRoom = true;
        rule.positionAttempts = 128;
        rule.minDistanceBetweenSpawns = 2;
        rule.spreadSpawns = true;
        rule.requirePlayerNearby = false;
        rule.playerActivationRange = 64;
        rule.requireChunkLoaded = true;
        rule.allowForceLoad = false;
        return rule;
    }

    public static MobRule uniqueDefaults(String id, String entity) {
        MobRule rule = new MobRule();
        rule.id = id == null || id.isBlank() ? generateId() : id;
        rule.name = displayNameFromId(rule.id);
        rule.entity = entity;
        rule.enabled = true;
        rule.spawnType = SpawnType.UNIQUE;
        rule.refillMode = RefillMode.AFTER_DEATH;
        rule.maxAlive = 1;
        rule.spawnCount = 1;
        rule.respawnSeconds = 86400;
        rule.chance = 1.0;
        rule.cooldownStart = CooldownStart.AFTER_DEATH;
        rule.spawnWhenReady = true;
        rule.failedSpawnRetrySeconds = 60;
        rule.despawnWhenZoneInactive = false;
        rule.announceOnSpawn = true;
        rule.boundaryMode = BOUNDARY_TELEPORT_BACK;
        rule.boundaryMaxOutsideSeconds = 10;
        rule.boundaryCheckIntervalTicks = 40;
        rule.boundaryTeleportBack = true;
        rule.spawnMode = SPAWN_MODE_BOSS_ROOM;
        rule.spawnTrigger = SPAWN_TRIGGER_AFTER_DEATH;
        rule.afterDeathDelaySeconds = 30;
        rule.respawnAfterDeath = true;
        rule.respawnAfterDespawn = false;
        rule.allowSmallRoom = true;
        rule.positionAttempts = 128;
        rule.minDistanceBetweenSpawns = 2;
        rule.spreadSpawns = true;
        rule.requirePlayerNearby = true;
        rule.playerActivationRange = 64;
        rule.requireChunkLoaded = true;
        rule.allowForceLoad = false;
        return rule;
    }

    public static String generateId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }

    private static String displayNameFromId(String id) {
        if (id == null || id.isBlank()) {
            return "New Mob Rule";
        }
        String[] parts = id.replace('-', '_').split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                name.append(part.substring(1));
            }
        }
        return name.length() == 0 ? id : name.toString();
    }
}
