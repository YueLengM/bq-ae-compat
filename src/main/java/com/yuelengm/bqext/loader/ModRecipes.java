package com.yuelengm.bqext.loader;

import net.minecraft.item.ItemStack;

import com.yuelengm.bqext.BQExt;

import appeng.api.AEApi;
import betterquesting.core.BetterQuesting;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ModRecipes {

    private ModRecipes() {}

    public static void register() {
        ItemStack meInterface = AEApi.instance()
            .definitions()
            .blocks()
            .iface()
            .maybeStack(1)
            .orNull();
        if (meInterface == null) {
            BQExt.LOG.error("Unable to register BQ interface recipe: AE ME Interface is unavailable");
            return;
        }

        GameRegistry.addShapelessRecipe(
            new ItemStack(ModBlocks.BQ_INTERFACE),
            new ItemStack(BetterQuesting.submitStation),
            meInterface);
    }
}
