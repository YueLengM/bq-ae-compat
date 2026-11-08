package com.yuelengm.bqext.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.yuelengm.bqext.ModIds;

import cpw.mods.fml.common.ModAPIManager;

public final class BqMixins {

    private static final String[] MIXINS = { "SimpleDatabaseMixin", "TaskFluidAeMixin", "TaskRetrievalAeMixin",
        "TaskProgressableBaseAccessor", };

    private BqMixins() {}

    public static List<String> collect(Set<String> loadedMods) {
        Set<String> presentMods = loadedMods == null ? Collections.emptySet() : loadedMods;
        if (!presentMods.contains(ModIds.BETTER_QUESTING) && !ModAPIManager.INSTANCE.hasAPI(ModIds.BETTER_QUESTING)) {
            return Collections.emptyList();
        }
        if (!presentMods.contains(ModIds.APPLIED_ENERGISTICS)) {
            return Collections.emptyList();
        }

        List<String> mixins = new ArrayList<>(MIXINS.length);
        Collections.addAll(mixins, MIXINS);
        return mixins;
    }
}
