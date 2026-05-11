package com.gerbarium.regions.model;

public class MobRule {
    public String id;
    public String entity;
    public int maxAlive;
    public int spawnCount;
    public int respawnSeconds;
    public double chance;

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
}