package com.yuelengm.bqext.mixins;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    GUI_CONTAINER_ACCESS(new MixinBuilder("GUI container bounds access").addClientMixins("GuiContainerAccessor")
        .setPhase(Phase.EARLY)),

    BQ_FULLSCREEN(new MixinBuilder("BetterQuesting fullscreen GUIs")
        .addClientMixins("GuiScreenCanvasFullscreenMixin", "GuiContainerCanvasFullscreenMixin")
        .setPhase(Phase.LATE)
        .addRequiredMod(MixinTargets.BETTER_QUESTING)),

    BQ_AE_TASKS(new MixinBuilder("BetterQuesting AE task detection")
        .addCommonMixins(
            "SimpleDatabaseMixin",
            "TaskFluidAeMixin",
            "TaskRetrievalAeMixin",
            "TaskProgressableBaseAccessor")
        .setPhase(Phase.LATE)
        .addRequiredMod(MixinTargets.BETTER_QUESTING)
        .addRequiredMod(MixinTargets.APPLIED_ENERGISTICS));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    @NotNull
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
