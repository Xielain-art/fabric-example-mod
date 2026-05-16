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
        if (!isAvailable()) {
            throw new IllegalStateException("WorldEdit is not loaded");
        }
        Object worldEditPlayer = adaptPlayer(player);
        if (worldEditPlayer == null) {
            throw new IllegalStateException("WorldEdit player adapter returned null");
        }
        Object localSession = getLocalSession(worldEditPlayer);
        if (localSession == null) {
            throw new IllegalStateException("WorldEdit local session is null");
        }
        Object world = invoke(worldEditPlayer, "getWorld");
        if (world == null) {
            throw new IllegalStateException("WorldEdit world is null");
        }

        Object region = invoke(localSession, "getSelection", new Class<?>[]{classFor("com.sk89q.worldedit.world.World")}, new Object[]{world});
        if (region == null) {
            throw new IllegalStateException("WorldEdit selection is null. Make a selection with //wand first.");
        }

        // Validate region type supports getMinimumPoint/getMaximumPoint
        String regionClass = region.getClass().getName();
        if (!regionClass.toLowerCase().contains("cuboid")) {
            throw new IllegalStateException("WorldEdit selection must be a cuboid region. Current: " + regionClass);
        }

        Object min = invoke(region, "getMinimumPoint");
        Object max = invoke(region, "getMaximumPoint");
        if (min == null || max == null) {
            throw new IllegalStateException("WorldEdit selection bounds are null");
        }

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
        
        // Try Actor first (common interface)
        Object result = tryGetSession(sessionManager, "com.sk89q.worldedit.extension.platform.Actor", worldEditPlayer);
        if (result != null) return result;
        
        // Try SessionOwner (base interface)
        result = tryGetSession(sessionManager, "com.sk89q.worldedit.session.SessionOwner", worldEditPlayer);
        if (result != null) return result;
        
        // Fallback: find any compatible get method
        return invokeCompatible(sessionManager, "get", worldEditPlayer);
    }
    
    private static Object tryGetSession(Object sessionManager, String argClassName, Object worldEditPlayer) {
        try {
            Class<?> argClass = classFor(argClassName);
            if (argClass.isInstance(worldEditPlayer)) {
                return invoke(sessionManager, "get", new Class<?>[]{argClass}, new Object[]{worldEditPlayer});
            }
        } catch (Exception e) {
            // Expected if class not found or method not found
        }
        return null;
    }

    private static Class<?> classFor(String fqcn) throws ClassNotFoundException {
        return Class.forName(fqcn);
    }

    private static Object invoke(Object target, String method) throws Exception {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    private static Object invoke(Object target, String method, Class<?>[] argTypes, Object[] args) throws Exception {
        try {
            return target.getClass().getMethod(method, argTypes).invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    private static Object invokeStatic(String className, String method, Class<?>[] argTypes, Object[] args) throws Exception {
        try {
            return classFor(className).getMethod(method, argTypes).invoke(null, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    private static Object invokeCompatible(Object target, String method, Object arg) throws Exception {
        Method fallback = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = candidate.getParameterTypes()[0];
            if (arg == null || param.isInstance(arg) || param.isAssignableFrom(arg.getClass())) {
                try {
                    return candidate.invoke(target, arg);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) throw (Exception) cause;
                    if (cause instanceof Error) throw (Error) cause;
                    throw e;
                }
            }
            fallback = candidate;
        }
        if (fallback != null) {
            try {
                return fallback.invoke(target, arg);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw e;
            }
        }
        throw new NoSuchMethodException(method + "(compatible)");
    }

    private static void setRegionSelector(Object localSession, Object world, Object selector) throws Exception {
        try {
            invoke(localSession, "setRegionSelector",
                    new Class<?>[]{classFor("com.sk89q.worldedit.world.World"), classFor("com.sk89q.worldedit.regions.selector.RegionSelector")},
                    new Object[]{world, selector});
            return;
        } catch (NoSuchMethodException | NoSuchMethodError e) {
            invokeCompatibleByArgs(localSession, "setRegionSelector", world, selector);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchMethodException || cause instanceof NoSuchMethodError) {
                invokeCompatibleByArgs(localSession, "setRegionSelector", world, selector);
                return;
            }
            throw e;
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
                try {
                    return candidate.invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) throw (Exception) cause;
                    if (cause instanceof Error) throw (Error) cause;
                    throw e;
                }
            }
            fallback = candidate;
        }
        if (fallback != null) {
            try {
                return fallback.invoke(target, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw e;
            }
        }
        throw new NoSuchMethodException(method + "(compatible args)");
    }
}
