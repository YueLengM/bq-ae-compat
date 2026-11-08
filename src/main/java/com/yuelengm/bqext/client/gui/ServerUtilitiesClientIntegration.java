package com.yuelengm.bqext.client.gui;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ServerUtilitiesClientIntegration {

    private ServerUtilitiesClientIntegration() {}

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new QuestBookSidebarButton());
    }
}
