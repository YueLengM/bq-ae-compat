package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.yuelengm.bqext.bq.PartyIdentity;
import com.yuelengm.bqext.config.BqExtConfig;
import com.yuelengm.bqext.mixins.late.TaskProgressableBaseAccessor;
import com.yuelengm.bqext.tile.TileBqInterface;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.GridAccessException;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.party.IParty;
import betterquesting.api.questing.tasks.ITask;
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

        AeRetrievalDetector.detect(task, pInfo, quest, networks, initialProgress);
        if (task.isComplete(pInfo.UUID)) return;

        if (!task.consume && BqExtConfig.retrievalFluidCompat) {
            AeFluidContainerDetector.detect(task, pInfo, quest, networks, initialProgress);
        }
    }

    public static void detectFluids(TaskFluid task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest) {
        AeFluidDetector.detect(task, pInfo, quest, resolveNetworks(pInfo));
    }

    static List<NetworkView> resolveNetworks(ParticipantInfo pInfo) {
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

                networks.putIfAbsent(grid.getId(), new NetworkView(grid.getId(), bqInterface, storage));
            } catch (GridAccessException ignored) {}
        }
        return new ArrayList<>(networks.values());
    }

    static void completeTask(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, int[] progress) {
        applyProgress(task, pInfo, quest, progress, true);
    }

    static void applyProgress(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, int[] progress,
        boolean complete) {
        TaskProgressableBaseAccessor accessor = (TaskProgressableBaseAccessor) task;
        for (UUID uuid : pInfo.ALL_UUIDS) {
            accessor.bqext$setUserProgress(uuid, progress.clone());
            if (complete) task.setComplete(uuid);
        }
        pInfo.markDirtyParty(quest.getKey());
    }

    static void applyPartialProgress(TaskRetrieval task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest,
        int[] initialProgress, int[] bestProgress) {
        if (Arrays.equals(initialProgress, bestProgress)) return;
        applyProgress(task, pInfo, quest, bestProgress, false);
    }

    static boolean isProgressComplete(TaskRetrieval task, int[] progress) {
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

    static int[] mergeMaxProgress(TaskRetrieval task, int[] current, int[] candidate) {
        int[] merged = current.clone();
        for (int i = 0; i < task.requiredItems.size(); i++) {
            merged[i] = Math.max(getProgress(merged, i), getProgress(candidate, i));
        }
        return merged;
    }

    static int getProgress(int[] progress, int index) {
        return index < progress.length ? progress[index] : 0;
    }

    private static UUID getPartyUid(ParticipantInfo pInfo) {
        DBEntry<IParty> party = pInfo.PARTY_INSTANCE;
        if (party == null) return null;
        return PartyIdentity.get(party.getValue());
    }

    static final class NetworkView {

        final UUID networkId;
        final TileBqInterface bqInterface;
        final IStorageGrid storage;

        private NetworkView(UUID networkId, TileBqInterface bqInterface, IStorageGrid storage) {
            this.networkId = networkId;
            this.bqInterface = bqInterface;
            this.storage = storage;
        }
    }
}
