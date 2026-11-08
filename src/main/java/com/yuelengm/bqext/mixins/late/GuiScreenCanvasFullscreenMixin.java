package com.yuelengm.bqext.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuelengm.bqext.config.BqExtConfig;

import betterquesting.api2.client.gui.GuiScreenCanvas;

@Mixin(value = GuiScreenCanvas.class, remap = false)
public abstract class GuiScreenCanvasFullscreenMixin {

    @Shadow
    private boolean useMargins;

    @Unique
    private boolean bqext$fullscreenApplied;

    @Inject(method = "initPanel", at = @At("HEAD"), remap = false)
    private void bqext$useFullscreenBounds(CallbackInfo callback) {
        if (BqExtConfig.fullscreenQuestGui) {
            this.useMargins = false;
            this.bqext$fullscreenApplied = true;
        } else if (this.bqext$fullscreenApplied) {
            this.useMargins = true;
            this.bqext$fullscreenApplied = false;
        }
    }
}
