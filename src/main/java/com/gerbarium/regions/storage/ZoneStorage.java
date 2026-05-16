package com.gerbarium.regions.storage;

import com.gerbarium.regions.GerbariumRegionsBridge;
import com.gerbarium.regions.model.MobRulesFile;
import com.gerbarium.regions.model.ResourceRulesFile;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.model.ZoneBaseConfig;
import com.gerbarium.regions.model.ZoneDefaults;
import com.gerbarium.regions.model.ZonesFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
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

    private ZonesFile data;

    private ZoneStorage() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("gerbarium");
        this.zonesDir = configDir.resolve("zones");

        this.data = load();

        GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Zones storage dir: {}", zonesDir.toAbsolutePath());
    }

    public synchronized ZonesFile getData() {
        return data;
    }

    public synchronized String toJson() {
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

        ZoneDefaults.normalizeZone(zone);
        ZoneDefaults.validateZone(zone);

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
        Path zoneDir = zonePath(zone.id);

        boolean removedFromMemory = data.zones.removeIf(existing -> existing.id != null && existing.id.equalsIgnoreCase(id));

        boolean deletedFolder = false;

        try {
            if (Files.exists(zoneDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(zoneDir)) {
                    for (Path file : stream) {
                        Files.deleteIfExists(file);
                    }
                }
                Files.deleteIfExists(zoneDir);
                deletedFolder = true;
            }
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to delete zone folder {}", zoneDir.toAbsolutePath(), e);
            throw new RuntimeException("Failed to delete zone folder: " + zoneDir.toAbsolutePath(), e);
        }

        if (removedFromMemory) {
            sortZones();
        }

        GerbariumRegionsBridge.LOGGER.info("[Gerbarium] Delete zone request: id={}, removedFromMemory={}, deletedFolder={}", id, removedFromMemory, deletedFolder);
        return removedFromMemory;
    }

    private ZonesFile load() {
        try {
            Files.createDirectories(zonesDir);

            ZonesFile loaded = loadFromZoneFolders();

            if (loaded.zones == null) {
                loaded.zones = new ArrayList<>();
            }

            sortZones(loaded);

            GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Loaded zones: total={}", loaded.zones.size());
            return loaded;
        } catch (IOException e) {
            GerbariumRegionsBridge.LOGGER.error("[GerbariumBridge] Failed to load zones from {}", zonesDir.toAbsolutePath(), e);

            ZonesFile fallback = new ZonesFile();

            if (fallback.zones == null) {
                fallback.zones = new ArrayList<>();
            }

            return fallback;
        }
    }

    private ZonesFile loadFromZoneFolders() throws IOException {
        ZonesFile result = new ZonesFile();
        if (result.zones == null) {
            result.zones = new ArrayList<>();
        }

        // Log and skip flat .json files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(zonesDir, "*.json")) {
            for (Path path : stream) {
                GerbariumRegionsBridge.LOGGER.warn("[Gerbarium] Ignoring legacy flat zone file {}. Modular folder format is required.", path.getFileName());
            }
        }

        // Scan subdirectories
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(zonesDir)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;

                String folderName = path.getFileName().toString();
                Path zoneJsonPath = path.resolve("zone.json");

                if (!Files.exists(zoneJsonPath)) {
                    GerbariumRegionsBridge.LOGGER.warn("[Gerbarium] Skipping folder {} - no zone.json found", folderName);
                    continue;
                }

                ZoneBaseConfig base;
                try (Reader reader = Files.newBufferedReader(zoneJsonPath)) {
                    base = GSON.fromJson(JsonParser.parseReader(reader), ZoneBaseConfig.class);
                } catch (Exception e) {
                    GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load zone.json for folder {}. Skipping zone.", folderName, e);
                    continue;
                }

                if (base == null || base.id == null || base.id.isBlank()) {
                    GerbariumRegionsBridge.LOGGER.warn("[Gerbarium] Skipping {} - invalid zone.json", folderName);
                    continue;
                }

                if (!base.id.equals(folderName)) {
                    GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Zone id '{}' in zone.json does not match folder name '{}'. Skipping.", base.id, folderName);
                    continue;
                }

                MobRulesFile mobs = null;
                Path mobsPath = path.resolve("mobs.json");
                if (Files.exists(mobsPath)) {
                    try (Reader reader = Files.newBufferedReader(mobsPath)) {
                        mobs = GSON.fromJson(JsonParser.parseReader(reader), MobRulesFile.class);
                    } catch (Exception e) {
                        GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load mobs.json for zone '{}'. Using empty mob rules.", base.id, e);
                        mobs = null;
                    }
                    if (mobs != null && mobs.zoneId != null && !mobs.zoneId.equals(base.id)) {
                        GerbariumRegionsBridge.LOGGER.error("[Gerbarium] mobs.json zoneId '{}' does not match zone '{}'. Skipping mobs.", mobs.zoneId, base.id);
                        mobs = null;
                    }
                }

                ResourceRulesFile resources = null;
                Path resourcesPath = path.resolve("resources.json");
                if (Files.exists(resourcesPath)) {
                    try (Reader reader = Files.newBufferedReader(resourcesPath)) {
                        resources = GSON.fromJson(JsonParser.parseReader(reader), ResourceRulesFile.class);
                    } catch (Exception e) {
                        GerbariumRegionsBridge.LOGGER.error("[Gerbarium] Failed to load resources.json for zone '{}'. Using empty resource rules.", base.id, e);
                        resources = null;
                    }
                    if (resources != null && resources.zoneId != null && !resources.zoneId.equals(base.id)) {
                        GerbariumRegionsBridge.LOGGER.error("[Gerbarium] resources.json zoneId '{}' does not match zone '{}'. Skipping resources.", resources.zoneId, base.id);
                        resources = null;
                    }
                }

                Zone zone = zoneFromFiles(base, mobs, resources);
                ZoneDefaults.normalizeZone(zone);
                result.zones.add(zone);
            }
        }

        return result;
    }

    private Zone zoneFromFiles(ZoneBaseConfig base, MobRulesFile mobs, ResourceRulesFile resources) {
        Zone zone = new Zone();
        zone.id = base.id;
        zone.name = base.name;
        zone.enabled = base.enabled;
        zone.dimension = base.dimension;
        zone.min = base.min;
        zone.max = base.max;
        zone.activation = base.activation;
        zone.version = base.version;
        if (mobs != null) {
            zone.spawn = mobs.spawn;
            zone.mobs = mobs.rules;
        }
        if (resources != null) {
            zone.resources = resources.rules;
        }
        return zone;
    }

    private ZoneBaseConfig zoneToBaseConfig(Zone zone) {
        ZoneBaseConfig base = new ZoneBaseConfig();
        base.version = zone.version;
        base.id = zone.id;
        base.name = zone.name != null ? zone.name : zone.id;
        base.enabled = zone.enabled;
        base.dimension = zone.dimension;
        base.min = zone.min;
        base.max = zone.max;
        base.activation = zone.activation;
        return base;
    }

    private MobRulesFile zoneToMobsFile(Zone zone) {
        MobRulesFile mobs = new MobRulesFile();
        mobs.version = zone.version;
        mobs.zoneId = zone.id;
        mobs.spawn = zone.spawn;
        mobs.rules = zone.mobs;
        return mobs;
    }

    private ResourceRulesFile zoneToResourcesFile(Zone zone) {
        ResourceRulesFile resources = new ResourceRulesFile();
        resources.version = zone.version;
        resources.zoneId = zone.id;
        resources.rules = zone.resources;
        return resources;
    }

    private void saveZone(Zone zone) throws IOException {
        ZoneDefaults.normalizeZone(zone);
        ZoneDefaults.validateZone(zone);

        Path zoneDir = zonesDir.resolve(zone.id);
        Files.createDirectories(zoneDir);

        // Write zone.json
        ZoneBaseConfig base = zoneToBaseConfig(zone);
        writeJsonAtomic(zoneDir.resolve("zone.json"), base);

        // Write mobs.json
        MobRulesFile mobs = zoneToMobsFile(zone);
        writeJsonAtomic(zoneDir.resolve("mobs.json"), mobs);

        // Write resources.json
        ResourceRulesFile resources = zoneToResourcesFile(zone);
        writeJsonAtomic(zoneDir.resolve("resources.json"), resources);

        int mobRules = zone.mobs == null ? 0 : zone.mobs.size();
        int resourceRules = zone.resources == null ? 0 : zone.resources.size();
        GerbariumRegionsBridge.LOGGER.info("[GerbariumBridge] Saved zone={} path={} mobRules={} resourceRules={}", zone.id, zoneDir.toAbsolutePath(), mobRules, resourceRules);
    }

    private void writeJsonAtomic(Path target, Object value) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(value, writer);
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path zonePath(String zoneId) {
        return zonesDir.resolve(safeFileName(zoneId));
    }

    private String safeFileName(String zoneId) {
        return zoneId
                .trim()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
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
