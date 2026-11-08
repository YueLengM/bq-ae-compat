package com.yuelengm.bqext.ae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;

import com.yuelengm.bqext.BQExt;
import com.yuelengm.bqext.tile.TileBqInterface;

public class BqInterfaceIndex extends WorldSavedData {

    private static final String DATA_NAME = BQExt.MODID + "_interfaces";

    private final Map<AnchorPosition, Anchor> anchors = new HashMap<>();
    private final Map<UUID, Set<AnchorPosition>> anchorsByParty = new HashMap<>();
    private final Map<ChunkKey, Set<AnchorPosition>> anchorsByChunk = new HashMap<>();

    public BqInterfaceIndex(String name) {
        super(name);
    }

    public static BqInterfaceIndex get(World world) {
        if (world == null || world.isRemote) return null;
        return getServerIndex();
    }

    private static BqInterfaceIndex getServerIndex() {
        World overworld = DimensionManager.getWorld(0);
        if (overworld == null) return null;

        MapStorage storage = overworld.mapStorage;
        BqInterfaceIndex index = (BqInterfaceIndex) storage.loadData(BqInterfaceIndex.class, DATA_NAME);
        if (index == null) {
            index = new BqInterfaceIndex(DATA_NAME);
            storage.setData(DATA_NAME, index);
        }
        return index;
    }

    public static void add(World world, UUID partyUid, int partyId, int x, int y, int z) {
        BqInterfaceIndex index = get(world);
        if (index != null) {
            index.add(world.provider.dimensionId, partyUid, partyId, x, y, z);
        }
    }

    public static void remove(World world, int x, int y, int z) {
        BqInterfaceIndex index = get(world);
        if (index != null) {
            index.remove(world.provider.dimensionId, x, y, z);
        }
    }

    public static void clearPartyOnServer(UUID partyUid, int partyId) {
        BqInterfaceIndex index = getServerIndex();
        if (index != null) {
            index.clearParty(partyUid, partyId);
        }
    }

    public static List<TileBqInterface> getLoadedInterfaces(UUID partyUid) {
        BqInterfaceIndex index = getServerIndex();
        if (index == null) return Collections.emptyList();
        return index.getLoadedInterfacesInternal(partyUid);
    }

    public static boolean hasAnchors(UUID partyUid) {
        BqInterfaceIndex index = getServerIndex();
        return index != null && index.hasAnchorsInternal(partyUid);
    }

    private boolean hasAnchorsInternal(UUID partyUid) {
        Set<AnchorPosition> positions = this.anchorsByParty.get(partyUid);
        return positions != null && !positions.isEmpty();
    }

    public List<Anchor> getAnchors(UUID partyUid) {
        Set<AnchorPosition> positions = this.anchorsByParty.get(partyUid);
        if (positions == null || positions.isEmpty()) return Collections.emptyList();

        List<Anchor> result = new ArrayList<>(positions.size());
        for (AnchorPosition position : positions) {
            Anchor anchor = this.anchors.get(position);
            if (anchor != null) result.add(anchor);
        }
        return result;
    }

    public void validateChunk(World world, int chunkX, int chunkZ) {
        ChunkKey chunkKey = new ChunkKey(world.provider.dimensionId, chunkX, chunkZ);
        Set<AnchorPosition> positions = this.anchorsByChunk.get(chunkKey);
        if (positions == null || positions.isEmpty()) return;

        for (AnchorPosition position : new ArrayList<>(positions)) {
            TileEntity tile = world.getTileEntity(position.x, position.y, position.z);
            Anchor anchor = this.anchors.get(position);
            if (anchor == null) {
                this.removePosition(position);
                continue;
            }

            if (!(tile instanceof TileBqInterface)) {
                this.removePosition(position);
                continue;
            }

            if (!anchor.partyUid.equals(((TileBqInterface) tile).getPartyUid())) {
                this.removePosition(position);
            }
        }
    }

    private List<TileBqInterface> getLoadedInterfacesInternal(UUID partyUid) {
        Set<AnchorPosition> positions = this.anchorsByParty.get(partyUid);
        if (positions == null || positions.isEmpty()) return Collections.emptyList();

        List<TileBqInterface> interfaces = new ArrayList<>();
        for (AnchorPosition position : new ArrayList<>(positions)) {
            World world = DimensionManager.getWorld(position.dimension);
            if (world == null || world.isRemote
                || !world.getChunkProvider()
                    .chunkExists(position.x >> 4, position.z >> 4)) {
                continue;
            }

            TileEntity tile = world.getTileEntity(position.x, position.y, position.z);
            Anchor anchor = this.anchors.get(position);
            if (!(tile instanceof TileBqInterface)) {
                this.removePosition(position);
                continue;
            }

            TileBqInterface bqInterface = (TileBqInterface) tile;
            if (anchor == null || !anchor.partyUid.equals(bqInterface.getPartyUid())) {
                this.removePosition(position);
                continue;
            }

            if (!bqInterface.isBindingValid()) {
                this.removePosition(position);
                continue;
            }

            interfaces.add(bqInterface);
        }
        return interfaces;
    }

    private void add(int dimension, UUID partyUid, int partyId, int x, int y, int z) {
        AnchorPosition position = new AnchorPosition(dimension, x, y, z);
        Anchor oldAnchor = this.anchors.get(position);
        if (oldAnchor != null) {
            if (oldAnchor.partyUid.equals(partyUid)) {
                if (oldAnchor.partyId != partyId) {
                    oldAnchor.partyId = partyId;
                    this.markDirty();
                }
                return;
            }
            this.removePosition(position);
        }

        Anchor anchor = new Anchor(partyUid, partyId, position);
        this.anchors.put(position, anchor);
        this.anchorsByParty.computeIfAbsent(partyUid, ignored -> new HashSet<>())
            .add(position);
        this.anchorsByChunk.computeIfAbsent(ChunkKey.from(position), ignored -> new HashSet<>())
            .add(position);
        this.markDirty();
    }

    private void remove(int dimension, int x, int y, int z) {
        this.removePosition(new AnchorPosition(dimension, x, y, z));
    }

    private void clearParty(UUID partyUid, int partyId) {
        List<AnchorPosition> positions = new ArrayList<>();
        Set<AnchorPosition> partyPositions = partyUid == null ? null : this.anchorsByParty.get(partyUid);
        if (partyPositions != null) positions.addAll(partyPositions);

        if (partyUid == null) {
            for (Anchor anchor : this.anchors.values()) {
                if (anchor.partyId == partyId) positions.add(anchor.position);
            }
        }

        for (AnchorPosition position : positions) {
            this.removePosition(position);
        }
    }

    private void removePosition(AnchorPosition position) {
        Anchor anchor = this.anchors.remove(position);
        if (anchor == null) return;

        this.removeFromSet(this.anchorsByParty, anchor.partyUid, position);
        this.removeFromSet(this.anchorsByChunk, ChunkKey.from(position), position);
        this.markDirty();
    }

    private <K> void removeFromSet(Map<K, Set<AnchorPosition>> map, K key, AnchorPosition position) {
        Set<AnchorPosition> positions = map.get(key);
        if (positions == null) return;

        positions.remove(position);
        if (positions.isEmpty()) map.remove(key);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.anchors.clear();
        this.anchorsByParty.clear();
        this.anchorsByChunk.clear();

        NBTTagList list = nbt.getTagList("anchors", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            try {
                UUID partyUid = UUID.fromString(entry.getString("partyUid"));
                this.add(
                    entry.getInteger("dimension"),
                    partyUid,
                    entry.getInteger("partyId"),
                    entry.getInteger("x"),
                    entry.getInteger("y"),
                    entry.getInteger("z"));
            } catch (IllegalArgumentException ignored) {}
        }
        this.setDirty(false);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Anchor anchor : this.anchors.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("partyUid", anchor.partyUid.toString());
            entry.setInteger("partyId", anchor.partyId);
            entry.setInteger("dimension", anchor.position.dimension);
            entry.setInteger("x", anchor.position.x);
            entry.setInteger("y", anchor.position.y);
            entry.setInteger("z", anchor.position.z);
            list.appendTag(entry);
        }
        nbt.setTag("anchors", list);
    }

    public static final class Anchor {

        private final UUID partyUid;
        private int partyId;
        private final AnchorPosition position;

        private Anchor(UUID partyUid, int partyId, AnchorPosition position) {
            this.partyUid = partyUid;
            this.partyId = partyId;
            this.position = position;
        }

        public UUID getPartyUid() {
            return this.partyUid;
        }

        public int getPartyId() {
            return this.partyId;
        }

        public int getDimension() {
            return this.position.dimension;
        }

        public int getX() {
            return this.position.x;
        }

        public int getY() {
            return this.position.y;
        }

        public int getZ() {
            return this.position.z;
        }
    }

    private static final class AnchorPosition {

        private final int dimension;
        private final int x;
        private final int y;
        private final int z;

        private AnchorPosition(int dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AnchorPosition)) return false;

            AnchorPosition other = (AnchorPosition) obj;
            return this.dimension == other.dimension && this.x == other.x && this.y == other.y && this.z == other.z;
        }

        @Override
        public int hashCode() {
            int result = this.dimension;
            result = 31 * result + this.x;
            result = 31 * result + this.y;
            result = 31 * result + this.z;
            return result;
        }
    }

    private static final class ChunkKey {

        private final int dimension;
        private final int x;
        private final int z;

        private ChunkKey(int dimension, int x, int z) {
            this.dimension = dimension;
            this.x = x;
            this.z = z;
        }

        private static ChunkKey from(AnchorPosition position) {
            return new ChunkKey(position.dimension, position.x >> 4, position.z >> 4);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ChunkKey)) return false;

            ChunkKey other = (ChunkKey) obj;
            return this.dimension == other.dimension && this.x == other.x && this.z == other.z;
        }

        @Override
        public int hashCode() {
            int result = this.dimension;
            result = 31 * result + this.x;
            result = 31 * result + this.z;
            return result;
        }
    }
}
