package com.gerbarium.regions.client;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class GerbariumRegionsBridgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GerbariumClientNetworking.register();
    }
}