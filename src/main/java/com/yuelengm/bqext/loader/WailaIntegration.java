package com.yuelengm.bqext.loader;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.yuelengm.bqext.tile.TileBqInterface;

import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

public final class WailaIntegration implements IWailaDataProvider {

    private static final String TAG_STATUS = "bqextStatus";
    private static final String TAG_PARTY_NAME = "bqextPartyName";
    private static final String TAG_PARTY_ID = "bqextPartyId";

    private static final int STATUS_UNBOUND = 0;
    private static final int STATUS_BOUND = 1;
    private static final int STATUS_PARTY_MISSING = 2;

    private WailaIntegration() {}

    public static void init() {
        FMLInterModComms.sendMessage(ModIds.WAILA, "register", WailaIntegration.class.getName() + ".register");
    }

    @SuppressWarnings("unused")
    public static void register(IWailaRegistrar registrar) {
        WailaIntegration provider = new WailaIntegration();
        registrar.registerBodyProvider(provider, TileBqInterface.class);
        registrar.registerNBTProvider(provider, TileBqInterface.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currentToolTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        return currentToolTip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currentToolTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        if (!(accessor.getTileEntity() instanceof TileBqInterface)) return currentToolTip;

        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey(TAG_STATUS)) return currentToolTip;

        int status = tag.getInteger(TAG_STATUS);
        if (status == STATUS_UNBOUND) {
            currentToolTip.add(StatCollector.translateToLocal("bqext.waila.unbound"));
        } else if (status == STATUS_PARTY_MISSING) {
            currentToolTip.add(StatCollector.translateToLocal("bqext.waila.party_missing"));
        } else if (status == STATUS_BOUND) {
            currentToolTip
                .add(StatCollector.translateToLocalFormatted("bqext.waila.bound", tag.getString(TAG_PARTY_NAME)));
        }

        return currentToolTip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currentToolTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        return currentToolTip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x,
        int y, int z) {
        if (!(tile instanceof TileBqInterface)) return tag;

        TileBqInterface bqInterface = (TileBqInterface) tile;
        tag.setInteger(TAG_PARTY_ID, bqInterface.getPartyId());

        if (!bqInterface.isBound()) {
            tag.setInteger(TAG_STATUS, STATUS_UNBOUND);
        } else if (!bqInterface.isBindingValid()) {
            tag.setInteger(TAG_STATUS, STATUS_PARTY_MISSING);
        } else {
            String partyName = bqInterface.getPartyName();
            if (partyName.isEmpty()) partyName = "#" + bqInterface.getPartyId();

            tag.setInteger(TAG_STATUS, STATUS_BOUND);
            tag.setString(TAG_PARTY_NAME, partyName);
        }

        return tag;
    }
}
