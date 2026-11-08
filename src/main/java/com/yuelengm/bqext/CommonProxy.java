package com.yuelengm.bqext;

import com.yuelengm.bqext.loader.Ae2Integration;
import com.yuelengm.bqext.loader.ModIds;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        if (Loader.isModLoaded(ModIds.APPLIED_ENERGISTICS)) {
            Ae2Integration.preInit();
        }

        BQExt.LOG.info("BQExt at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded(ModIds.APPLIED_ENERGISTICS)) {
            Ae2Integration.init();
        }
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}

    public void serverStopped(FMLServerStoppedEvent event) {
        if (Loader.isModLoaded(ModIds.APPLIED_ENERGISTICS)) {
            Ae2Integration.serverStopped();
        }
    }
}
