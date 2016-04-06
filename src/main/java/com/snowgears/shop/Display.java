package com.snowgears.shop;

import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Sign;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

public class Display {

    private ShopObject shop;
    private ArrayList<Item> items;

    public Display(ShopObject shop) {
        this.shop = shop;
        items = new ArrayList<Item>();
    }

    public void spawn() {
        deleteCurrentItems();
        Random random = new Random();

        //two items on the chest
        if (shop.getType() == ShopType.BARTER) {
            if (shop.getItemStack() == null || shop.getBarterItemStack() == null)
                return;
            //Drop first display item (the 'SELL' item)
            ItemStack sellDisplay = shop.getItemStack().clone();
            sellDisplay.setAmount(1);
            ItemMeta sellMeta = sellDisplay.getItemMeta();
            sellMeta.setDisplayName(shop.getOwnerUUID().toString()+random.nextInt()); // stop item stacking and aid in searching
            sellDisplay.setItemMeta(sellMeta);

            Item i1 = shop.getChestLocation().getWorld().dropItem(this.getDropLocation(false), sellDisplay);
            i1.setVelocity(new Vector(0, 0.1, 0));
            i1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
            items.add(i1);
            Shop.getPlugin().getDisplayListener().addDisplayItem(i1); //stop item from despawning

            //Drop second display item (the 'BUY' item)
            ItemStack buyDisplay = shop.getBarterItemStack().clone();
            buyDisplay.setAmount(1);
            ItemMeta buyMeta = buyDisplay.getItemMeta();
            buyMeta.setDisplayName(shop.getOwnerUUID().toString()+random.nextInt()); // stop item stacking and aid in searching
            buyDisplay.setItemMeta(buyMeta);

            Item i2 = shop.getChestLocation().getWorld().dropItem(this.getDropLocation(true), buyDisplay);
            i2.setVelocity(new Vector(0, 0.1, 0));
            i2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
            items.add(i2);
            Shop.getPlugin().getDisplayListener().addDisplayItem(i2); //stop item from despawning
        }
        //one item on the chest
        else {
            if (shop.getItemStack() == null)
                return;
            ItemStack display = shop.getItemStack().clone();
            display.setAmount(1);
            ItemMeta meta = display.getItemMeta();
            meta.setDisplayName(shop.getOwnerUUID().toString()+random.nextInt()); // stop item stacking and aid in searching
            display.setItemMeta(meta);

            Item i = shop.getChestLocation().getWorld().dropItem(this.getDropLocation(false), display);
            i.setVelocity(new Vector(0, 0.1, 0));
            i.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
            items.add(i);
            Shop.getPlugin().getDisplayListener().addDisplayItem(i); //stop item from despawning
        }
        shop.updateSign();
    }

    public void remove() {
        for (Item item : items) {
            Shop.getPlugin().getDisplayListener().removeDisplayItem(item);
            item.remove();
        }
    }

    public Location getLocation() {
        Location location = shop.getChestLocation().clone();
        location.add(0.5, 0.8, 0.5);
        return location;
    }

    private void deleteCurrentItems() {
        for (Entity e : shop.getChestLocation().getChunk().getEntities()) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                if (UtilMethods.basicLocationMatch(e.getLocation(), shop.getChestLocation())){
                    ItemMeta meta = ((Item) e).getItemStack().getItemMeta();
                    if(meta.getDisplayName() != null) {
                        if (meta.getDisplayName().contains(shop.getOwnerUUID().toString())) {
                            Shop.getPlugin().getDisplayListener().removeDisplayItem((Item) e);
                            e.remove();
                        }
                    }
                }
            }
        }
    }

    private Location getDropLocation(boolean isBarterItem) {
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