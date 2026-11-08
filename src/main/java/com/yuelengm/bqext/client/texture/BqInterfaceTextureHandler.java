package com.yuelengm.bqext.client.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.yuelengm.bqext.loader.ModBlocks;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BqInterfaceTextureHandler {

    private static final String SPRITE_NAME = "bqext:bq_interface";

    @SubscribeEvent
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        event.map.setTextureEntry(SPRITE_NAME, new BqInterfaceSprite(SPRITE_NAME));
    }

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || ModBlocks.BQ_INTERFACE == null) return;

        TextureAtlasSprite sprite = event.map.getAtlasSprite(SPRITE_NAME);
        ModBlocks.BQ_INTERFACE.setCompositeIcon(sprite);
    }
}
