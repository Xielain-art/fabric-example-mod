package com.gerbarium.regions.runtime;

import com.gerbarium.regions.GerbariumRegionsBridge;
import com.gerbarium.regions.command.CommandFeedback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Method;

public final class BridgeRuntimeReloadDispatcher {
    private static final String ZONES_RUNTIME_MOD_ID = "gerbarium_regions_runtime";
    private static final String RESOURCES_RUNTIME_MOD_ID = "gerbarium_regions_resources_runtime";

    private static final String ZONES_RUNTIME_API = "com.gerbarium.runtime.api.RuntimeReloadApi";
    private static final String RESOURCES_RUNTIME_API = "com.gerbarium.resources.api.ResourcesReloadApi";

    private BridgeRuntimeReloadDispatcher() {
    }

    public static void reloadIfPresent() {
        trigger(ZONES_RUNTIME_MOD_ID, ZONES_RUNTIME_API, "reload");
        trigger(RESOURCES_RUNTIME_MOD_ID, RESOURCES_RUNTIME_API, "reload");
    }

    public static void sendSavedHint(ServerCommandSource source) {
        CommandFeedback.send(source, "Saved. Runtime reload was triggered automatically. If changes do not appear, run /gerbzone reload and /gerbresource reload.");
    }

    private static boolean trigger(String modId, String className, String methodName) {
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName(className);
            Method reloadMethod = apiClass.getMethod(methodName);
            reloadMethod.invoke(null);
            return true;
        } catch (Exception e) {
            GerbariumRegionsBridge.LOGGER.warn("[Gerbarium] Failed to trigger runtime reload API for mod '{}': {}", modId, e.getMessage());
            return false;
        }
    }
}
