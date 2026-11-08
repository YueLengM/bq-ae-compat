package com.yuelengm.bqext.loader;

import com.yuelengm.bqext.BQExt;
import com.yuelengm.bqext.block.BlockBqInterface;
import com.yuelengm.bqext.tile.TileBqInterface;

import appeng.block.AEBaseItemBlock;
import appeng.core.CreativeTab;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static BlockBqInterface BQ_INTERFACE;

    private ModBlocks() {}

    public static void register() {
        BQ_INTERFACE = new BlockBqInterface();
        BQ_INTERFACE.setCreativeTab(CreativeTab.instance);

        GameRegistry.registerBlock(BQ_INTERFACE, AEBaseItemBlock.class, "bq_interface");
        GameRegistry.registerTileEntity(TileBqInterface.class, BQExt.MODID + ":bq_interface");
    }
}
