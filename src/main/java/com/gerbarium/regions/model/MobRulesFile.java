package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;

public class MobRulesFile {
    public int version = 1;
    public String zoneId;
    public ZoneSpawnSettings spawn = new ZoneSpawnSettings();
    public List<MobRule> rules = new ArrayList<>();
}