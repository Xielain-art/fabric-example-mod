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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class ZoneStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ZoneStorage INSTANCE = new ZoneStorage();

    public static ZoneStorage getInstance() {
        return INSTANCE;
    }

    private final Path configDir;
    private final Path zonesDir;

    // Старый файл. Только для миграции, больше как основной storage не используется.
    private final Path legacyRegionsPath;

    private ZonesFile data;

    private ZoneStorage() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("gerbarium");
        this.zonesDir = configDir.resolve("zones");
        this.legacyRegionsPath = configDir.resolve("regions.json");

        this.data = load();

        GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Zones storage dir: {}", zonesDir.toAbsolutePath());
    }

    public synchronized ZonesFile getData() {
        return data;
    }

    public synchronized String toJson() {
        // Клиенту всё ещё отдаём общий JSON, но собираем его из отдельных файлов зон.
        return GSON.toJson(data);
    }

    public synchronized void reload() {
        this.data = load();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(zonesDir);

            for (Zone zone : data.zones) {
                saveZone(zone);
            }

            GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Saved {} zone file(s) to {}", data.zones.size(), zonesDir.toAbsolutePath());
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to save zone files to {}", zonesDir.toAbsolutePath(), e);
            throw new RuntimeException("Failed to save zone files", e);
        }
    }

    public synchronized Optional<Zone> findZone(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return data.zones.stream()
                .filter(zone -> zone.id != null && zone.id.equalsIgnoreCase(id))
                .findFirst();
    }

    public synchronized boolean exists(String id) {
        reload();
        return findZone(id).isPresent();
    }

    public synchronized void addZone(Zone zone) {
        if (zone == null || zone.id == null || zone.id.isBlank()) {
            throw new IllegalArgumentException("Zone id cannot be empty");
        }

        normalizeZone(zone);

        data.zones.removeIf(existing -> existing.id != null && existing.id.equalsIgnoreCase(zone.id));
        data.zones.add(zone);
        sortZones();

        try {
            Files.createDirectories(zonesDir);
            saveZone(zone);
            GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Added zone '{}' as {}", zone.id, zonePath(zone.id).toAbsolutePath());
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to save zone '{}'", zone.id, e);
            throw new RuntimeException("Failed to save zone: " + zone.id, e);
        }
    }

    public synchronized boolean deleteZone(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        reload();

        Optional<Zone> optionalZone = findZone(id);

        if (optionalZone.isEmpty()) {
            GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Delete zone request: id={}, removed=false, reason=not_found", id);
            return false;
        }

        Zone zone = optionalZone.get();
        Path path = zonePath(zone.id);

        boolean removedFromMemory = data.zones.removeIf(existing -> existing.id != null && existing.id.equalsIgnoreCase(id));

        boolean deletedFile = false;

        try {
            deletedFile = Files.deleteIfExists(path);
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to delete zone file {}", path.toAbsolutePath(), e);
            throw new RuntimeException("Failed to delete zone file: " + path.toAbsolutePath(), e);
        }

        GerbariumRegionsBridge.LOGGER.info(
                "[Gerbarium] Delete zone request: id={}, removedFromMemory={}, deletedFile={}, path={}",
                id,
                removedFromMemory,
                deletedFile,
                path.toAbsolutePath()
        );

        reload();

        return removedFromMemory || deletedFile;
    }

    private ZonesFile load() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(zonesDir);

            ZonesFile loaded = loadFromZoneFiles();

            // Если новая папка zones пустая, пробуем мигрировать старый config/gerbarium/regions.json.
            if (loaded.zones.isEmpty() && Files.exists(legacyRegionsPath)) {
                ZonesFile legacy = loadLegacyRegionsJson();

                if (!legacy.zones.isEmpty()) {
                    GerbariumRegionsBridge.LOGGER.info(
                            "[Gerbarium] Migrating {} zone(s) from legacy regions.json to zones/*.json",
                            legacy.zones.size()
                    );

                    for (Zone zone : legacy.zones) {
                        normalizeZone(zone);
                        saveZone(zone);
                    }

                    loaded = loadFromZoneFiles();
                }
            }

            sortZones(loaded);
            return loaded;
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load zones from {}", zonesDir.toAbsolutePath(), e);

            ZonesFile fallback = new ZonesFile();

            if (fallback.zones == null) {
                fallback.zones = new ArrayList<>();
            }

            return fallback;
        }
    }

    private ZonesFile loadFromZoneFiles() throws IOException {
        ZonesFile result = new ZonesFile();

        if (result.zones == null) {
            result.zones = new ArrayList<>();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(zonesDir, "*.json")) {
            for (Path path : stream) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    Zone zone = GSON.fromJson(reader, Zone.class);

                    if (zone == null) {
                        GerbariumRegionsBridge.LOGGER.warn("[Gerbarium] Skipped empty zone file: {}", path.toAbsolutePath());
                        continue;
                    }

                    if (zone.id == null || zone.id.isBlank()) {
                        String fileName = path.getFileName().toString();
                        zone.id = fileName.endsWith(".json")
                                ? fileName.substring(0, fileName.length() - ".json".length())
                                : fileName;
                    }

                    normalizeZone(zone);
                    result.zones.add(zone);
                } catch (Exception e) {
                    GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load zone file: {}", path.toAbsolutePath(), e);
                }
            }
        }

        return result;
    }

    private ZonesFile loadLegacyRegionsJson() {
        try (Reader reader = Files.newBufferedReader(legacyRegionsPath)) {
            ZonesFile loaded = GSON.fromJson(reader, ZonesFile.class);

            if (loaded == null) {
                return emptyZonesFile();
            }

            if (loaded.zones == null) {
                loaded.zones = new ArrayList<>();
            }

            for (Zone zone : loaded.zones) {
                normalizeZone(zone);
            }

            return loaded;
        } catch (Exception e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load legacy regions.json: {}", legacyRegionsPath.toAbsolutePath(), e);
            return emptyZonesFile();
        }
    }

    private void saveZone(Zone zone) throws IOException {
        normalizeZone(zone);

        Path path = zonePath(zone.id);

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(zone, writer);
        }
    }

    private Path zonePath(String zoneId) {
        return zonesDir.resolve(safeFileName(zoneId) + ".json");
    }

    private String safeFileName(String zoneId) {
        return zoneId
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-]", "_");
    }

    private void normalizeZone(Zone zone) {
        if (zone.mobs == null) {
            zone.mobs = new ArrayList<>();
        }

        zone.mobs.forEach(rule -> {
            if (rule.id == null || rule.id.isBlank()) {
                rule.id = "legacy_" + rule.entity.replace(':', '_');
            }
        });
    }

    private ZonesFile emptyZonesFile() {
        ZonesFile file = new ZonesFile();

        if (file.zones == null) {
            file.zones = new ArrayList<>();
        }

        return file;
    }

    private void sortZones() {
        sortZones(data);
    }

    private void sortZones(ZonesFile file) {
        if (file == null || file.zones == null) {
            return;
        }

        file.zones.sort(Comparator.comparing(zone -> zone.id == null ? "" : zone.id.toLowerCase()));
    }
}