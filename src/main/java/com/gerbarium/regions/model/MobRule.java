package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobRule {
    public static final String BOUNDARY_NONE = "NONE";
    public static final String BOUNDARY_LEASH = "LEASH";
    public static final String BOUNDARY_TELEPORT_BACK = "TELEPORT_BACK";
    public static final String BOUNDARY_REMOVE_OUTSIDE = "REMOVE_OUTSIDE";

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
    public String boundaryMode = BOUNDARY_LEASH;
    public int boundaryMaxOutsideSeconds = 10;
    public int boundaryCheckIntervalTicks = 40;
    public boolean boundaryTeleportBack = true;
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
        rule.name = id;
        rule.id = generateId();
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
        return rule;
    }

    public static MobRule uniqueDefaults(String id, String entity) {
        MobRule rule = new MobRule();
        rule.name = id;
        rule.id = generateId();
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
        return rule;
    }

    public static String generateId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }
}
