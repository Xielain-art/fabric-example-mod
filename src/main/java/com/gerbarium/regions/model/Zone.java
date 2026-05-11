package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;

public class Zone {
    public String id;
    public boolean enabled;
    public String dimension;
    public Vec3iJson min;
    public Vec3iJson max;
    public ZoneActivationSettings activation = new ZoneActivationSettings();
    public ZoneSpawnSettings spawn = new ZoneSpawnSettings();
    public List<MobRule> mobs = new ArrayList<>();

    public Zone() {
    }

    public Zone(String id, boolean enabled, String dimension, Vec3iJson min, Vec3iJson max) {
        this.id = id;
        this.enabled = enabled;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
        this.mobs = new ArrayList<>();
    }
}
