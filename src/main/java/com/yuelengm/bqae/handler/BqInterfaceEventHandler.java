package com.yuelengm.bqae.handler;

import net.minecraftforge.event.world.ChunkEvent;

import com.yuelengm.bqae.index.BqInterfaceIndex;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BqInterfaceEventHandler {

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world.isRemote) return;

        BqInterfaceIndex index = BqInterfaceIndex.get(event.world);
        if (index == null) return;

        index.validateChunk(event.world, event.getChunk().xPosition, event.getChunk().zPosition);
    }
}
