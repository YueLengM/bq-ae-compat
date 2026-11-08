package com.yuelengm.bqext;

import net.minecraftforge.common.MinecraftForge;

import com.yuelengm.bqext.client.gui.QuestBookSidebarButton;
import com.yuelengm.bqext.client.texture.BqInterfaceTextureHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new BqInterfaceTextureHandler());
        MinecraftForge.EVENT_BUS.register(new QuestBookSidebarButton());
    }
}
