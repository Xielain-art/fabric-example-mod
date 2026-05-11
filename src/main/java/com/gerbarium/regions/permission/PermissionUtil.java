package com.gerbarium.regions.permission;

import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Method;

public class PermissionUtil {
    private static final int FALLBACK_OP_LEVEL = 2;

    private PermissionUtil() {
    }

    public static boolean hasAdminPermission(ServerCommandSource source) {
        return hasPermission(source, "gerbarium.regions.admin", FALLBACK_OP_LEVEL);
    }

    public static boolean hasPermission(ServerCommandSource source, String permission, int fallbackOpLevel) {
        try {
            Class<?> permissionsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");

            Method checkMethod = permissionsClass.getMethod(
                    "check",
                    ServerCommandSource.class,
                    String.class,
                    int.class
            );

            Object result = checkMethod.invoke(null, source, permission, fallbackOpLevel);

            if (result instanceof Boolean allowed) {
                return allowed;
            }

            return source.hasPermissionLevel(fallbackOpLevel);
        } catch (Throwable ignored) {
            return source.hasPermissionLevel(fallbackOpLevel);
        }
    }
}