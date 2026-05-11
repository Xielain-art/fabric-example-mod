package com.gerbarium.regions.network;

import com.gerbarium.regions.GerbariumRegionsBridge;
import net.minecraft.util.Identifier;

public final class GerbariumPackets {
    public static final Identifier OPEN_GUI = new Identifier(GerbariumRegionsBridge.MOD_ID, "open_gui");

    public static final Identifier REQUEST_ZONES = new Identifier(GerbariumRegionsBridge.MOD_ID, "request_zones");
    public static final Identifier SYNC_ZONES = new Identifier(GerbariumRegionsBridge.MOD_ID, "sync_zones");

    public static final Identifier REQUEST_ENTITIES = new Identifier(GerbariumRegionsBridge.MOD_ID, "request_entities");
    public static final Identifier SYNC_ENTITIES = new Identifier(GerbariumRegionsBridge.MOD_ID, "sync_entities");

    public static final Identifier ADD_MOB_RULE = new Identifier(GerbariumRegionsBridge.MOD_ID, "add_mob_rule");
    public static final Identifier REMOVE_MOB_RULE = new Identifier(GerbariumRegionsBridge.MOD_ID, "remove_mob_rule");
    public static final Identifier TOGGLE_ZONE = new Identifier(GerbariumRegionsBridge.MOD_ID, "toggle_zone");
    public static final Identifier SELECT_ZONE = new Identifier(GerbariumRegionsBridge.MOD_ID, "select_zone");

    private GerbariumPackets() {
    }
}