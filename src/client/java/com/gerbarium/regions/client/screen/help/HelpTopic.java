package com.gerbarium.regions.client.screen.help;

public enum HelpTopic {
    ZONE_RUNTIME_SETTINGS("Zone Runtime Settings"),
    MOB_RULE_BASICS("Mob Rule: Basics"),
    MOB_RULE_SPAWN("Mob Rule: Spawn Settings"),
    MOB_RULE_PLACEMENT("Mob Rule: Placement"),
    MOB_RULE_ADVANCED("Mob Rule: Advanced"),
    MOB_RULE_BOUNDARY("Mob Rule: Boundary Control"),
    RESOURCE_RULE_BASICS("Resource Rule: Basics"),
    RESOURCE_RULE_BLOCKS("Resource Rule: Blocks"),
    RESOURCE_RULE_LIMITS("Resource Rule: Limits"),
    RESOURCE_RULE_SAFETY("Resource Rule: Safety"),
    COMPANION_EDIT("Companion Settings");

    public final String title;

    HelpTopic(String title) {
        this.title = title;
    }
}
