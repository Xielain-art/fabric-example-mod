package com.gerbarium.regions.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class RuleJsonMigration {
    private RuleJsonMigration() {
    }

    public static void migrateZone(JsonObject zoneJson) {
        if (zoneJson == null) {
            return;
        }

        JsonElement mobsElement = zoneJson.get("mobs");
        if (mobsElement != null && mobsElement.isJsonArray()) {
            for (JsonElement mobElement : mobsElement.getAsJsonArray()) {
                if (mobElement != null && mobElement.isJsonObject()) {
                    migrateMobRule(mobElement.getAsJsonObject());
                }
            }
        }
    }

    public static void migrateZonesFile(JsonObject zonesFileJson) {
        if (zonesFileJson == null) {
            return;
        }

        JsonElement zonesElement = zonesFileJson.get("zones");
        if (zonesElement != null && zonesElement.isJsonArray()) {
            for (JsonElement zoneElement : zonesElement.getAsJsonArray()) {
                if (zoneElement != null && zoneElement.isJsonObject()) {
                    migrateZone(zoneElement.getAsJsonObject());
                }
            }
        }
    }

    public static void migrateMobRule(JsonObject mobRuleJson) {
        if (mobRuleJson == null) {
            return;
        }

        copyLegacyId(mobRuleJson);

        JsonElement companionsElement = mobRuleJson.get("companions");
        if (companionsElement != null && companionsElement.isJsonArray()) {
            JsonArray companions = companionsElement.getAsJsonArray();
            for (JsonElement companionElement : companions) {
                if (companionElement != null && companionElement.isJsonObject()) {
                    migrateCompanionRule(companionElement.getAsJsonObject());
                }
            }
        }
    }

    public static void migrateCompanionRule(JsonObject companionRuleJson) {
        if (companionRuleJson == null) {
            return;
        }

        copyLegacyId(companionRuleJson);
    }

    private static void copyLegacyId(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        String id = readString(jsonObject, "id");
        String legacyId = readString(jsonObject, "uid64");

        if ((id == null || id.isBlank()) && legacyId != null && !legacyId.isBlank()) {
            jsonObject.addProperty("id", legacyId);
        }

        jsonObject.remove("uid64");
    }

    private static String readString(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
}
