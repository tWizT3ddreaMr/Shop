package com.snowgears.shop.display;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.util.InventoryUtils;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DisplayListener implements Listener {

    public Shop plugin = Shop.getPlugin();
    private ArrayList<ItemStack> allServerRecipeResults = new ArrayList<>();

    public DisplayListener(Shop instance) {
        plugin = instance;

        new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<ItemStack, Boolean> recipes = new HashMap();
                Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
                while(recipeIterator.hasNext()) {
                    recipes.put(recipeIterator.next().getResult(), true);
                }
                allServerRecipeResults.addAll(recipes.keySet());
                Collections.shuffle(allServerRecipeResults);
            }
        }.runTaskLater(this.plugin, 1); //load all recipes on server once all other plugins are loaded
    }

    public ItemStack getRandomItem(ShopObject shop){
        if(shop == null)
            return new ItemStack(Material.AIR);

        if(InventoryUtils.isEmpty(shop.getInventory())) {
            int index = new Random().nextInt(allServerRecipeResults.size());
            //TODO maybe later on add random amount between 1-64 depending on item type
            //like you could get 46 stack of dirt but not 46 stack of swords
            return allServerRecipeResults.get(index);
        } else {
            return InventoryUtils.getRandomItem(shop.getInventory());
        }
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
}
