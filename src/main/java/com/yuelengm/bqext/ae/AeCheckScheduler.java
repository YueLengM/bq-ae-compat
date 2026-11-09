package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskFluid;
import bq_standard.tasks.TaskRetrieval;

public final class AeCheckScheduler {

    private static final int DEBOUNCE_TICKS = 20;
    private static final int COOLDOWN_TICKS = 20;

    private static final Map<CheckKey, PendingCheck> pending = new HashMap<>();
    private static final Map<CheckKey, Long> nextAllowed = new HashMap<>();
    private static long currentTick;

    private AeCheckScheduler() {}

    public static boolean request(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, CheckKind kind) {
        CheckKey key = new CheckKey(task, pInfo.UUID, quest.getKey(), kind);
        if (task.isComplete(pInfo.UUID) || !AeTaskDetector.hasAnchors(pInfo)) {
            pending.remove(key);
            nextAllowed.remove(key);
            return false;
        }

        PendingCheck check = pending.get(key);
        if (check == null) {
            check = new PendingCheck(task, pInfo, quest, kind, currentTick);
            pending.put(key, check);
        } else {
            check.update(pInfo, quest, currentTick);
        }
        return true;
    }

    public static void tick() {
        currentTick++;
        AeStorageSnapshotCache.clear();
        if (pending.isEmpty()) return;

        List<CheckKey> dueKeys = new ArrayList<>();
        Map<CheckKey, PendingCheck> dueChecks = new HashMap<>();
        Iterator<Map.Entry<CheckKey, PendingCheck>> iterator = pending.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<CheckKey, PendingCheck> entry = iterator.next();
            Long allowedAt = nextAllowed.get(entry.getKey());
            PendingCheck check = entry.getValue();
            if (currentTick - check.lastRequestTick >= DEBOUNCE_TICKS
                && (allowedAt == null || currentTick >= allowedAt)) {
                dueKeys.add(entry.getKey());
                dueChecks.put(entry.getKey(), check);
                iterator.remove();
            }
        }

        for (CheckKey key : dueKeys) {
            PendingCheck check = dueChecks.get(key);
            if (check == null) continue;

            nextAllowed.put(key, currentTick + COOLDOWN_TICKS);
            run(check.task, check.pInfo, check.quest, check.kind);
            if (check.task.isComplete(check.pInfo.UUID)) {
                nextAllowed.remove(key);
            }
        }
    }

    public static void clear() {
        pending.clear();
        nextAllowed.clear();
        AeStorageSnapshotCache.clear();
        currentTick = 0;
    }

    private static void run(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, CheckKind kind) {
        if (kind == CheckKind.ITEM && task instanceof TaskRetrieval) {
            AeTaskDetector.detectItems((TaskRetrieval) task, pInfo, quest);
        } else if (kind == CheckKind.FLUID && task instanceof TaskFluid) {
            AeTaskDetector.detectFluids((TaskFluid) task, pInfo, quest);
        }
    }

    public enum CheckKind {
        ITEM,
        FLUID
    }

    private static final class PendingCheck {

        private final ITask task;
        private ParticipantInfo pInfo;
        private Map.Entry<UUID, IQuest> quest;
        private final CheckKind kind;
        private long lastRequestTick;

        private PendingCheck(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, CheckKind kind,
            long lastRequestTick) {
            this.task = task;
            this.pInfo = pInfo;
            this.quest = quest;
            this.kind = kind;
            this.lastRequestTick = lastRequestTick;
        }

        private void update(ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, long lastRequestTick) {
            this.pInfo = pInfo;
            this.quest = quest;
            this.lastRequestTick = lastRequestTick;
        }
    }

    private static final class CheckKey {

        private final ITask task;
        private final UUID playerId;
        private final UUID questId;
        private final CheckKind kind;

        private CheckKey(ITask task, UUID playerId, UUID questId, CheckKind kind) {
            this.task = task;
            this.playerId = playerId;
            this.questId = questId;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CheckKey)) return false;

            CheckKey other = (CheckKey) obj;
            return this.task == other.task && this.playerId.equals(other.playerId)
                && this.questId.equals(other.questId)
                && this.kind == other.kind;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.task);
            result = 31 * result + this.playerId.hashCode();
            result = 31 * result + this.questId.hashCode();
            result = 31 * result + this.kind.hashCode();
            return result;
        }
    }
}
