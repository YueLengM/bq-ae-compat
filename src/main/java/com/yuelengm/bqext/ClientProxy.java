package com.yuelengm.bqext;

import com.yuelengm.bqext.ae.Ae2ClientIntegration;
import com.yuelengm.bqext.client.gui.ServerUtilitiesClientIntegration;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        if (Loader.isModLoaded(ModIds.APPLIED_ENERGISTICS)) {
            Ae2ClientIntegration.preInit();
        }
        if (Loader.isModLoaded(ModIds.SERVER_UTILITIES)) {
            ServerUtilitiesClientIntegration.preInit();
        }
    }
}
