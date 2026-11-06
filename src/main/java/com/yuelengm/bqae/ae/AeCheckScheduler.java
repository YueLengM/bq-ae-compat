package com.yuelengm.bqae.ae;

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

    private static final int COOLDOWN_TICKS = 10;

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

        Long allowedAt = nextAllowed.get(key);
        if (allowedAt == null || currentTick >= allowedAt) {
            pending.remove(key);
            nextAllowed.put(key, currentTick + COOLDOWN_TICKS);
            run(task, pInfo, quest, kind);
            return true;
        }

        PendingCheck check = pending.get(key);
        if (check == null) {
            check = new PendingCheck(task, pInfo, quest, kind);
            pending.put(key, check);
        } else {
            check.update(pInfo, quest);
        }
        return true;
    }

    public static void tick() {
        currentTick++;
        if (pending.isEmpty()) return;

        List<CheckKey> dueKeys = new ArrayList<>();
        Map<CheckKey, PendingCheck> dueChecks = new HashMap<>();
        Iterator<Map.Entry<CheckKey, PendingCheck>> iterator = pending.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<CheckKey, PendingCheck> entry = iterator.next();
            Long allowedAt = nextAllowed.get(entry.getKey());
            if (allowedAt != null && currentTick >= allowedAt) {
                dueKeys.add(entry.getKey());
                dueChecks.put(entry.getKey(), entry.getValue());
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

        private PendingCheck(ITask task, ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest, CheckKind kind) {
            this.task = task;
            this.pInfo = pInfo;
            this.quest = quest;
            this.kind = kind;
        }

        private void update(ParticipantInfo pInfo, Map.Entry<UUID, IQuest> quest) {
            this.pInfo = pInfo;
            this.quest = quest;
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
