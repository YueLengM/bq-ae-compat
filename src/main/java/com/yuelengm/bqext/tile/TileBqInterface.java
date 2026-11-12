package com.yuelengm.bqext.tile;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import com.yuelengm.bqext.ae.BqInterfaceIndex;
import com.yuelengm.bqext.bq.PartyIdentity;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.GridAccessException;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.party.IParty;
import betterquesting.api2.storage.DBEntry;
import betterquesting.network.handlers.NetPartySync;
import betterquesting.questing.party.PartyManager;

public class TileBqInterface extends AENetworkTile {

    private static final int UNBOUND = -1;

    private int partyId = UNBOUND;
    private UUID partyUid;

    public TileBqInterface() {
        this.getProxy()
            .setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy()
            .setIdlePowerUsage(0.5);
    }

    public void onActivated(EntityPlayer player) {
        IGrid grid;
        try {
            grid = this.getProxy()
                .getGrid();
        } catch (GridAccessException e) {
            player.addChatMessage(new ChatComponentTranslation("bqext.chat.no_network"));
            return;
        }

        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        if (security != null && !security.hasPermission(player, SecurityPermissions.BUILD)) {
            player.addChatMessage(new ChatComponentTranslation("bqext.chat.no_permission"));
            return;
        }

        if (this.partyId != UNBOUND) {
            if (this.getBoundParty() == null) {
                player.addChatMessage(new ChatComponentTranslation("bqext.chat.bound_party_missing"));
            } else {
                player.addChatMessage(
                    new ChatComponentTranslation("bqext.chat.already_bound", this.getPartyDisplayName()));
            }
            return;
        }

        DBEntry<IParty> party = PartyManager.INSTANCE.getParty(QuestingAPI.getQuestingUUID(player));
        if (party == null) {
            player.addChatMessage(new ChatComponentTranslation("bqext.chat.no_party"));
            return;
        }

        this.partyId = party.getID();
        this.partyUid = PartyIdentity.getOrCreate(party.getValue());
        NetPartySync.quickSync(this.partyId);
        BqInterfaceIndex.add(this.getWorldObj(), this.partyUid, this.partyId, this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();

        player.addChatMessage(new ChatComponentTranslation("bqext.chat.bound", this.getPartyDisplayName()));
    }

    public boolean isBound() {
        return this.partyId != UNBOUND;
    }

    public boolean isBindingValid() {
        return this.getBoundParty() != null;
    }

    public int getPartyId() {
        return this.partyId;
    }

    public String getPartyName() {
        IParty party = this.getBoundParty();
        if (party == null) return "";

        String name = party.getProperties()
            .getProperty(NativeProps.NAME);
        return name == null ? "" : name;
    }

    public UUID getPartyUid() {
        return this.partyUid;
    }

    public IStorageGrid getStorageGrid() throws GridAccessException {
        return this.getProxy()
            .getStorage();
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!this.getWorldObj().isRemote && this.isBindingValid()) {
            BqInterfaceIndex
                .add(this.getWorldObj(), this.partyUid, this.partyId, this.xCoord, this.yCoord, this.zCoord);
        }
    }

    private String getPartyDisplayName() {
        String name = this.getPartyName();
        return name.isEmpty() ? "#" + this.partyId : name;
    }

    private IParty getBoundParty() {
        if (this.partyId == UNBOUND || this.partyUid == null) return null;

        IParty party = PartyManager.INSTANCE.getValue(this.partyId);
        if (party == null || !this.partyUid.equals(PartyIdentity.get(party))) return null;
        return party;
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_BqInterface(NBTTagCompound data) {
        this.partyId = data.hasKey("partyId") ? data.getInteger("partyId") : UNBOUND;
        this.partyUid = null;
        if (data.hasKey("partyUid")) {
            try {
                this.partyUid = UUID.fromString(data.getString("partyUid"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_BqInterface(NBTTagCompound data) {
        data.setInteger("partyId", this.partyId);
        if (this.partyUid != null) {
            data.setString("partyUid", this.partyUid.toString());
        }
    }
}
