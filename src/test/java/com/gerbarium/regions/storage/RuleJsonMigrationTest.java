package com.gerbarium.regions.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuleJsonMigrationTest {
    @Test
    void migratesLegacyMobAndCompanionIdsFromUid64() {
        JsonObject companion = new JsonObject();
        companion.addProperty("uid64", "comp-legacy-1");
        companion.addProperty("name", "Companion One");
        companion.addProperty("entity", "minecraft:zombie");
        companion.addProperty("count", 1);
        companion.addProperty("radius", 8);
        companion.addProperty("chance", 1.0);

        JsonArray companions = new JsonArray();
        companions.add(companion);

        JsonObject mob = new JsonObject();
        mob.addProperty("uid64", "mob-legacy-1");
        mob.addProperty("name", "Mob One");
        mob.addProperty("entity", "minecraft:zombie");
        mob.addProperty("spawnType", "PACK");
        mob.addProperty("refillMode", "ON_ACTIVATION");
        mob.addProperty("maxAlive", 10);
        mob.addProperty("spawnCount", 4);
        mob.addProperty("respawnSeconds", 900);
        mob.addProperty("chance", 1.0);
        mob.addProperty("failedSpawnRetrySeconds", 60);
        mob.add("companions", companions);

        JsonArray mobs = new JsonArray();
        mobs.add(mob);

        JsonObject zone = new JsonObject();
        zone.add("mobs", mobs);

        RuleJsonMigration.migrateZone(zone);

        JsonObject migratedMob = zone.getAsJsonArray("mobs").get(0).getAsJsonObject();
        JsonObject migratedCompanion = migratedMob.getAsJsonArray("companions").get(0).getAsJsonObject();

        assertEquals("mob-legacy-1", migratedMob.get("id").getAsString());
        assertFalse(migratedMob.has("uid64"));
        assertEquals("comp-legacy-1", migratedCompanion.get("id").getAsString());
        assertFalse(migratedCompanion.has("uid64"));
    }
}
