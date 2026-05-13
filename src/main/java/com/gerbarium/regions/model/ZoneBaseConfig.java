package com.gerbarium.regions.model;

public class ZoneBaseConfig {
    public int version = 1;
    public String id;
    public String name;
    public boolean enabled = true;
    public String dimension;
    public Vec3iJson min;
    public Vec3iJson max;
    public ZoneActivationSettings activation = new ZoneActivationSettings();
}