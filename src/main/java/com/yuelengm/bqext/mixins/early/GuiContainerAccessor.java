package com.yuelengm.bqext.mixins.early;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {

    @Accessor("guiLeft")
    void setGuiLeft(int value);

    @Accessor("guiTop")
    void setGuiTop(int value);

    @Accessor("xSize")
    void setXSize(int value);

    @Accessor("ySize")
    void setYSize(int value);
}
