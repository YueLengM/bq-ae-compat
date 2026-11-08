package com.yuelengm.bqext.ae;

import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class BqInterfaceEventHandler {

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world.isRemote) return;

        BqInterfaceIndex index = BqInterfaceIndex.get(event.world);
        if (index == null) return;

        index.validateChunk(event.world, event.getChunk().xPosition, event.getChunk().zPosition);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AeCheckScheduler.tick();
        }
    }
}
