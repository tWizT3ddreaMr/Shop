package com.snowgears.shop;


import com.snowgears.shop.utils.DisplayUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

public class LargeItem {

    private ArmorStand armorStand;
    private Location blockLocation;
    private ItemStack itemStack;
    private BlockFace facing;

    public LargeItem(ItemStack itemStack, Location blockLocation, BlockFace facing){
        this.itemStack = itemStack;
        this.blockLocation = blockLocation;
        this.facing = facing;
    }

    private void spawn(){
        remove();
        armorStand = DisplayUtil.createDisplay(itemStack, blockLocation, facing);
    }

    public void remove(){
        if(armorStand != null)
            armorStand.remove();
    }

    public ItemStack getItemStack(){
        return itemStack;
    }

    public Location getBlockLocation(){
        return blockLocation;
    }

    public BlockFace getFacing(){
        return facing;
    }

    public void setFacing(BlockFace facing){
        this.facing = facing;
        spawn();
    }
}
