package com.yuelengm.bqae.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.yuelengm.bqae.index.BqInterfaceIndex;
import com.yuelengm.bqae.tile.TileBqInterface;

import appeng.block.AEBaseTileBlock;

public class BlockBqInterface extends AEBaseTileBlock {

    public BlockBqInterface() {
        super(Material.iron);
        this.setTileEntity(TileBqInterface.class);
        this.setBlockName("bqae.bqInterface");
        this.setBlockTextureName("appliedenergistics2:BlockSecurity");
        this.setHardness(2.5F);
        this.setResistance(10.0F);
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
