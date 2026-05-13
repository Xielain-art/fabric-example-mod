package com.gerbarium.regions.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZoneDefaultsBoundaryTest {
    @Test
    void normalizeMobRuleAppliesBoundaryDefaults() {
        MobRule rule = new MobRule();
        rule.id = "mob-1";
        rule.name = "Mob One";
        rule.entity = "minecraft:zombie";
        rule.spawnType = SpawnType.PACK;
        rule.refillMode = RefillMode.ON_ACTIVATION;
        rule.maxAlive = 10;
        rule.spawnCount = 4;
        rule.respawnSeconds = 900;
        rule.chance = 1.0;
        rule.failedSpawnRetrySeconds = 60;

        ZoneDefaults.normalizeMobRule(rule);

        assertEquals("LEASH", rule.boundaryMode);
        assertEquals(10, rule.boundaryMaxOutsideSeconds);
        assertEquals(40, rule.boundaryCheckIntervalTicks);
        assertEquals(true, rule.boundaryTeleportBack);
    }

    @Test
    void validateMobRuleRejectsInvalidBoundarySettings() {
        MobRule rule = new MobRule();
        rule.id = "mob-1";
        rule.name = "Mob One";
        rule.entity = "minecraft:zombie";
        rule.spawnType = SpawnType.PACK;
        rule.refillMode = RefillMode.ON_ACTIVATION;
        rule.maxAlive = 10;
        rule.spawnCount = 4;
        rule.respawnSeconds = 900;
        rule.chance = 1.0;
        rule.failedSpawnRetrySeconds = 60;
        rule.boundaryMode = "NOPE";
        rule.boundaryMaxOutsideSeconds = -1;
        rule.boundaryCheckIntervalTicks = 10;

        assertThrows(IllegalArgumentException.class, () -> ZoneDefaults.validateMobRule(rule));
    }

    @Test
    void gsonSerializesBoundaryFieldsWithCamelCaseNames() {
        MobRule rule = MobRule.packDefaults("mob-1", "minecraft:zombie");
        rule.boundaryMode = "REMOVE_OUTSIDE";
        rule.boundaryMaxOutsideSeconds = 12;
        rule.boundaryCheckIntervalTicks = 60;
        rule.boundaryTeleportBack = false;

        JsonObject json = new Gson().toJsonTree(rule).getAsJsonObject();

        assertEquals("REMOVE_OUTSIDE", json.get("boundaryMode").getAsString());
        assertEquals(12, json.get("boundaryMaxOutsideSeconds").getAsInt());
        assertEquals(60, json.get("boundaryCheckIntervalTicks").getAsInt());
        assertEquals(false, json.get("boundaryTeleportBack").getAsBoolean());
    }
}
