package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraftforge.fluids.FluidStack;

import com.yuelengm.bqext.ae.AeTaskDetector.NetworkView;
import com.yuelengm.bqext.bq.RetrievalFluidSupport;
import com.yuelengm.bqext.bq.RetrievalFluidSupport.FluidRequirement;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskRetrieval;

public final class AeFluidContainerDetector {

    private AeFluidContainerDetector() {}

    static void detect(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        List<NetworkView> networks, int[] initialProgress) {
        if (task.isComplete(pInfo.UUID) || task.consume) return;

        FluidRequirement[] requirements = RetrievalFluidSupport.resolveRequirements(task);
        if (!RetrievalFluidSupport.hasRequirements(requirements)) return;

        int[] currentProgress = task.getUsersProgress(pInfo.UUID)
            .clone();
        int[] bestProgress = currentProgress;
        for (NetworkView network : networks) {
            if (network.storage == null) continue;
            IMEMonitor<IAEItemStack> itemInventory = network.storage.getItemInventory();
            IMEMonitor<IAEFluidStack> fluidInventory = network.storage.getFluidInventory();
            if (itemInventory == null || fluidInventory == null) continue;

            int[] candidate = planPartialItemsWithFluids(
                task,
                requirements,
                AeStorageSnapshotCache.getItemSnapshot(network),
                AeStorageSnapshotCache.getFluidSnapshot(network),
                initialProgress);
            if (AeTaskDetector.isProgressComplete(task, candidate)) {
                AeTaskDetector.applyProgress(task, pInfo, quest, candidate, true);
                return;
            }
            bestProgress = AeTaskDetector.mergeMaxProgress(task, bestProgress, candidate);
        }

        AeTaskDetector.applyPartialProgress(task, pInfo, quest, currentProgress, bestProgress);
    }

    private static int[] planPartialItemsWithFluids(TaskRetrieval task, FluidRequirement[] fluidRequirements,
        List<IAEItemStack> itemSnapshot, List<IAEFluidStack> fluidSnapshot, int[] initialProgress) {
        List<AeRetrievalDetector.AvailableItem> availableItems = AeRetrievalDetector.createAvailableItems(itemSnapshot);
        int[] progress = initialProgress.clone();

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
                if (remaining <= 0) continue;

                List<AeRetrievalDetector.AvailableItem> workingItems = AeRetrievalDetector
                    .copyFreshAvailableItems(availableItems);
                List<AeRetrievalDetector.ItemExtraction> extraction = new ArrayList<>();
                remaining = AeRetrievalDetector
                    .allocateItemRemaining(task, requirement, remaining, workingItems, extraction);
                AeRetrievalDetector.addProgress(task, progress, i, AeRetrievalDetector.extractionAmount(extraction));
                if (remaining <= 0) continue;

                FluidRequirement fluidRequirement = fluidRequirements[i];
                if (fluidRequirement == null) continue;

                FluidStack unitFluid = fluidRequirement.getUnitFluid();
                long needed = unitFluid.amount * remaining;
                long available = matchingFluidAmount(unitFluid, needed, fluidSnapshot);
                AeRetrievalDetector.addProgress(task, progress, i, (int) (available / unitFluid.amount));
            }
            return progress;
        }

        for (int i = 0; i < task.requiredItems.size(); i++) {
            BigItemStack requirement = task.requiredItems.get(i);
            long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
            if (remaining <= 0) continue;

            List<AeRetrievalDetector.ItemExtraction> extraction = new ArrayList<>();
            remaining = AeRetrievalDetector
                .allocateItemRemaining(task, requirement, remaining, availableItems, extraction);
            AeRetrievalDetector.addProgress(task, progress, i, AeRetrievalDetector.extractionAmount(extraction));
            if (remaining <= 0) continue;

            FluidRequirement fluidRequirement = fluidRequirements[i];
            if (fluidRequirement == null) continue;

            FluidStack unitFluid = fluidRequirement.getUnitFluid();
            long needed = unitFluid.amount * remaining;
            long available = matchingFluidAmount(unitFluid, needed, fluidSnapshot);
            AeRetrievalDetector.addProgress(task, progress, i, (int) (available / unitFluid.amount));
        }
        return progress;
    }

    private static long matchingFluidAmount(FluidStack requirement, long maximum, Iterable<IAEFluidStack> available) {
        for (IAEFluidStack candidate : available) {
            if (candidate.getStackSize() <= 0 || requirement.getFluid() != candidate.getFluid()) {
                continue;
            }
            return Math.min(maximum, candidate.getStackSize());
        }
        return 0;
    }
}
