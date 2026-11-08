package com.yuelengm.bqext.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.ModAPIManager;

public final class BqMixins {

    private static final String BETTER_QUESTING = "betterquesting";

    private static final String[] MIXINS = { "SimpleDatabaseMixin", "TaskFluidAeMixin", "TaskRetrievalAeMixin",
        "TaskProgressableBaseAccessor", };

    private BqMixins() {}

    public static List<String> collect(Set<String> loadedMods) {
        Set<String> presentMods = loadedMods == null ? Collections.emptySet() : loadedMods;
        if (!presentMods.contains(BETTER_QUESTING) && !ModAPIManager.INSTANCE.hasAPI(BETTER_QUESTING)) {
            return Collections.emptyList();
        }

        List<String> mixins = new ArrayList<>(MIXINS.length);
        Collections.addAll(mixins, MIXINS);
        return mixins;
    }
}
