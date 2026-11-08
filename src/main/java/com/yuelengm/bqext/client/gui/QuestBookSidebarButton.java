package com.yuelengm.bqext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.yuelengm.bqext.BQExt;

import betterquesting.api.storage.BQ_Settings;
import betterquesting.client.gui2.GuiHome;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.events.client.CustomClickEvent;

@SideOnly(Side.CLIENT)
public final class QuestBookSidebarButton {

    private static final ResourceLocation BUTTON_ID = new ResourceLocation(BQExt.MODID, "quest_book");

    @SubscribeEvent
    public void onCustomClick(CustomClickEvent event) {
        if (!BUTTON_ID.equals(event.getID())) return;
        event.setCanceled(true);

        Minecraft mc = Minecraft.getMinecraft();
        if (BQ_Settings.useBookmark && GuiHome.bookmark != null) {
            mc.displayGuiScreen(GuiHome.bookmark);
        } else {
            mc.displayGuiScreen(new GuiHome(mc.currentScreen));
        }
    }
}
