package com.yuelengm.bqext.ae;

import net.minecraftforge.common.MinecraftForge;

import com.yuelengm.bqext.ModBlocks;
import com.yuelengm.bqext.ModRecipes;

import cpw.mods.fml.common.FMLCommonHandler;

public final class Ae2Integration {

    private Ae2Integration() {}

    public static void preInit() {
        ModBlocks.register();

        BqInterfaceEventHandler handler = new BqInterfaceEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }

    public static void init() {
        ModRecipes.register();
    }

    public static void serverStopped() {
        AeCheckScheduler.clear();
    }
}
