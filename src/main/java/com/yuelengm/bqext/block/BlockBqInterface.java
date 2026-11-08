package com.yuelengm.bqext.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.yuelengm.bqext.ae.BqInterfaceIndex;
import com.yuelengm.bqext.tile.TileBqInterface;

import appeng.block.AEBaseTileBlock;
import appeng.client.texture.FlippableIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBqInterface extends AEBaseTileBlock {

    public BlockBqInterface() {
        super(Material.iron);
        this.setTileEntity(TileBqInterface.class);
        this.setBlockName("bqext.bqInterface");
        this.setBlockTextureName("appliedenergistics2:BlockInterface");
        this.setHardness(2.5F);
        this.setResistance(10.0F);
    }

    @SideOnly(Side.CLIENT)
    public void setCompositeIcon(IIcon icon) {
        this.blockIcon = icon;
        FlippableIcon flippable = new FlippableIcon(icon);
        this.getRendererInstance()
            .updateIcons(flippable, flippable, flippable, flippable, flippable, flippable);
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY,
        float hitZ) {
        TileBqInterface tile = this.getTileEntity(world, x, y, z);
        if (tile == null) return false;

        if (!world.isRemote) {
            tile.onActivated(player);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        if (!world.isRemote) {
            BqInterfaceIndex.remove(world, x, y, z);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
