package com.gerbarium.regions.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceRulesFile {
    public int version = 1;
    public String zoneId;
    public List<ResourceRule> rules = new ArrayList<>();
}