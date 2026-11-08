package com.yuelengm.bqext.mixins.late;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuelengm.bqext.config.BqExtConfig;
import com.yuelengm.bqext.mixins.early.GuiContainerAccessor;

import betterquesting.api2.client.gui.GuiContainerCanvas;

@Mixin(value = GuiContainerCanvas.class, remap = false)
public abstract class GuiContainerCanvasFullscreenMixin {

    @Shadow
    private boolean useMargins;

    @Unique
    private boolean bqext$fullscreenApplied;

    @Inject(method = "initGui", at = @At("HEAD"), remap = false)
    private void bqext$disableMargins(CallbackInfo callback) {
        if (BqExtConfig.fullscreenQuestGui) {
            this.useMargins = false;
            this.bqext$fullscreenApplied = true;
        } else if (this.bqext$fullscreenApplied) {
            this.useMargins = true;
            this.bqext$fullscreenApplied = false;
        }
    }

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void bqext$fillScreen(CallbackInfo callback) {
        if (!BqExtConfig.fullscreenQuestGui) return;

        GuiContainerAccessor accessor = (GuiContainerAccessor) (Object) this;
        GuiScreen screen = (GuiScreen) (Object) this;

        accessor.setGuiLeft(0);
        accessor.setGuiTop(0);
        accessor.setXSize(screen.width);
        accessor.setYSize(screen.height);
    }
}
