package com.snowgears.shop.listeners;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.UUID;

public class DisplayItemListener implements Listener {

    public Shop plugin = Shop.getPlugin();
    private HashMap<UUID, Boolean> displayItems = new HashMap<UUID, Boolean>();

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
        if (displayItems.get(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (displayItems.get(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ShopObject shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (displayItems.get(event.getItem().getUniqueId()) != null) {
            event.setCancelled(true);

            //do a hard reset on all shop items
            for (World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getType() == EntityType.DROPPED_ITEM) {
                        ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                        if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                            entity.remove();
                        }
                    }
                }
            }
            plugin.getShopHandler().refreshShopItems();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (displayItems.get(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    public void addDisplayItem(Item i) {
        displayItems.put(i.getUniqueId(), true);
    }

    public void removeDisplayItem(Item i) {
        if (displayItems.get(i.getUniqueId()) != null) {
            displayItems.remove(i.getUniqueId());
        }
    }

    public boolean containsItem(Entity entity) {
        return displayItems.get(entity.getUniqueId()) != null;
    }
}
