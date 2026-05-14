package com.gerbarium.regions.worldedit;

import com.gerbarium.regions.model.Vec3iJson;
import com.gerbarium.regions.model.Zone;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WorldEditSelectionReader {
    private WorldEditSelectionReader() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("worldedit");
    }

    public static Zone readSelection(ServerPlayerEntity player, String zoneId) throws Exception {
        Object worldEditPlayer = adaptPlayer(player);
        Object localSession = getLocalSession(worldEditPlayer);
        Object world = invoke(worldEditPlayer, "getWorld");

        Object region = invoke(localSession, "getSelection", new Class<?>[]{classFor("com.sk89q.worldedit.world.World")}, new Object[]{world});
        Object min = invoke(region, "getMinimumPoint");
        Object max = invoke(region, "getMaximumPoint");

        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();

        return new Zone(
                zoneId,
                null,
                true,
                dimension,
                new Vec3iJson((int) invoke(min, "getBlockX"), (int) invoke(min, "getBlockY"), (int) invoke(min, "getBlockZ")),
                new Vec3iJson((int) invoke(max, "getBlockX"), (int) invoke(max, "getBlockY"), (int) invoke(max, "getBlockZ"))
        );
    }

    public static void applySelection(ServerPlayerEntity player, Zone zone) throws Exception {
        Object worldEditPlayer = adaptPlayer(player);
        Object localSession = getLocalSession(worldEditPlayer);
        Object world = invoke(worldEditPlayer, "getWorld");

        Object min = invokeStatic("com.sk89q.worldedit.math.BlockVector3", "at",
                new Class<?>[]{int.class, int.class, int.class}, new Object[]{zone.min.x, zone.min.y, zone.min.z});
        Object max = invokeStatic("com.sk89q.worldedit.math.BlockVector3", "at",
                new Class<?>[]{int.class, int.class, int.class}, new Object[]{zone.max.x, zone.max.y, zone.max.z});

        Object selector = classFor("com.sk89q.worldedit.regions.selector.CuboidRegionSelector")
                .getConstructor(classFor("com.sk89q.worldedit.world.World"), classFor("com.sk89q.worldedit.math.BlockVector3"), classFor("com.sk89q.worldedit.math.BlockVector3"))
                .newInstance(world, min, max);

        invoke(localSession, "setRegionSelector",
                new Class<?>[]{classFor("com.sk89q.worldedit.world.World"), classFor("com.sk89q.worldedit.regions.selector.RegionSelector")},
                new Object[]{world, selector});
        invoke(selector, "learnChanges");
        invoke(localSession, "dispatchCUISelection", new Class<?>[]{classFor("com.sk89q.worldedit.entity.Player")}, new Object[]{worldEditPlayer});
    }

    public static void clearSelection(ServerPlayerEntity player) throws Exception {
        Object worldEditPlayer = adaptPlayer(player);
        Object localSession = getLocalSession(worldEditPlayer);
        Object world = invoke(worldEditPlayer, "getWorld");

        Object selector = classFor("com.sk89q.worldedit.regions.selector.CuboidRegionSelector")
                .getConstructor(classFor("com.sk89q.worldedit.world.World"))
                .newInstance(world);

        invoke(localSession, "setRegionSelector",
                new Class<?>[]{classFor("com.sk89q.worldedit.world.World"), classFor("com.sk89q.worldedit.regions.selector.RegionSelector")},
                new Object[]{world, selector});
        invoke(localSession, "dispatchCUISelection", new Class<?>[]{classFor("com.sk89q.worldedit.entity.Player")}, new Object[]{worldEditPlayer});
    }

    private static Object adaptPlayer(ServerPlayerEntity player) throws Exception {
        return invokeStatic("com.sk89q.worldedit.fabric.FabricAdapter", "adaptPlayer",
                new Class<?>[]{ServerPlayerEntity.class}, new Object[]{player});
    }

    private static Object getLocalSession(Object worldEditPlayer) throws Exception {
        Object worldEdit = invokeStatic("com.sk89q.worldedit.WorldEdit", "getInstance", new Class<?>[0], new Object[0]);
        Object sessionManager = invoke(worldEdit, "getSessionManager");
        return invoke(sessionManager, "get", new Class<?>[]{classFor("com.sk89q.worldedit.entity.Player")}, new Object[]{worldEditPlayer});
    }

    private static Class<?> classFor(String fqcn) throws ClassNotFoundException {
        return Class.forName(fqcn);
    }

    private static Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object invoke(Object target, String method, Class<?>[] argTypes, Object[] args) throws Exception {
        return target.getClass().getMethod(method, argTypes).invoke(target, args);
    }

    private static Object invokeStatic(String className, String method, Class<?>[] argTypes, Object[] args) throws Exception {
        return classFor(className).getMethod(method, argTypes).invoke(null, args);
    }
}
