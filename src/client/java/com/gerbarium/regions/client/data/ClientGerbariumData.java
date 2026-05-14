package com.gerbarium.regions.client.data;

import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneDefaults;
import com.gerbarium.regions.model.ZonesFile;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClientGerbariumData {
    private static final Gson GSON = new Gson();

    private static ZonesFile zonesFile = new ZonesFile();
    private static List<String> entityIds = new ArrayList<>();
    private static List<String> blockIds = new ArrayList<>();

    private ClientGerbariumData() {
    }

    public static void setZonesJson(String json) {
        ZonesFile parsed = GSON.fromJson(json, ZonesFile.class);

        if (parsed == null || parsed.zones == null) {
            zonesFile = new ZonesFile();
            return;
        }

        zonesFile = parsed;
        for (Zone zone : zonesFile.zones) {
            ZoneDefaults.normalizeZone(zone);
        }
    }

    public static ZonesFile zonesFile() {
        return zonesFile;
    }

    public static Optional<Zone> findZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return Optional.empty();
        }

        return zonesFile.zones.stream()
                .filter(zone -> zone.id.equalsIgnoreCase(zoneId))
                .findFirst();
    }

    public static List<String> entityIds() {
        return entityIds;
    }

    public static void setEntityIds(List<String> ids) {
        entityIds = ids == null ? new ArrayList<>() : ids;
    }

    public static List<String> blockIds() {
        return blockIds;
    }

    public static void setBlockIds(List<String> ids) {
        blockIds = ids == null ? new ArrayList<>() : ids;
    }
}
