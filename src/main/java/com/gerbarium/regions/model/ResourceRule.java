package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ResourceRule {
    public String id;
    public String name;
    public boolean enabled = true;

    public List<String> targetBlocks = new ArrayList<>();
    public List<WeightedBlock> resourceBlocks = new ArrayList<>();

    public int maxActiveBlocks = 12;
    public int spawnCount = 3;
    public int respawnSeconds = 900;
    public double chance = 1.0;

    public Integer minY = null;
    public Integer maxY = null;

    public ReplaceMode replaceMode = ReplaceMode.ONLY_TARGET_BLOCKS;
    public RestoreMode restoreMode = RestoreMode.RESTORE_ORIGINAL;
    public ResourceActivationMode activationMode = ResourceActivationMode.REAL_TIME;

    public int restoreDelaySeconds = 300;
    public int maxPositionAttempts = 64;
    public boolean requireLoadedChunk = true;
    public boolean respectProtectedBlocks = true;
    public boolean dropOriginalBlockOnReplace = false;
    public boolean restoreIfNotMined = true;
    public boolean preventPlayerPlacedBlocks = true;
    public boolean allowBlockEntities = false;

    public PlacementMode placementMode = PlacementMode.RANDOM_SCATTER;
    public int minDistanceBetweenResources = 3;

    public ResourceRule() {
    }

    public static ResourceRule defaults(String id, String name) {
        ResourceRule rule = new ResourceRule();
        rule.id = id;
        rule.name = name;
        rule.enabled = true;
        return rule;
    }

    public static String generateId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }
}
