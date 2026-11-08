package com.yuelengm.bqae.mixin.late;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuelengm.bqae.ae.AeCheckScheduler;
import com.yuelengm.bqae.ae.AeCheckScheduler.CheckKind;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskFluid;

@Mixin(TaskFluid.class)
public abstract class TaskFluidAeMixin {

    @Inject(method = "detect", at = @At("TAIL"), remap = false)
    private void bqae$detectAeAfterInventory(ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        CallbackInfo callback) {
        TaskFluid task = (TaskFluid) (Object) this;
        AeCheckScheduler.request(task, pInfo, quest, CheckKind.FLUID);
    }
}
