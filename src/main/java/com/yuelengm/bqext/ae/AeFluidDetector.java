package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.FluidStack;

import com.yuelengm.bqext.BQExt;
import com.yuelengm.bqext.ae.AeTaskDetector.NetworkView;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskFluid;

public final class AeFluidDetector {

    private AeFluidDetector() {}

    static void detect(TaskFluid task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        List<NetworkView> networks) {
        if (task.isComplete(pInfo.UUID)) return;

        for (NetworkView network : networks) {
            if (network.storage == null) continue;
            IMEMonitor<IAEFluidStack> inventory = network.storage.getFluidInventory();
            if (inventory == null) continue;

            FluidPlan plan = planFluids(task, inventory, task.getUsersProgress(pInfo.UUID));
            if (plan == null) continue;
            if (task.consume && !consumeFluids(network, inventory, plan.extraction, pInfo.PLAYER)) continue;

            AeTaskDetector.completeTask(task, pInfo, quest, plan.progress);
            return;
        }
    }

    private static FluidPlan planFluids(TaskFluid task, IMEMonitor<IAEFluidStack> inventory, int[] progress) {
        if (task.requiredFluids.isEmpty()) return new FluidPlan(new ArrayList<>(), progress.clone());

        List<AvailableFluid> available = copyAvailableFluids(inventory);
        List<FluidExtraction> extraction = new ArrayList<>();
        for (int i = 0; i < task.requiredFluids.size(); i++) {
            FluidStack requirement = task.requiredFluids.get(i);
            long remaining = Math.max(0, requirement.amount - AeTaskDetector.getProgress(progress, i));
            if (!allocateFluid(requirement, remaining, available, extraction)) return null;
        }

        int[] completedProgress = progress.clone();
        for (int i = 0; i < task.requiredFluids.size(); i++) {
            completedProgress[i] = task.requiredFluids.get(i).amount;
        }
        return new FluidPlan(extraction, completedProgress);
    }

    private static boolean allocateFluid(FluidStack requirement, long remaining, List<AvailableFluid> available,
        List<FluidExtraction> extraction) {
        for (AvailableFluid candidate : available) {
            if (remaining <= 0) break;
            if (candidate.remaining <= 0 || requirement.getFluid() != candidate.stack.getFluid()) {
                continue;
            }

            long amount = Math.min(remaining, candidate.remaining);
            extraction.add(new FluidExtraction(candidate.stack.copy(), amount));
            candidate.remaining -= amount;
            remaining -= amount;
        }
        return remaining <= 0;
    }

    private static boolean consumeFluids(NetworkView network, IMEMonitor<IAEFluidStack> inventory,
        List<FluidExtraction> extraction, EntityPlayer player) {
        BaseActionSource source = new PlayerSource(player, network.bqInterface);
        for (FluidExtraction plan : extraction) {
            IAEFluidStack request = plan.stack.copy();
            request.setStackSize(plan.amount);
            IAEFluidStack result = inventory.extractItems(request, Actionable.SIMULATE, source);
            if (result == null || result.getStackSize() < plan.amount) return false;
        }

        for (FluidExtraction plan : extraction) {
            IAEFluidStack request = plan.stack.copy();
            request.setStackSize(plan.amount);
            IAEFluidStack result = inventory.extractItems(request, Actionable.MODULATE, source);
            if (result == null || result.getStackSize() < plan.amount) {
                BQExt.LOG.error("AE fluid extraction rejected after simulation for {}", request);
                return false;
            }
        }
        return true;
    }

    private static List<AvailableFluid> copyAvailableFluids(IMEMonitor<IAEFluidStack> inventory) {
        List<AvailableFluid> available = new ArrayList<>();
        for (IAEFluidStack stack : inventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) available.add(new AvailableFluid(stack.copy()));
        }
        return available;
    }

    private static final class AvailableFluid {

        private final IAEFluidStack stack;
        private long remaining;

        private AvailableFluid(IAEFluidStack stack) {
            this.stack = stack;
            this.remaining = stack.getStackSize();
        }
    }

    private static final class FluidExtraction {

        private final IAEFluidStack stack;
        private final long amount;

        private FluidExtraction(IAEFluidStack stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }

    private static final class FluidPlan {

        private final List<FluidExtraction> extraction;
        private final int[] progress;

        private FluidPlan(List<FluidExtraction> extraction, int[] progress) {
            this.extraction = extraction;
            this.progress = progress;
        }
    }
}
