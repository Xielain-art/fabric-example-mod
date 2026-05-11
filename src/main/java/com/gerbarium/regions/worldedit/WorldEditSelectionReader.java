package com.gerbarium.regions.worldedit;

import com.gerbarium.regions.model.Vec3iJson;
import com.gerbarium.regions.model.Zone;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import net.minecraft.server.network.ServerPlayerEntity;

public class WorldEditSelectionReader {
    public static Zone readSelection(ServerPlayerEntity player, String zoneId) throws IncompleteRegionException {
        com.sk89q.worldedit.entity.Player worldEditPlayer = FabricAdapter.adaptPlayer(player);

        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(worldEditPlayer);

        Region region = session.getSelection(worldEditPlayer.getWorld());

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        String dimension = player.getServerWorld()
                .getRegistryKey()
                .getValue()
                .toString();

        return new Zone(
                zoneId,
                true,
                dimension,
                new Vec3iJson(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
                new Vec3iJson(max.getBlockX(), max.getBlockY(), max.getBlockZ())
        );
    }

    public static void applySelection(ServerPlayerEntity player, Zone zone) {
        com.sk89q.worldedit.entity.Player worldEditPlayer = FabricAdapter.adaptPlayer(player);

        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(worldEditPlayer);

        BlockVector3 min = BlockVector3.at(zone.min.x, zone.min.y, zone.min.z);
        BlockVector3 max = BlockVector3.at(zone.max.x, zone.max.y, zone.max.z);

        CuboidRegionSelector selector = new CuboidRegionSelector(worldEditPlayer.getWorld(), min, max);

        session.setRegionSelector(worldEditPlayer.getWorld(), selector);
        selector.learnChanges();

        session.dispatchCUISelection(worldEditPlayer);
    }

    public static void clearSelection(ServerPlayerEntity player) {
        com.sk89q.worldedit.entity.Player worldEditPlayer = FabricAdapter.adaptPlayer(player);

        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(worldEditPlayer);

        CuboidRegionSelector selector = new CuboidRegionSelector(worldEditPlayer.getWorld());

        session.setRegionSelector(worldEditPlayer.getWorld(), selector);
        session.dispatchCUISelection(worldEditPlayer);
    }
}