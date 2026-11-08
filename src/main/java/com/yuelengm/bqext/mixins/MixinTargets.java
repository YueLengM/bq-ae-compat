package com.yuelengm.bqext.mixins;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;
import com.yuelengm.bqext.ModIds;

public enum MixinTargets implements ITargetMod {

    BETTER_QUESTING(new TargetModBuilder().setModId(ModIds.BETTER_QUESTING)),
    APPLIED_ENERGISTICS(new TargetModBuilder().setModId(ModIds.APPLIED_ENERGISTICS));

    private final TargetModBuilder builder;

    MixinTargets(TargetModBuilder builder) {
        this.builder = builder;
    }

    @Override
    public TargetModBuilder getBuilder() {
        return this.builder;
    }
}
