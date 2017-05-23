package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShopGUIListener implements Listener {

    private Shop plugin = Shop.getPlugin();

    public ShopGUIListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player)event.getWhoClicked();

            ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);

            if(event.getInventory().getTitle().equals(window.getInventory().getTitle())){

                ItemStack clicked = event.getCurrentItem();
                if(clicked != null){

                    event.setCancelled(true);

                    //this is the case in all windows
                    if(clicked.getType() == Material.STAINED_GLASS_PANE){
                        if(window.hasPrevWindow()){
                            plugin.getGuiHandler().setWindow(player, window.prevWindow);
                            return;
                        }
                    }

                    if(window instanceof HomeWindow){
                        if(clicked.getType() == Material.CHEST){
                            ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), player.getUniqueId());
                            shopsWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, shopsWindow);
                            return;
                        }
                        else if(clicked.getType() == Material.SKULL_ITEM){
                            ListPlayersWindow playersWindow = new ListPlayersWindow(player.getUniqueId());
                            playersWindow.setPrevWindow(window);
                            plugin.getGuiHandler().setWindow(player, playersWindow);
                            return;
                        }
                    }
                    else if(window instanceof ListPlayersWindow){
                        if(clicked.getType() == Material.SKULL_ITEM || clicked.getType() == Material.CHEST){
                            List<String> lore = clicked.getItemMeta().getLore();
                            if(lore != null){
                                for(String line : lore){
                                    if(line.startsWith("UUID: ")){
                                        line = line.substring(6, line.length());
                                        UUID playerUUID = UUID.fromString(line);

                                        ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), playerUUID);
                                        shopsWindow.setPrevWindow(window);

                                        plugin.getGuiHandler().setWindow(player, shopsWindow);


                                        return;
                                    }
                                }
                            }
                        }
                    }
                    else if(window instanceof ListShopsWindow){
                        List<String> lore = clicked.getItemMeta().getLore();
                        if(lore != null){
                            for(String line : lore){
                                if(line.startsWith("Location: ")){
                                    line = line.substring(10, line.length());
                                    Location loc = UtilMethods.getLocation(line);
                                    player.teleport(loc);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
//            System.out.println("Inventory slot: "+event.getSlot());
//            System.out.println("Inventory raw slot: "+event.getRawSlot());
        }
    }
}
