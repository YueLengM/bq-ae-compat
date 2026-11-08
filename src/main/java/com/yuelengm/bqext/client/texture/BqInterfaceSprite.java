package com.yuelengm.bqext.client.texture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.yuelengm.bqext.BQExt;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BqInterfaceSprite extends TextureAtlasSprite {

    private static final ResourceLocation AE_INTERFACE = new ResourceLocation(
        "appliedenergistics2",
        "textures/blocks/BlockInterface.png");
    private static final ResourceLocation QUEST_BOOK = new ResourceLocation(
        "questbook",
        "textures/items/ItemQuestBook.png");

    public BqInterfaceSprite(String name) {
        super(name);
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        boolean loaded = false;
        try {
            BufferedImage base = read(manager, AE_INTERFACE);
            if (base == null) return true;

            int width = base.getWidth();
            int height = base.getHeight();
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = combined.createGraphics();
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(base, 0, 0, null);

            BufferedImage book = read(manager, QUEST_BOOK);
            if (book != null) {
                graphics.drawImage(book, 0, 0, width, height, null);
            }
            graphics.dispose();

            this.width = width;
            this.height = height;
            int[] pixels = new int[width * height];
            combined.getRGB(0, 0, width, height, pixels, 0, width);
            int mipLevels = Math.max(0, Minecraft.getMinecraft().gameSettings.mipmapLevels);
            int[][] frameData = new int[mipLevels + 1][];
            frameData[0] = pixels;
            this.setFramesTextureData(Collections.singletonList(frameData));
            loaded = true;
        } catch (IOException e) {
            BQExt.LOG.error("Unable to build BQ interface texture", e);
        }

        return !loaded;
    }

    private static BufferedImage read(IResourceManager manager, ResourceLocation location) throws IOException {
        try (InputStream stream = manager.getResource(location)
            .getInputStream()) {
            return ImageIO.read(stream);
        }
    }
}
