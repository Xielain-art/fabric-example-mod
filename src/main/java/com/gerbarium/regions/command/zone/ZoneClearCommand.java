package com.gerbarium.regions.command.zone;

import com.gerbarium.regions.command.CommandFeedback;
import com.gerbarium.regions.model.Zone;
import com.gerbarium.regions.storage.ZoneStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ZoneClearCommand {
    private ZoneClearCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build(ZoneStorage storage) {
        return literal("clear")
                .then(argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            Optional<Zone> optionalZone = storage.findZone(id);

                            if (optionalZone.isEmpty()) {
                                CommandFeedback.error(context.getSource(), "Zone not found: " + id);
                                return 0;
                            }

                            int removed = clearZoneMobs(context.getSource(), optionalZone.get());
                            CommandFeedback.send(context.getSource(), "Removed " + removed + " spawned mob(s) from zone: " + id);
                            return removed;
                        })
                );
    }

    private static int clearZoneMobs(ServerCommandSource source, Zone zone) {
        Identifier dimensionId = Identifier.tryParse(zone.dimension);

        if (dimensionId == null) {
            CommandFeedback.error(source, "Invalid dimension id in zone: " + zone.dimension);
            return 0;
        }

        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        ServerWorld world = source.getServer().getWorld(worldKey);

        if (world == null) {
            CommandFeedback.error(source, "World not loaded: " + zone.dimension);
            return 0;
        }

        String tag = "gerb_zone_" + zone.id;
        Box box = new Box(
                zone.min.x,
                zone.min.y,
                zone.min.z,
                zone.max.x + 1,
                zone.max.y + 1,
                zone.max.z + 1
        );

        List<Entity> entities = world.getEntitiesByType(
                TypeFilter.instanceOf(Entity.class),
                box,
                entity -> entity.getCommandTags().contains(tag)
        );

        for (Entity entity : entities) {
            entity.discard();
        }

        return entities.size();
    }
}