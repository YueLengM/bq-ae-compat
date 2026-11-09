package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.yuelengm.bqext.BQExt;
import com.yuelengm.bqext.ae.AeTaskDetector.NetworkView;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskRetrieval;

public final class AeRetrievalDetector {

    private AeRetrievalDetector() {}

    static void detect(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        List<NetworkView> networks, int[] initialProgress) {
        if (task.consume) {
            detectConsumable(task, pInfo, quest, networks, initialProgress);
            return;
        }

        int[] bestProgress = initialProgress;
        for (NetworkView network : networks) {
            if (network.storage == null) continue;

            int[] candidate = planPartialItems(task, AeStorageSnapshotCache.getItemSnapshot(network), initialProgress);
            if (AeTaskDetector.isProgressComplete(task, candidate)) {
                AeTaskDetector.applyProgress(task, pInfo, quest, candidate, true);
                return;
            }
            bestProgress = AeTaskDetector.mergeMaxProgress(task, bestProgress, candidate);
        }

        AeTaskDetector.applyPartialProgress(task, pInfo, quest, initialProgress, bestProgress);
    }

    private static void detectConsumable(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        List<NetworkView> networks, int[] progress) {
        for (NetworkView network : networks) {
            if (network.storage == null) continue;
            IMEMonitor<IAEItemStack> inventory = network.storage.getItemInventory();
            if (inventory == null) continue;

            ItemPlan plan = planItems(task, AeStorageSnapshotCache.getItemSnapshot(network), progress);
            if (plan == null) continue;
            boolean consumed = consumeItems(network, inventory, plan.extraction, pInfo.PLAYER);
            AeStorageSnapshotCache.invalidateItems(network);
            if (!consumed) continue;

            AeTaskDetector.completeTask(task, pInfo, quest, plan.progress);
            return;
        }
    }

    private static ItemPlan planItems(TaskRetrieval task, List<IAEItemStack> snapshot, int[] progress) {
        List<AvailableItem> available = createAvailableItems(snapshot);
        if (task.requiredItems.isEmpty()) {
            return task.requireOnlyOneItem ? null : new ItemPlan(new ArrayList<>(), progress.clone());
        }

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
                int[] completedProgress = progress.clone();
                completedProgress[i] = requirement.stackSize;
                if (remaining <= 0) return new ItemPlan(new ArrayList<>(), completedProgress);

                List<AvailableItem> working = copyFreshAvailableItems(available);
                List<ItemExtraction> extraction = new ArrayList<>();
                if (allocateItem(task, requirement, remaining, working, extraction)) {
                    return new ItemPlan(extraction, completedProgress);
                }
            }
            return null;
        }

        List<ItemExtraction> extraction = new ArrayList<>();
        for (int i = 0; i < task.requiredItems.size(); i++) {
            BigItemStack requirement = task.requiredItems.get(i);
            long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
            if (!allocateItem(task, requirement, remaining, available, extraction)) return null;
        }

        int[] completedProgress = progress.clone();
        for (int i = 0; i < task.requiredItems.size(); i++) {
            completedProgress[i] = task.requiredItems.get(i).stackSize;
        }
        return new ItemPlan(extraction, completedProgress);
    }

    private static int[] planPartialItems(TaskRetrieval task, List<IAEItemStack> snapshot, int[] initialProgress) {
        List<AvailableItem> availableItems = createAvailableItems(snapshot);
        int[] progress = initialProgress.clone();

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
                if (remaining <= 0) continue;

                List<AvailableItem> workingItems = copyFreshAvailableItems(availableItems);
                List<ItemExtraction> extraction = new ArrayList<>();
                allocateItemRemaining(task, requirement, remaining, workingItems, extraction);
                addProgress(task, progress, i, extractionAmount(extraction));
            }
            return progress;
        }

        for (int i = 0; i < task.requiredItems.size(); i++) {
            BigItemStack requirement = task.requiredItems.get(i);
            long remaining = Math.max(0, requirement.stackSize - AeTaskDetector.getProgress(progress, i));
            if (remaining <= 0) continue;

            List<ItemExtraction> extraction = new ArrayList<>();
            allocateItemRemaining(task, requirement, remaining, availableItems, extraction);
            addProgress(task, progress, i, extractionAmount(extraction));
        }
        return progress;
    }

    private static boolean allocateItem(TaskRetrieval task, BigItemStack requirement, long remaining,
        List<AvailableItem> available, List<ItemExtraction> extraction) {
        return allocateItemRemaining(task, requirement, remaining, available, extraction) <= 0;
    }

    static long allocateItemRemaining(TaskRetrieval task, BigItemStack requirement, long remaining,
        List<AvailableItem> available, List<ItemExtraction> extraction) {
        for (AvailableItem candidate : available) {
            if (remaining <= 0) break;
            if (candidate.remaining <= 0 || !matchesItem(task, requirement, candidate.stack)) continue;

            long amount = Math.min(remaining, candidate.remaining);
            extraction.add(new ItemExtraction(candidate.stack.copy(), amount));
            candidate.remaining -= amount;
            remaining -= amount;
        }
        return remaining;
    }

    static int extractionAmount(List<ItemExtraction> extraction) {
        long amount = 0;
        for (ItemExtraction item : extraction) {
            amount += item.amount;
        }
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    static void addProgress(TaskRetrieval task, int[] progress, int index, int amount) {
        int required = task.requiredItems.get(index).stackSize;
        progress[index] = Math.min(required, progress[index] + amount);
    }

    static boolean matchesItem(TaskRetrieval task, BigItemStack requirement, IAEItemStack candidate) {
        ItemStack sample = candidate.getItemStack();
        return ItemComparison.StackMatch(requirement.getBaseStack(), sample, !task.ignoreNBT, task.partialMatch)
            || ItemComparison.OreDictionaryMatch(
                requirement.getOreIngredient(),
                requirement.GetTagCompound(),
                sample,
                !task.ignoreNBT,
                task.partialMatch);
    }

    static List<AvailableItem> createAvailableItems(List<IAEItemStack> snapshot) {
        List<AvailableItem> available = new ArrayList<>(snapshot.size());
        for (IAEItemStack stack : snapshot) {
            if (stack != null && stack.getStackSize() > 0) available.add(new AvailableItem(stack));
        }
        return available;
    }

    static List<AvailableItem> copyFreshAvailableItems(List<AvailableItem> source) {
        List<AvailableItem> available = new ArrayList<>(source.size());
        for (AvailableItem item : source) {
            available.add(new AvailableItem(item.stack));
        }
        return available;
    }

    private static boolean consumeItems(NetworkView network, IMEMonitor<IAEItemStack> inventory,
        List<ItemExtraction> extraction, EntityPlayer player) {
        BaseActionSource source = new PlayerSource(player, network.bqInterface);
        for (ItemExtraction plan : extraction) {
            IAEItemStack request = plan.stack.copy();
            request.setStackSize(plan.amount);
            IAEItemStack result = inventory.extractItems(request, Actionable.SIMULATE, source);
            if (result == null || result.getStackSize() < plan.amount) return false;
        }

        for (ItemExtraction plan : extraction) {
            IAEItemStack request = plan.stack.copy();
            request.setStackSize(plan.amount);
            IAEItemStack result = inventory.extractItems(request, Actionable.MODULATE, source);
            if (result == null || result.getStackSize() < plan.amount) {
                BQExt.LOG.error("AE item extraction rejected after simulation for {}", request);
                return false;
            }
        }
        return true;
    }

    static final class AvailableItem {

        final IAEItemStack stack;
        long remaining;

        private AvailableItem(IAEItemStack stack) {
            this.stack = stack;
            this.remaining = stack.getStackSize();
        }
    }

    static final class ItemExtraction {

        final IAEItemStack stack;
        final long amount;

        private ItemExtraction(IAEItemStack stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }

    private static final class ItemPlan {

        private final List<ItemExtraction> extraction;
        private final int[] progress;

        private ItemPlan(List<ItemExtraction> extraction, int[] progress) {
            this.extraction = extraction;
            this.progress = progress;
        }
    }
}
