package com.yuelengm.bqext.mixins;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.yuelengm.bqext.config.BqExtConfig;

public enum Mixins implements IMixins {

    GUI_CONTAINER_ACCESS(new MixinBuilder("GUI container bounds access").addClientMixins("GuiContainerAccessor")
        .setPhase(Phase.EARLY)),

    BQ_FULLSCREEN(new MixinBuilder("BetterQuesting fullscreen GUIs")
        .addClientMixins("GuiScreenCanvasFullscreenMixin", "GuiContainerCanvasFullscreenMixin")
        .setPhase(Phase.LATE)
        .addRequiredMod(MixinTargets.BETTER_QUESTING)),

    BQ_CORE(new MixinBuilder("BetterQuesting core integration").addCommonMixins("TaskProgressableBaseAccessor")
        .setPhase(Phase.LATE)
        .addRequiredMod(MixinTargets.BETTER_QUESTING)),

    BQ_RETRIEVAL_FLUID(new MixinBuilder("BetterQuesting Retrieval fluid-container compatibility")
        .addCommonMixins("TaskRetrievalFluidCompatMixin")
        .setPhase(Phase.LATE)
        .addRequiredMod(MixinTargets.BETTER_QUESTING)
        .setApplyIf(() -> BqExtConfig.retrievalFluidCompat)),

    BQ_AE_TASKS(new MixinBuilder("BetterQuesting AE task detection")
        .addCommonMixins("SimpleDatabaseMixin", "TaskFluidAeMixin", "TaskRetrievalAeMixin")
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
