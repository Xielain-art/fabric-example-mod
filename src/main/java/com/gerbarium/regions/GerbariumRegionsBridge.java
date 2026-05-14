package com.gerbarium.regions;

import com.gerbarium.regions.command.ZoneCommands;
import com.gerbarium.regions.network.GerbariumServerNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerbariumRegionsBridge implements ModInitializer {
    public static final String MOD_ID = "gerbarium_regions_bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        printStartupBanner();

        ZoneCommands.register();
        GerbariumServerNetworking.register();

        LOGGER.info("[Gerbarium] Commands registered successfully.");
        LOGGER.info("[Gerbarium] Networking registered successfully.");
        LOGGER.info("[Gerbarium] Regions Bridge is ready.");
    }

    private void printStartupBanner() {
        LOGGER.info("");
        LOGGER.info("================================================================================");
        LOGGER.info("   _____           _                 _                 ");
        LOGGER.info("  / ____|         | |               (_)                ");
        LOGGER.info(" | |  __  ___ _ __| |__   __ _ _ __ _ _   _ _ __ ___  ");
        LOGGER.info(" | | |_ |/ _ \\ '__| '_ \\ / _` | '__| | | | | '_ ` _ \\ ");
        LOGGER.info(" | |__| |  __/ |  | |_) | (_| | |  | | |_| | | | | | |");
        LOGGER.info("  \\_____|\\___|_|  |_.__/ \\__,_|_|  |_|\\__,_|_| |_| |_|");
        LOGGER.info("");
        LOGGER.info("                     REGIONS BRIDGE");
        LOGGER.info("================================================================================");
        LOGGER.info("  Mod ID:      {}", MOD_ID);
        LOGGER.info("  Version:     1.0.0");
        LOGGER.info("  Environment: CLIENT + SERVER");
        LOGGER.info("  Purpose:     Modular zone config editor/admin GUI");
        LOGGER.info("--------------------------------------------------------------------------------");
        LOGGER.info("  Status: LOADED");
        LOGGER.info("================================================================================");
        LOGGER.info("");
    }
}
