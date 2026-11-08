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

    private BqExtConfig() {}
}
