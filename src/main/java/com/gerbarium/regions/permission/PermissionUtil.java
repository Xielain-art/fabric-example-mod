package com.gerbarium.regions.permission;

import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Method;

public final class PermissionUtil {
    private static final int FALLBACK_OP_LEVEL = 2;
    private static final String ADMIN_PERMISSION = "gerbarium.regions.admin";

    private PermissionUtil() {
    }

    public static boolean hasAdminPermission(ServerCommandSource source) {
        return hasPermission(source, ADMIN_PERMISSION, FALLBACK_OP_LEVEL);
    }

    public static boolean hasPermission(ServerCommandSource source, String permission, int fallbackOpLevel) {
        Boolean apiResult = checkViaFabricPermissionsApi(source, permission, fallbackOpLevel);

        if (apiResult != null) {
            return apiResult;
        }

        return source.hasPermissionLevel(fallbackOpLevel);
    }

    private static Boolean checkViaFabricPermissionsApi(ServerCommandSource source, String permission, int fallbackOpLevel) {
        try {
            Class<?> permissionsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");

            // Вариант 1:
            // Permissions.check(ServerCommandSource source, String permission, int defaultRequiredLevel)
            Boolean result = tryInvokeBoolean(
                    permissionsClass,
                    "check",
                    new Class<?>[]{ServerCommandSource.class, String.class, int.class},
                    new Object[]{source, permission, fallbackOpLevel}
            );

            if (result != null) {
                return result;
            }

            // Вариант 2:
            // Permissions.check(ServerCommandSource source, String permission, boolean defaultValue)
            result = tryInvokeBoolean(
                    permissionsClass,
                    "check",
                    new Class<?>[]{ServerCommandSource.class, String.class, boolean.class},
                    new Object[]{source, permission, source.hasPermissionLevel(fallbackOpLevel)}
            );

            if (result != null) {
                return result;
            }

            // Вариант 3:
            // Ищем любой check(source-compatible, String, int)
            for (Method method : permissionsClass.getMethods()) {
                if (!method.getName().equals("check")) {
                    continue;
                }

                Class<?>[] params = method.getParameterTypes();

                if (params.length != 3) {
                    continue;
                }

                if (!params[1].equals(String.class)) {
                    continue;
                }

                if (!params[0].isAssignableFrom(source.getClass())) {
                    continue;
                }

                if (params[2].equals(int.class) || params[2].equals(Integer.class)) {
                    Object raw = method.invoke(null, source, permission, fallbackOpLevel);

                    if (raw instanceof Boolean allowed) {
                        return allowed;
                    }
                }

                if (params[2].equals(boolean.class) || params[2].equals(Boolean.class)) {
                    Object raw = method.invoke(null, source, permission, source.hasPermissionLevel(fallbackOpLevel));

                    if (raw instanceof Boolean allowed) {
                        return allowed;
                    }
                }
            }

            // Вариант 4:
            // Ищем check(source-compatible, String)
            for (Method method : permissionsClass.getMethods()) {
                if (!method.getName().equals("check")) {
                    continue;
                }

                Class<?>[] params = method.getParameterTypes();

                if (params.length != 2) {
                    continue;
                }

                if (!params[1].equals(String.class)) {
                    continue;
                }

                if (!params[0].isAssignableFrom(source.getClass())) {
                    continue;
                }

                Object raw = method.invoke(null, source, permission);

                if (raw instanceof Boolean allowed) {
                    return allowed;
                }
            }

            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Boolean tryInvokeBoolean(Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = targetClass.getMethod(methodName, parameterTypes);
            Object raw = method.invoke(null, args);

            if (raw instanceof Boolean allowed) {
                return allowed;
            }

            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}