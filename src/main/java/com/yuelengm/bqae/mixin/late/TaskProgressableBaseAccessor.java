package com.yuelengm.bqae.mixin.late;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import bq_standard.tasks.base.TaskProgressableBase;

@Mixin(TaskProgressableBase.class)
public interface TaskProgressableBaseAccessor {

    @Invoker(value = "setUserProgress", remap = false)
    void bqae$setUserProgress(UUID uuid, Object progress);
}
