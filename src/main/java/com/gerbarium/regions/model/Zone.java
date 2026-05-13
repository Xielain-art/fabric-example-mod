package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;

public class Zone {
    public String id;
    public String name;
    public boolean enabled;
    public String dimension;
    public Vec3iJson min;
    public Vec3iJson max;
    public ZoneActivationSettings activation = new ZoneActivationSettings();
    public ZoneSpawnSettings spawn = new ZoneSpawnSettings();
    public List<MobRule> mobs = new ArrayList<>();
    public List<ResourceRule> resources = new ArrayList<>();
    public int version = 1;

    public Zone() {
    }

    public Zone(String id, String name, boolean enabled, String dimension, Vec3iJson min, Vec3iJson max) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
        this.mobs = new ArrayList<>();
        this.resources = new ArrayList<>();
    }
}
