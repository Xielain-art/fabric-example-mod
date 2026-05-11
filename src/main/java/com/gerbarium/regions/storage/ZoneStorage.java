package com.gerbarium.regions.storage;

import com.gerbarium.regions.GerbariumRegionsBridge;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZonesFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ZoneStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Path regionsPath;
    private ZonesFile data;

    public ZoneStorage() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("gerbarium");
        this.regionsPath = configDir.resolve("regions.json");
        this.data = load();
    }

    public ZonesFile getData() {
        return data;
    }

    public String toJson() {
        return GSON.toJson(data);
    }

    public void reload() {
        this.data = load();
    }

    public void save() {
        try {
            Files.createDirectories(configDir);

            try (Writer writer = Files.newBufferedWriter(regionsPath)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("Failed to save regions.json", e);
            throw new RuntimeException("Failed to save regions.json", e);
        }
    }

    public Optional<Zone> findZone(String id) {
        return data.zones.stream()
                .filter(zone -> zone.id.equalsIgnoreCase(id))
                .findFirst();
    }

    public boolean exists(String id) {
        return findZone(id).isPresent();
    }

    public void addZone(Zone zone) {
        data.zones.add(zone);
        save();
    }

    public boolean deleteZone(String id) {
        boolean removed = data.zones.removeIf(zone -> zone.id.equalsIgnoreCase(id));

        if (removed) {
            save();
        }

        return removed;
    }

    private ZonesFile load() {
        try {
            Files.createDirectories(configDir);

            if (!Files.exists(regionsPath)) {
                ZonesFile empty = new ZonesFile();

                try (Writer writer = Files.newBufferedWriter(regionsPath)) {
                    GSON.toJson(empty, writer);
                }

                return empty;
            }

            try (Reader reader = Files.newBufferedReader(regionsPath)) {
                ZonesFile loaded = GSON.fromJson(reader, ZonesFile.class);

                if (loaded == null || loaded.zones == null) {
                    return new ZonesFile();
                }

                for (Zone zone : loaded.zones) {
                    if (zone.mobs == null) {
                        continue;
                    }

                    zone.mobs.forEach(rule -> {
                        if (rule.id == null || rule.id.isBlank()) {
                            rule.id = "legacy_" + rule.entity.replace(':', '_');
                        }
                    });
                }

                return loaded;
            }
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("Failed to load regions.json", e);
            return new ZonesFile();
        }
    }
}