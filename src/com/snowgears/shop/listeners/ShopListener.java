package com.snowgears.shop.listeners;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.utils.ShopMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Iterator;


public class ShopListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<String, Integer> shopBuildLimits = new HashMap<String, Integer>();

    public ShopListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if(plugin.usePerms()){
            Player player = event.getPlayer();
            int buildPermissionNumber = 0;
            boolean foundPerm = false;
            for (int i = 500; i > 0; i--) {
                if (player.hasPermission("shop.buildlimit." + i)) {
                    buildPermissionNumber = i;
                    foundPerm = true;
                    break;
                }
            }
            if(foundPerm)
                shopBuildLimits.put(player.getName(), buildPermissionNumber);
        }
    }

    public int getBuildLimit(Player player){
        if(shopBuildLimits.get(player.getName()) != null)
            return shopBuildLimits.get(player.getName());
        return Integer.MAX_VALUE;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopOpen(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.getShopHandler().isChest(event.getClickedBlock())) {

                Player player = event.getPlayer();
                ShopObject shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
                if (shop == null)
                    return;

                if(shop.getChestLocation().getBlock().getType() == Material.ENDER_CHEST) {
                    if(player.isSneaking()){
                        shop.printItemStats(player);
                        event.setCancelled(true);
                    }
                    return;
                }

                //non-owner is trying to open shop
                if (!shop.getOwnerName().equals(player.getName())) {
                    if ((plugin.usePerms() && player.hasPermission("plugin.operator")) || player.isOp()) {
                        if (shop.isAdminShop()) {
                            event.setCancelled(true);
                        } else
                            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "opOpen", shop, player));
                    } else {
                        event.setCancelled(true);
                        //player.sendMessage(ChatColor.RED + "You do not have access to open this shop.");
                    }
                    shop.printItemStats(player);
                } else if (shop.isAdminShop() && !player.isSneaking()) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "adminOpen", shop, player));
                    shop.printItemStats(player);
                    event.setCancelled(true);
                }
                //player is sneaking and clicks own shop
                else if(player.isSneaking()){
                    shop.printItemStats(player);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreateShop(final PlayerCreateShopEvent event) {
        if (event.isCancelled()) {
            event.getShop().delete();
            return;
        }
        Player player = event.getPlayer();
        event.getShop().updateSign();
        plugin.getCreativeSelectionListener().returnPlayerData(player);

        player.sendMessage(ShopMessage.getMessage(event.getShop().getType().toString(), "create", event.getShop(), player));
        plugin.getShopHandler().saveShops();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopDestroy(PlayerDestroyShopEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();

        if (event.getShop().getOwnerName().equals(player.getName())) {
            player.sendMessage(ShopMessage.getMessage(event.getShop().getType().toString(), "destroy", event.getShop(), player));
        } else {
            player.sendMessage(ShopMessage.getMessage(event.getShop().getType().toString(), "opDestroy", event.getShop(), player));
        }
        event.getShop().delete();
        plugin.getShopHandler().saveShops();
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        //save all potential shop blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        ShopObject shop = null;
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            if (block.getType() == Material.WALL_SIGN) {
                shop = plugin.getShopHandler().getShop(block.getLocation());
            } else if (plugin.getShopHandler().isChest(block)) {
                shop = plugin.getShopHandler().getShopByChest(block);
            }

            if (shop != null) {
                blockIterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void signDetachCheck(BlockPhysicsEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.WALL_SIGN) {
            ShopObject shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop != null) {
                event.setCancelled(true);
            }
        }
    }

    //===================================================================================//
    //              ENDER CHEST HANDLING EVENTS
    //===================================================================================//

    @EventHandler
    public void onCloseEnderChest(InventoryCloseEvent event){
        if(event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                plugin.getEnderChestHandler().updateInventory(player, event.getInventory());
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event){
        final Player player = event.getPlayer();
        final Inventory inv = plugin.getEnderChestHandler().getInventory(player);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if(inv != null){
                    plugin.getEnderChestHandler().updateInventory(player, inv);
                }
            }
        }, 2L);
    }
}