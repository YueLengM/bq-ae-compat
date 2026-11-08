package com.yuelengm.bqext.ae;

import net.minecraftforge.common.MinecraftForge;

import com.yuelengm.bqext.client.texture.BqInterfaceTextureHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class Ae2ClientIntegration {

    private Ae2ClientIntegration() {}

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new BqInterfaceTextureHandler());
    }
}
