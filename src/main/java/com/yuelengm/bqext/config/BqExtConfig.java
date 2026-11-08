package com.yuelengm.bqext.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.yuelengm.bqext.BQExt;

@Config(modid = BQExt.MODID, category = "general")
@Config.LangKey("bqext.config.category.general")
public final class BqExtConfig {

    @Config.Comment("Make BetterQuesting GUIs fill the entire screen.")
    @Config.LangKey("bqext.config.fullscreen_quest_gui")
    @Config.DefaultBoolean(false)
    public static boolean fullscreenQuestGui = false;

    @Config.Comment("Allow Retrieval container-item tasks to match equivalent fluid in inventories and AE networks.")
    @Config.LangKey("bqext.config.retrieval_fluid_compat")
    @Config.RequiresMcRestart
    @Config.DefaultBoolean(true)
    public static boolean retrievalFluidCompat = true;

    private BqExtConfig() {}
}
