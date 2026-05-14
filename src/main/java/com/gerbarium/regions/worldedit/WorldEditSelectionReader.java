package com.gerbarium.regions.worldedit;

import com.gerbarium.regions.model.Vec3iJson;
import com.gerbarium.regions.model.Zone;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

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

        invoke(selector, "learnChanges");
        setRegionSelector(localSession, world, selector);
        invoke(localSession, "dispatchCUISelection", new Class<?>[]{classFor("com.sk89q.worldedit.entity.Player")}, new Object[]{worldEditPlayer});
    }

    public static void clearSelection(ServerPlayerEntity player) throws Exception {
        Object worldEditPlayer = adaptPlayer(player);
        Object localSession = getLocalSession(worldEditPlayer);
        Object world = invoke(worldEditPlayer, "getWorld");

        Object selector = classFor("com.sk89q.worldedit.regions.selector.CuboidRegionSelector")
                .getConstructor(classFor("com.sk89q.worldedit.world.World"))
                .newInstance(world);

        setRegionSelector(localSession, world, selector);
        invoke(localSession, "dispatchCUISelection", new Class<?>[]{classFor("com.sk89q.worldedit.entity.Player")}, new Object[]{worldEditPlayer});
    }

    private static Object adaptPlayer(ServerPlayerEntity player) throws Exception {
        return invokeStatic("com.sk89q.worldedit.fabric.FabricAdapter", "adaptPlayer",
                new Class<?>[]{ServerPlayerEntity.class}, new Object[]{player});
    }

    private static Object getLocalSession(Object worldEditPlayer) throws Exception {
        Object worldEdit = invokeStatic("com.sk89q.worldedit.WorldEdit", "getInstance", new Class<?>[0], new Object[0]);
        Object sessionManager = invoke(worldEdit, "getSessionManager");
        try {
            return invoke(sessionManager, "get", new Class<?>[]{classFor("com.sk89q.worldedit.extension.platform.Actor")}, new Object[]{worldEditPlayer});
        } catch (NoSuchMethodException ignoredActor) {
            try {
                return invoke(sessionManager, "get", new Class<?>[]{classFor("com.sk89q.worldedit.session.SessionOwner")}, new Object[]{worldEditPlayer});
            } catch (NoSuchMethodException ignoredOwner) {
                return invokeCompatible(sessionManager, "get", worldEditPlayer);
            }
        }
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

    private static Object invokeCompatible(Object target, String method, Object arg) throws Exception {
        Method fallback = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = candidate.getParameterTypes()[0];
            if (arg == null || param.isInstance(arg) || param.isAssignableFrom(arg.getClass())) {
                return candidate.invoke(target, arg);
            }
            fallback = candidate;
        }
        if (fallback != null) {
            return fallback.invoke(target, arg);
        }
        throw new NoSuchMethodException(method + "(compatible)");
    }

    private static void setRegionSelector(Object localSession, Object world, Object selector) throws Exception {
        try {
            invoke(localSession, "setRegionSelector",
                    new Class<?>[]{classFor("com.sk89q.worldedit.world.World"), classFor("com.sk89q.worldedit.regions.selector.RegionSelector")},
                    new Object[]{world, selector});
            return;
        } catch (NoSuchMethodException ignored) {
            invokeCompatibleByArgs(localSession, "setRegionSelector", world, selector);
        }
    }

    private static Object invokeCompatibleByArgs(Object target, String method, Object... args) throws Exception {
        Method fallback = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] params = candidate.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < params.length; i++) {
                Object arg = args[i];
                if (arg != null && !params[i].isInstance(arg) && !params[i].isAssignableFrom(arg.getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return candidate.invoke(target, args);
            }
            fallback = candidate;
        }
        if (fallback != null) {
            return fallback.invoke(target, args);
        }
        throw new NoSuchMethodException(method + "(compatible args)");
    }

    private static Object invokeStatic(String className, String method, Class<?>[] argTypes, Object[] args) throws Exception {
        return classFor(className).getMethod(method, argTypes).invoke(null, args);
    }
}
