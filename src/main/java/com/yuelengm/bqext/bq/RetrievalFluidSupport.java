package com.yuelengm.bqext.bq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.yuelengm.bqext.config.BqExtConfig;
import com.yuelengm.bqext.mixins.late.TaskProgressableBaseAccessor;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskRetrieval;

public final class RetrievalFluidSupport {

    private RetrievalFluidSupport() {}

    public static boolean isEnabled(TaskRetrieval task) {
        return BqExtConfig.retrievalFluidCompat && !task.consume;
    }

    public static FluidRequirement[] resolveRequirements(TaskRetrieval task) {
        FluidRequirement[] requirements = new FluidRequirement[task.requiredItems.size()];
        for (int i = 0; i < requirements.length; i++) {
            BigItemStack required = task.requiredItems.get(i);
            if (required == null || required.hasOreDict()) continue;

            FluidStack unitFluid = getContainedFluid(required.getBaseStack());
            if (unitFluid == null || unitFluid.amount <= 0) continue;
            if (task.ignoreNBT) unitFluid.tag = null;

            requirements[i] = new FluidRequirement(i, unitFluid, required.stackSize);
        }
        return requirements;
    }

    public static void detectInventory(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest) {
        if (!isEnabled(task) || task.isComplete(pInfo.UUID)) return;

        FluidRequirement[] requirements = resolveRequirements(task);
        if (!hasRequirements(requirements)) return;

        List<AvailableFluid> available = collectInventoryFluids(pInfo);
        if (available.isEmpty()) return;

        boolean updated = false;
        List<int[]> progressList = new ArrayList<>(pInfo.ALL_UUIDS.size());
        for (UUID uuid : pInfo.ALL_UUIDS) {
            int[] current = task.getUsersProgress(uuid)
                .clone();
            int[] updatedProgress = applyAvailableFluids(task, requirements, current, available);
            progressList.add(updatedProgress);
            updated |= !Arrays.equals(current, updatedProgress);
        }

        if (!updated) return;

        TaskProgressableBaseAccessor accessor = (TaskProgressableBaseAccessor) task;
        for (int i = 0; i < pInfo.ALL_UUIDS.size(); i++) {
            accessor.bqext$setUserProgress(pInfo.ALL_UUIDS.get(i), progressList.get(i));
        }

        if (isCompleteForAll(task, progressList)) {
            for (UUID uuid : pInfo.ALL_UUIDS) {
                task.setComplete(uuid);
            }
        }
        pInfo.markDirtyParty(quest.getKey());
    }

    public static boolean matchesFluid(TaskRetrieval task, FluidStack requirement, FluidStack candidate) {
        if (task.ignoreNBT) return requirement.getFluid() == candidate.getFluid();
        return requirement.isFluidEqual(candidate);
    }

    private static int[] applyAvailableFluids(TaskRetrieval task, FluidRequirement[] requirements, int[] sourceProgress,
        List<AvailableFluid> available) {
        int[] progress = sourceProgress.clone();
        if (task.requireOnlyOneItem) {
            for (FluidRequirement requirement : requirements) {
                if (requirement == null) continue;

                int index = requirement.index;
                if (progress[index] >= requirement.requiredContainers) continue;

                long needed = requirement.requiredAmount(progress[index]);
                if (maxMatching(available, task, requirement.unitFluid, needed) >= needed) {
                    progress[index] = requirement.requiredContainers;
                    break;
                }
            }
            return progress;
        }

        for (FluidRequirement requirement : requirements) {
            if (requirement == null) continue;

            int index = requirement.index;
            int remainingContainers = Math.max(0, requirement.requiredContainers - progress[index]);
            if (remainingContainers == 0) continue;

            long needed = requirement.requiredAmount(progress[index]);
            long availableAmount = maxMatching(available, task, requirement.unitFluid, needed);
            int gained = (int) Math.min(remainingContainers, availableAmount / requirement.unitFluid.amount);
            if (gained > 0) {
                progress[index] = Math.min(requirement.requiredContainers, progress[index] + gained);
            }
        }
        return progress;
    }

    private static boolean isCompleteForAll(TaskRetrieval task, List<int[]> progressList) {
        for (int[] progress : progressList) {
            boolean complete = !task.requireOnlyOneItem;
            for (int i = 0; i < task.requiredItems.size(); i++) {
                int required = task.requiredItems.get(i).stackSize;
                if (task.requireOnlyOneItem) {
                    complete |= progress[i] >= required;
                    if (complete) break;
                } else {
                    complete &= progress[i] >= required;
                    if (!complete) break;
                }
            }
            if (!complete) return false;
        }
        return true;
    }

    private static List<AvailableFluid> collectInventoryFluids(ParticipantInfo pInfo) {
        List<AvailableFluid> available = new ArrayList<>();
        for (EntityPlayer player : pInfo.ACTIVE_PLAYERS) {
            InventoryPlayer inventory = player.inventory;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                FluidStack fluid = getContainedFluid(stack);
                if (fluid == null || fluid.amount <= 0) continue;

                for (int count = 0; count < stack.stackSize; count++) {
                    available.add(new AvailableFluid(fluid.copy()));
                }
            }
        }
        return available;
    }

    private static FluidStack getContainedFluid(ItemStack source) {
        if (source == null || source.stackSize <= 0 || source.getItem() == null) return null;

        ItemStack single = source.copy();
        single.stackSize = 1;

        if (single.getItem() instanceof IFluidContainerItem) {
            FluidStack fluid = ((IFluidContainerItem) single.getItem()).getFluid(single);
            return fluid == null ? null : fluid.copy();
        }

        FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(single);
        return fluid == null ? null : fluid.copy();
    }

    private static long maxMatching(List<AvailableFluid> available, TaskRetrieval task, FluidStack requirement,
        long maximum) {
        long maximumSingleContainer = 0;
        for (AvailableFluid candidate : available) {
            if (candidate.remaining <= 0 || !matchesFluid(task, requirement, candidate.stack)) continue;
            maximumSingleContainer = Math.max(maximumSingleContainer, Math.min(maximum, candidate.remaining));
            if (maximumSingleContainer >= maximum) break;
        }
        return maximumSingleContainer;
    }

    private static boolean hasRequirements(FluidRequirement[] requirements) {
        for (FluidRequirement requirement : requirements) {
            if (requirement != null) return true;
        }
        return false;
    }

    public static final class FluidRequirement {

        private final int index;
        private final FluidStack unitFluid;
        private final int requiredContainers;

        private FluidRequirement(int index, FluidStack unitFluid, int requiredContainers) {
            this.index = index;
            this.unitFluid = unitFluid;
            this.requiredContainers = requiredContainers;
        }

        public int getIndex() {
            return this.index;
        }

        public FluidStack getUnitFluid() {
            return this.unitFluid.copy();
        }

        public int getRequiredContainers() {
            return this.requiredContainers;
        }

        public long requiredAmount(int progress) {
            int remainingContainers = Math.max(0, this.requiredContainers - progress);
            return (long) this.unitFluid.amount * remainingContainers;
        }
    }

    private static final class AvailableFluid {

        private final FluidStack stack;
        private long remaining;

        private AvailableFluid(FluidStack stack) {
            this(stack, stack.amount);
        }

        private AvailableFluid(FluidStack stack, long remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }
}
