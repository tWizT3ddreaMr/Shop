package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
                if(clicked != null && clicked.getType() != Material.AIR){

                    event.setCancelled(true);

                    //this is the case in all windows
                    if(event.getRawSlot() == 0 && clicked.getType() == Material.BARRIER){
                        if(window.hasPrevWindow()){
                            plugin.getGuiHandler().setWindow(player, window.prevWindow);
                            return;
                        }
                    }

                    //this is the case in all windows
                    if(event.getRawSlot() == 8 && clicked.getType() == Material.COMPASS) {
                        SearchWindow searchWindow = new SearchWindow(player.getUniqueId());
                        searchWindow.setPrevWindow(window);
                        plugin.getGuiHandler().setWindow(player, searchWindow);
                        return;
                    }

                    //this is the case in all windows
                    if(event.getRawSlot() == 45 && clicked.getType() == Material.STAINED_GLASS_PANE){
                        window.scrollPagePrev();
                        return;
                    }

                    //this is the case in all windows
                    if(event.getRawSlot() == 53 && clicked.getType() == Material.STAINED_GLASS_PANE){
                        window.scrollPageNext();
                        return;
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
                                    ShopObject shop = plugin.getShopHandler().getShop(loc);

                                    if(shop != null){
                                        shop.teleportPlayer(player);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    else if(window instanceof SearchWindow){
                        if(window.hasPrevWindow()){
                            plugin.getGuiHandler().setWindow(player, window.prevWindow);
                            return;
                        }
                    }
                }
            }
            //System.out.println("Inventory slot: "+event.getSlot());
            //System.out.println("Inventory raw slot: "+event.getRawSlot());
        }
    }
}
