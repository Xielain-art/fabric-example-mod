package com.gerbarium.regions.client;

import com.gerbarium.regions.client.network.GerbariumClientNetworking;
import com.gerbarium.regions.client.screen.help.HelpRegistry;
import net.fabricmc.api.ClientModInitializer;

public class GerbariumRegionsBridgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HelpRegistry.init();
        GerbariumClientNetworking.register();
    }
}