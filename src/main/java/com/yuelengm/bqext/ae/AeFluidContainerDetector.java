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
        if (!hasFluidRequirements(requirements)) return;

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
                itemInventory,
                fluidInventory,
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
        IMEMonitor<IAEItemStack> itemInventory, IMEMonitor<IAEFluidStack> fluidInventory, int[] initialProgress) {
        List<AeRetrievalDetector.AvailableItem> availableItems = AeRetrievalDetector.copyAvailableItems(itemInventory);
        List<AvailableRetrievalFluid> availableFluids = copyRetrievalFluids(fluidInventory);
        int[] progress = initialProgress.clone();

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
                if (remaining <= 0) continue;

                List<AeRetrievalDetector.AvailableItem> workingItems = AeRetrievalDetector
                    .copyAvailableItems(availableItems);
                List<AeRetrievalDetector.ItemExtraction> extraction = new ArrayList<>();
                remaining = AeRetrievalDetector
                    .allocateItemRemaining(task, requirement, remaining, workingItems, extraction);
                AeRetrievalDetector.addProgress(task, progress, i, AeRetrievalDetector.extractionAmount(extraction));
                if (remaining <= 0) continue;

                FluidRequirement fluidRequirement = fluidRequirements[i];
                if (fluidRequirement == null) continue;

                FluidStack unitFluid = fluidRequirement.getUnitFluid();
                long needed = unitFluid.amount * remaining;
                long available = matchingFluidAmount(task, unitFluid, needed, availableFluids);
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
            long available = matchingFluidAmount(task, unitFluid, needed, availableFluids);
            AeRetrievalDetector.addProgress(task, progress, i, (int) (available / unitFluid.amount));
        }
        return progress;
    }

    private static boolean hasFluidRequirements(FluidRequirement[] requirements) {
        for (FluidRequirement requirement : requirements) {
            if (requirement != null) return true;
        }
        return false;
    }

    private static long matchingFluidAmount(TaskRetrieval task, FluidStack requirement, long maximum,
        List<AvailableRetrievalFluid> available) {
        for (AvailableRetrievalFluid candidate : available) {
            if (candidate.remaining <= 0 || !RetrievalFluidSupport.matchesFluid(task, requirement, candidate.stack)) {
                continue;
            }
            return Math.min(maximum, candidate.remaining);
        }
        return 0;
    }

    private static List<AvailableRetrievalFluid> copyRetrievalFluids(IMEMonitor<IAEFluidStack> fluidInventory) {
        List<AvailableRetrievalFluid> available = new ArrayList<>();
        for (IAEFluidStack stack : fluidInventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) {
                available.add(
                    new AvailableRetrievalFluid(
                        stack.getFluidStack()
                            .copy(),
                        stack.getStackSize()));
            }
        }
        return available;
    }

    private static final class AvailableRetrievalFluid {

        private final FluidStack stack;
        private long remaining;

        private AvailableRetrievalFluid(FluidStack stack, long remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }
}
