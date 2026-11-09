package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.yuelengm.bqext.ae.AeTaskDetector.NetworkView;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;

final class AeStorageSnapshotCache {

    private static final Map<UUID, List<IAEItemStack>> itemSnapshots = new HashMap<>();
    private static final Map<UUID, List<IAEFluidStack>> fluidSnapshots = new HashMap<>();

    private AeStorageSnapshotCache() {}

    static List<IAEItemStack> getItemSnapshot(NetworkView network) {
        List<IAEItemStack> snapshot = itemSnapshots.get(network.networkId);
        if (snapshot == null) {
            snapshot = captureItems(network.storage.getItemInventory());
            itemSnapshots.put(network.networkId, snapshot);
        }
        return snapshot;
    }

    static List<IAEFluidStack> getFluidSnapshot(NetworkView network) {
        List<IAEFluidStack> snapshot = fluidSnapshots.get(network.networkId);
        if (snapshot == null) {
            snapshot = captureFluids(network.storage.getFluidInventory());
            fluidSnapshots.put(network.networkId, snapshot);
        }
        return snapshot;
    }

    static void invalidateItems(NetworkView network) {
        itemSnapshots.remove(network.networkId);
    }

    static void invalidateFluids(NetworkView network) {
        fluidSnapshots.remove(network.networkId);
    }

    static void clear() {
        itemSnapshots.clear();
        fluidSnapshots.clear();
    }

    private static List<IAEItemStack> captureItems(IMEMonitor<IAEItemStack> inventory) {
        if (inventory == null) return Collections.emptyList();

        List<IAEItemStack> snapshot = new ArrayList<>();
        for (IAEItemStack stack : inventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) snapshot.add(stack.copy());
        }
        return snapshot;
    }

    private static List<IAEFluidStack> captureFluids(IMEMonitor<IAEFluidStack> inventory) {
        if (inventory == null) return Collections.emptyList();

        List<IAEFluidStack> snapshot = new ArrayList<>();
        for (IAEFluidStack stack : inventory.getStorageList()) {
            if (stack != null && stack.getStackSize() > 0) snapshot.add(stack.copy());
        }
        return snapshot;
    }
}
