package com.snowgears.shop;

import com.snowgears.shop.utils.DisplayUtil;
import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Sign;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Display {

    private ShopObject shop;
    private ArrayList<Entity> entities;

    public Display(ShopObject shop) {
        this.shop = shop;
        entities = new ArrayList<>();
    }

    public void spawn() {
        remove();
        Random random = new Random();

        if (shop.getItemStack() == null)
            return;

        //define the initial display item
        ItemStack item = shop.getItemStack().clone();
        item.setAmount(1);
        ItemMeta sellMeta = item.getItemMeta();
        sellMeta.setDisplayName(shop.getOwnerUUID().toString()+random.nextInt()); // stop item stacking and aid in searching
        item.setItemMeta(sellMeta);

        //two display entities on the chest
        if (shop.getType() == ShopType.BARTER) {
            if (shop.getBarterItemStack() == null)
                return;

            //define the barter display item
            ItemStack barterItem = shop.getBarterItemStack().clone();
            barterItem.setAmount(1);
            ItemMeta buyMeta = barterItem.getItemMeta();
            buyMeta.setDisplayName(shop.getOwnerUUID().toString()+random.nextInt()); // stop item stacking and aid in searching
            barterItem.setItemMeta(buyMeta);

            switch (Shop.getPlugin().getDisplayType()){
                case NONE:
                    //TODO (get from signConfig somehow)
                    break;
                case ITEM:
                    //Drop initial display item
                    Item i1 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    i1.setVelocity(new Vector(0, 0.1, 0));
                    i1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i1);
                    Shop.getPlugin().getDisplayListener().addDisplay(i1); //stop item from despawning

                    //Drop the barter display item
                    Item i2 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(true), barterItem);
                    i2.setVelocity(new Vector(0, 0.1, 0));
                    i2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i2);
                    Shop.getPlugin().getDisplayListener().addDisplay(i2); //stop item from despawning
                    break;
                case LARGE_ITEM:
                    //TODO (make getLargeItemLocation method for spacing out the two large items on one chest)
                    break;
            }
        }
        //one display entity on the chest
        else {
            switch (Shop.getPlugin().getDisplayType()){
                case NONE:
                    //TODO (get from signConfig somehow)
                    break;
                case ITEM:
                    Item i = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    i.setVelocity(new Vector(0, 0.1, 0));
                    i.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i);
                    Shop.getPlugin().getDisplayListener().addDisplay(i); //stop item from despawning
                    break;
                case LARGE_ITEM:
                    ArmorStand stand = DisplayUtil.createDisplay(item, shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation(), shop.getFacing());
                    stand.setCustomName(shop.getOwnerUUID().toString());
                    stand.setCustomNameVisible(false);
                    entities.add(stand);
                    Shop.getPlugin().getDisplayListener().addDisplay(stand); //stop item from despawning
                    break;
            }
        }
        shop.updateSign();

        removeOldItems();
    }

    public void remove() {
        Iterator<Entity> displayIterator = entities.iterator();
        while(displayIterator.hasNext()) {
            Entity item = displayIterator.next();
            Shop.getPlugin().getDisplayListener().removeDisplay(item);
            item.remove();
        }
        entities.clear();
    }

//    public Location getLocation() {
//        Location location = shop.getChestLocation().clone();
//        location.add(0.5, 0.8, 0.5);
//        return location;
//    }

    private void removeOldItems() {
        for(Entity e : entities){
            for(Entity oldItem : e.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (oldItem.getType() == EntityType.DROPPED_ITEM) {
                    ItemMeta itemMeta = ((Item) oldItem).getItemStack().getItemMeta();
                    if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                        if ((!entities.contains(oldItem)))
                            oldItem.remove();
                    }
                }
            }
        }
//        for (Entity e : shop.getChestLocation().getChunk().getEntities()) {
//            if (e.getType() == EntityType.DROPPED_ITEM) {
//                if (UtilMethods.basicLocationMatch(e.getLocation(), shop.getChestLocation())){
//                    ItemMeta meta = ((Item) e).getItemStack().getItemMeta();
//                    if(meta.getDisplayName() != null) {
//                        if (meta.getDisplayName().contains(shop.getOwnerUUID().toString())) {
//                            Shop.getPlugin().getDisplayListener().removeDisplayItem((Item) e);
//                            e.remove();
//                        }
//                    }
//                }
//            }
//        }
    }

    private Location getItemDropLocation(boolean isBarterItem) {
        //calculate which x,z to drop items at depending on direction of the shop sign
        double dropY = 1.2;
        double dropX = 0.5;
        double dropZ = 0.5;
        if (shop.getType() == ShopType.BARTER) {
            Sign shopSign = (Sign) shop.getSignLocation().getBlock().getState().getData();
            switch (shopSign.getFacing()) {
                case NORTH:
                    if (isBarterItem)
                        dropX = 0.3;
                    else
                        dropX = 0.7;
                    break;
                case EAST:
                    if (isBarterItem)
                        dropZ = 0.3;
                    else
                        dropZ = 0.7;
                    break;
                case SOUTH:
                    if (isBarterItem)
                        dropX = 0.7;
                    else
                        dropX = 0.3;
                    break;
                case WEST:
                    if (isBarterItem)
                        dropZ = 0.7;
                    else
                        dropZ = 0.3;
                    break;
                default:
                    dropX = 0.5;
                    dropZ = 0.5;
                    break;
            }
        }
        return shop.getChestLocation().clone().add(dropX, dropY, dropZ);
    }
}