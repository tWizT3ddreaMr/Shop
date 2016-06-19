package com.snowgears.shop.listeners;

import com.snowgears.shop.Display;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class DisplayListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public DisplayListener(Shop instance) {
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
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (Display.isDisplay(event.getEntity())) {
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

        if(Display.isDisplay(event.getItem())){
            event.setCancelled(true);
            ShopObject shop = Display.getShop(event.getItem());
            if(shop != null)
                shop.getDisplay().spawn();
            else
                event.getItem().remove();
        }
    }

    //prevent hoppers from grabbing display items
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryPickupItemEvent event) {
        if(Display.isDisplay(event.getItem())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
