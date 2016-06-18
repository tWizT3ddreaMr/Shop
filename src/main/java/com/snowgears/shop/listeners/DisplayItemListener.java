package com.snowgears.shop.listeners;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.HashMap;
import java.util.UUID;

public class DisplayItemListener implements Listener {

    public Shop plugin = Shop.getPlugin();
    private HashMap<UUID, Boolean> displayEntities = new HashMap<UUID, Boolean>();

    public DisplayItemListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onWaterFlow(BlockFromToEvent event) {
        ShopObject shop = plugin.getShopHandler().getShopByChest(event.getToBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombust(EntityCombustEvent event) {
        if (isDisplayEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isDisplayEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ShopObject shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }


    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPickup(PlayerPickupItemEvent event) {
        if(event.isCancelled())
            return;

        boolean goneWrong = false;
        if (isDisplayEntity(event.getItem())) {
            event.setCancelled(true);
            goneWrong = true;
        }
        else if(UtilMethods.stringStartsWithUUID(event.getItem().getItemStack().getItemMeta().getDisplayName())) {
            event.setCancelled(true);
            goneWrong = true;
        }

        if(goneWrong){
            plugin.getShopHandler().refreshShopDisplays();
        }
    }

    //prevent hoppers from grabbing display items
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryPickupItemEvent event) {
        if(UtilMethods.stringStartsWithUUID(event.getItem().getItemStack().getItemMeta().getDisplayName())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isDisplayEntity(event.getEntity())) {
            event.setCancelled(true);
        }
        else if(UtilMethods.stringStartsWithUUID(event.getEntity().getItemStack().getItemMeta().getDisplayName())) {
            event.setCancelled(true);
        }
    }

    public void addDisplay(Entity entity) {
        displayEntities.put(entity.getUniqueId(), true);
    }

    public void removeDisplay(Entity entity) {
        if (isDisplayEntity(entity)) {
            displayEntities.remove(entity.getUniqueId());
        }
    }

    public boolean isDisplayEntity(Entity entity) {
        return displayEntities.get(entity.getUniqueId()) != null;
    }
}
