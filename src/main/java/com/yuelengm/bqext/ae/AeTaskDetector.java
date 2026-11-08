package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.yuelengm.bqext.BQExt;
import com.yuelengm.bqext.bq.PartyIdentity;
import com.yuelengm.bqext.bq.RetrievalFluidSupport;
import com.yuelengm.bqext.bq.RetrievalFluidSupport.FluidRequirement;
import com.yuelengm.bqext.config.BqExtConfig;
import com.yuelengm.bqext.mixins.late.TaskProgressableBaseAccessor;
import com.yuelengm.bqext.tile.TileBqInterface;

import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.party.IParty;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskFluid;
import bq_standard.tasks.TaskRetrieval;

public final class AeTaskDetector {

    private AeTaskDetector() {}

    public static boolean hasAnchors(ParticipantInfo pInfo) {
        UUID partyUid = getPartyUid(pInfo);
        return partyUid != null && BqInterfaceIndex.hasAnchors(partyUid);
    }

    public static void detectItems(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest) {
        if (task.isComplete(pInfo.UUID)) return;

        List<NetworkView> networks = resolveNetworks(pInfo);
        int[] initialProgress = task.getUsersProgress(pInfo.UUID)
            .clone();

        if (task.consume) {
            for (NetworkView network : networks) {
                if (network.storage == null) continue;
                IMEMonitor<IAEItemStack> inventory = network.storage.getItemInventory();
                if (inventory == null) continue;

                ItemPlan plan = planItems(task, inventory, initialProgress);
                if (plan == null) continue;
                if (!consumeItems(network, inventory, plan.extraction, pInfo.PLAYER)) continue;

                completeTask(task, pInfo, quest, plan.progress);
                return;
            }
            return;
        }

        int[] bestProgress = initialProgress;
        for (NetworkView network : networks) {
            if (network.storage == null) continue;
            IMEMonitor<IAEItemStack> inventory = network.storage.getItemInventory();
            if (inventory == null) continue;

            int[] candidate = planPartialItems(task, inventory, initialProgress);
            if (isProgressComplete(task, candidate)) {
                applyProgress(task, pInfo, quest, candidate, true);
                return;
            }
            bestProgress = mergeMaxProgress(task, bestProgress, candidate);
        }

        if (!BqExtConfig.retrievalFluidCompat) {
            applyPartialProgress(task, pInfo, quest, initialProgress, bestProgress);
            return;
        }

        FluidRequirement[] requirements = RetrievalFluidSupport.resolveRequirements(task);
        if (!hasFluidRequirements(requirements)) {
            applyPartialProgress(task, pInfo, quest, initialProgress, bestProgress);
            return;
        }

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
            if (isProgressComplete(task, candidate)) {
                applyProgress(task, pInfo, quest, candidate, true);
                return;
            }
            bestProgress = mergeMaxProgress(task, bestProgress, candidate);
        }

        applyPartialProgress(task, pInfo, quest, initialProgress, bestProgress);
    }

    public static void detectFluids(TaskFluid task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest) {
        if (task.isComplete(pInfo.UUID)) return;

        for (NetworkView network : resolveNetworks(pInfo)) {
            if (network.storage == null) continue;
            IMEMonitor<IAEFluidStack> inventory = network.storage.getFluidInventory();
            if (inventory == null) continue;

            FluidPlan plan = planFluids(task, inventory, task.getUsersProgress(pInfo.UUID));
            if (plan == null) continue;

            if (task.consume && !consumeFluids(network, inventory, plan.extraction, pInfo.PLAYER)) {
                continue;
            }

            completeTask(task, pInfo, quest, plan.progress);
            return;
        }
    }

    private static List<NetworkView> resolveNetworks(ParticipantInfo pInfo) {
        UUID partyUid = getPartyUid(pInfo);
        if (partyUid == null) return new ArrayList<>();

        Map<UUID, NetworkView> networks = new HashMap<>();
        for (TileBqInterface bqInterface : BqInterfaceIndex.getLoadedInterfaces(partyUid)) {
            try {
                IGrid grid = bqInterface.getProxy()
                    .getGrid();
                ISecurityGrid security = grid.getCache(ISecurityGrid.class);
                if (security != null && !security.hasPermission(pInfo.PLAYER, SecurityPermissions.EXTRACT)) {
                    continue;
                }

                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                if (storage == null) continue;

                networks.putIfAbsent(grid.getId(), new NetworkView(bqInterface, storage));
            } catch (GridAccessException ignored) {}
        }
        return new ArrayList<>(networks.values());
    }

    private static UUID getPartyUid(ParticipantInfo pInfo) {
        DBEntry<IParty> party = pInfo.PARTY_INSTANCE;
        if (party == null) return null;
        return PartyIdentity.get(party.getValue());
    }

    private static ItemPlan planItems(TaskRetrieval task, IMEMonitor<IAEItemStack> inventory, int[] progress) {
        List<AvailableItem> available = copyAvailableItems(inventory);
        if (task.requiredItems.isEmpty()) {
            return task.requireOnlyOneItem ? null : new ItemPlan(new ArrayList<>(), progress.clone());
        }

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
                int[] completedProgress = progress.clone();
                completedProgress[i] = requirement.stackSize;
                if (remaining <= 0) return new ItemPlan(new ArrayList<>(), completedProgress);

                List<AvailableItem> working = copyAvailableItems(available);
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
            long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
            if (!allocateItem(task, requirement, remaining, available, extraction)) return null;
        }

        int[] completedProgress = progress.clone();
        for (int i = 0; i < task.requiredItems.size(); i++) {
            completedProgress[i] = task.requiredItems.get(i).stackSize;
        }
        return new ItemPlan(extraction, completedProgress);
    }

    private static int[] planPartialItems(TaskRetrieval task, IMEMonitor<IAEItemStack> itemInventory,
        int[] initialProgress) {
        List<AvailableItem> availableItems = copyAvailableItems(itemInventory);
        int[] progress = initialProgress.clone();

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
                if (remaining <= 0) continue;

                List<AvailableItem> workingItems = copyAvailableItems(availableItems);
                List<ItemExtraction> extraction = new ArrayList<>();
                allocateItemRemaining(task, requirement, remaining, workingItems, extraction);
                addProgress(task, progress, i, extractionAmount(extraction));
            }
            return progress;
        }

        for (int i = 0; i < task.requiredItems.size(); i++) {
            BigItemStack requirement = task.requiredItems.get(i);
            long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
            if (remaining <= 0) continue;

            List<ItemExtraction> extraction = new ArrayList<>();
            allocateItemRemaining(task, requirement, remaining, availableItems, extraction);
            addProgress(task, progress, i, extractionAmount(extraction));
        }
        return progress;
    }

    private static int[] planPartialItemsWithFluids(TaskRetrieval task, FluidRequirement[] fluidRequirements,
        IMEMonitor<IAEItemStack> itemInventory, IMEMonitor<IAEFluidStack> fluidInventory, int[] initialProgress) {
        List<AvailableItem> availableItems = copyAvailableItems(itemInventory);
        List<AvailableRetrievalFluid> availableFluids = copyRetrievalFluids(fluidInventory);
        int[] progress = initialProgress.clone();

        if (task.requireOnlyOneItem) {
            for (int i = 0; i < task.requiredItems.size(); i++) {
                BigItemStack requirement = task.requiredItems.get(i);
                long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
                if (remaining <= 0) continue;

                List<AvailableItem> workingItems = copyAvailableItems(availableItems);
                List<ItemExtraction> extraction = new ArrayList<>();
                remaining = allocateItemRemaining(task, requirement, remaining, workingItems, extraction);
                addProgress(task, progress, i, extractionAmount(extraction));
                if (remaining <= 0) continue;

                FluidRequirement fluidRequirement = fluidRequirements[i];
                if (fluidRequirement == null) continue;

                FluidStack unitFluid = fluidRequirement.getUnitFluid();
                long needed = unitFluid.amount * remaining;
                long available = sumRetrievalFluid(task, unitFluid, needed, availableFluids);
                addProgress(task, progress, i, (int) (available / unitFluid.amount));
            }
            return progress;
        }

        for (int i = 0; i < task.requiredItems.size(); i++) {
            BigItemStack requirement = task.requiredItems.get(i);
            long remaining = Math.max(0, requirement.stackSize - getProgress(progress, i));
            if (remaining <= 0) continue;

            List<ItemExtraction> extraction = new ArrayList<>();
            remaining = allocateItemRemaining(task, requirement, remaining, availableItems, extraction);
            addProgress(task, progress, i, extractionAmount(extraction));
            if (remaining <= 0) continue;

            FluidRequirement fluidRequirement = fluidRequirements[i];
            if (fluidRequirement == null) continue;

            FluidStack unitFluid = fluidRequirement.getUnitFluid();
            long needed = unitFluid.amount * remaining;
            long available = sumRetrievalFluid(task, unitFluid, needed, availableFluids);
            addProgress(task, progress, i, (int) (available / unitFluid.amount));
        }
        return progress;
    }

    private static boolean allocateItem(TaskRetrieval task, BigItemStack requirement, long remaining,
        List<AvailableItem> available, List<ItemExtraction> extraction) {
        return allocateItemRemaining(task, requirement, remaining, available, extraction) <= 0;
    }

    private static long allocateItemRemaining(TaskRetrieval task, BigItemStack requirement, long remaining,
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

    private static long sumRetrievalFluid(TaskRetrieval task, FluidStack requirement, long maximum,
        List<AvailableRetrievalFluid> available) {
        long matched = 0;
        for (AvailableRetrievalFluid candidate : available) {
            if (matched >= maximum) break;
            if (candidate.remaining <= 0 || !RetrievalFluidSupport.matchesFluid(task, requirement, candidate.stack)) {
                continue;
            }

            matched += Math.min(maximum - matched, candidate.remaining);
        }
        return matched;
    }

    private static int extractionAmount(List<ItemExtraction> extraction) {
        long amount = 0;
        for (ItemExtraction item : extraction) {
            amount += item.amount;
        }
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    private static void addProgress(TaskRetrieval task, int[] progress, int index, int amount) {
        int required = task.requiredItems.get(index).stackSize;
        progress[index] = Math.min(required, progress[index] + amount);
    }

    private static boolean hasFluidRequirements(FluidRequirement[] requirements) {
        for (FluidRequirement requirement : requirements) {
            if (requirement != null) return true;
        }
        return false;
    }

    private static boolean matchesItem(TaskRetrieval task, BigItemStack requirement, IAEItemStack candidate) {
        ItemStack sample = candidate.getItemStack();
        return ItemComparison.StackMatch(requirement.getBaseStack(), sample, !task.ignoreNBT, task.partialMatch)
            || ItemComparison.OreDictionaryMatch(
                requirement.getOreIngredient(),
                requirement.GetTagCompound(),
                sample,
                !task.ignoreNBT,
                task.partialMatch);
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

    private static FluidPlan planFluids(TaskFluid task, IMEMonitor<IAEFluidStack> inventory, int[] progress) {
        if (task.requiredFluids.isEmpty()) return new FluidPlan(new ArrayList<>(), progress.clone());

        List<AvailableFluid> available = copyAvailableFluids(inventory);
        List<FluidExtraction> extraction = new ArrayList<>();
        for (int i = 0; i < task.requiredFluids.size(); i++) {
            FluidStack requirement = task.requiredFluids.get(i);
            long remaining = Math.max(0, requirement.amount - getProgress(progress, i));
            if (!allocateFluid(task, requirement, remaining, available, extraction)) return null;
        }

        int[] completedProgress = progress.clone();
        for (int i = 0; i < task.requiredFluids.size(); i++) {
            completedProgress[i] = task.requiredFluids.get(i).amount;
        }
        return new FluidPlan(extraction, completedProgress);
    }

    private static boolean allocateFluid(TaskFluid task, FluidStack requirement, long remaining,
        List<AvailableFluid> available, List<FluidExtraction> extraction) {
        for (AvailableFluid candidate : available) {
            if (remaining <= 0) break;
            if (candidate.remaining <= 0 || !matchesFluid(task, requirement, candidate.stack)) continue;

            long amount = Math.min(remaining, candidate.remaining);
            extraction.add(new FluidExtraction(candidate.stack.copy(), amount));
            candidate.remaining -= amount;
            remaining -= amount;
        }
        return remaining <= 0;
    }

    private static boolean matchesFluid(TaskFluid task, FluidStack requirement, IAEFluidStack candidate) {
        FluidStack sample = candidate.getFluidStack();
        if (task.ignoreNbt) return requirement.getFluid() == sample.getFluid();
        return requirement.isFluidEqual(sample);
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

    private static List<AvailableItem> copyAvailableItems(IMEMonitor<IAEItemStack> inventory) {
        List<AvailableItem> available = new ArrayList<>();
        for (IAEItemStack stack : inventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) available.add(new AvailableItem(stack.copy()));
        }
        return available;
    }

    private static List<AvailableItem> copyAvailableItems(List<AvailableItem> source) {
        List<AvailableItem> available = new ArrayList<>(source.size());
        for (AvailableItem item : source) {
            available.add(new AvailableItem(item.stack.copy()));
        }
        return available;
    }

    private static List<AvailableFluid> copyAvailableFluids(IMEMonitor<IAEFluidStack> inventory) {
        List<AvailableFluid> available = new ArrayList<>();
        for (IAEFluidStack stack : inventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) available.add(new AvailableFluid(stack.copy()));
        }
        return available;
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

    private static void completeTask(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, int[] progress) {
        TaskProgressableBaseAccessor accessor = (TaskProgressableBaseAccessor) task;
        for (UUID uuid : pInfo.ALL_UUIDS) {
            accessor.bqext$setUserProgress(uuid, progress.clone());
            task.setComplete(uuid);
        }
        pInfo.markDirtyParty(quest.getKey());
    }

    private static void applyProgress(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        int[] progress, boolean complete) {
        TaskProgressableBaseAccessor accessor = (TaskProgressableBaseAccessor) task;
        for (UUID uuid : pInfo.ALL_UUIDS) {
            accessor.bqext$setUserProgress(uuid, progress.clone());
            if (complete) task.setComplete(uuid);
        }
        pInfo.markDirtyParty(quest.getKey());
    }

    private static void applyPartialProgress(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        int[] initialProgress, int[] bestProgress) {
        if (Arrays.equals(initialProgress, bestProgress)) return;
        applyProgress(task, pInfo, quest, bestProgress, false);
    }

    private static boolean isProgressComplete(TaskRetrieval task, int[] progress) {
        boolean complete = !task.requireOnlyOneItem;
        for (int i = 0; i < task.requiredItems.size(); i++) {
            int required = task.requiredItems.get(i).stackSize;
            if (task.requireOnlyOneItem) {
                complete |= getProgress(progress, i) >= required;
                if (complete) return true;
            } else {
                complete &= getProgress(progress, i) >= required;
                if (!complete) return false;
            }
        }
        return complete;
    }

    private static int[] mergeMaxProgress(TaskRetrieval task, int[] current, int[] candidate) {
        int[] merged = current.clone();
        for (int i = 0; i < task.requiredItems.size(); i++) {
            merged[i] = Math.max(getProgress(merged, i), getProgress(candidate, i));
        }
        return merged;
    }

    private static int getProgress(int[] progress, int index) {
        return index < progress.length ? progress[index] : 0;
    }

    private static final class NetworkView {

        private final TileBqInterface bqInterface;
        private final IStorageGrid storage;

        private NetworkView(TileBqInterface bqInterface, IStorageGrid storage) {
            this.bqInterface = bqInterface;
            this.storage = storage;
        }
    }

    private static final class AvailableItem {

        private final IAEItemStack stack;
        private long remaining;

        private AvailableItem(IAEItemStack stack) {
            this.stack = stack;
            this.remaining = stack.getStackSize();
        }
    }

    private static final class AvailableFluid {

        private final IAEFluidStack stack;
        private long remaining;

        private AvailableFluid(IAEFluidStack stack) {
            this(stack, stack.getStackSize());
        }

        private AvailableFluid(IAEFluidStack stack, long remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }

    private static final class AvailableRetrievalFluid {

        private final FluidStack stack;
        private long remaining;

        private AvailableRetrievalFluid(FluidStack stack, long remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }

    private static final class ItemExtraction {

        private final IAEItemStack stack;
        private final long amount;

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
