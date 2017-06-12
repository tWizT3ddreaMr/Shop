package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.util.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class PlayerSettingsWindow extends ShopGuiWindow {

    //TODO this window will call the player settings handler and set different variables in the associated player settings class
    public PlayerSettingsWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Your Settings");
        initInvContents();
    }

    @Override
    protected void initInvContents(){

        Player p = this.getPlayer();
        if(p != null) {

            ItemStack ownerNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS)) {
                ownerNotifyIcon = new ItemStack(Material.WOOL, 1, (short)5); //green wool
            }
            else{
                ownerNotifyIcon = new ItemStack(Material.WOOL, 1, (short)14); //red wool
            }
            ItemMeta im = ownerNotifyIcon.getItemMeta();
            im.setDisplayName("Sales Notifications");
            ownerNotifyIcon.setItemMeta(im);
            page.setItem(10, ownerNotifyIcon);


            ItemStack userNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.SALE_USER_NOTIFICATIONS)) {
                userNotifyIcon = new ItemStack(Material.WOOL, 1, (short)5); //green wool
            }
            else{
                userNotifyIcon = new ItemStack(Material.WOOL, 1, (short)14); //red wool
            }
            im = userNotifyIcon.getItemMeta();
            im.setDisplayName("User Notifications");
            userNotifyIcon.setItemMeta(im);
            page.setItem(11, userNotifyIcon);


            ItemStack stockNotifyIcon;
            if (Shop.getPlugin().getGuiHandler().getSettingsOption(p, PlayerSettings.Option.STOCK_NOTIFICATIONS)) {
                stockNotifyIcon = new ItemStack(Material.WOOL, 1, (short)5); //green wool
            }
            else{
                stockNotifyIcon = new ItemStack(Material.WOOL, 1, (short)14); //red wool
            }
            im = stockNotifyIcon.getItemMeta();
            im.setDisplayName("Stock Notifications");
            stockNotifyIcon.setItemMeta(im);
            page.setItem(12, stockNotifyIcon);
        }
    }

//    @Override
//    protected void makeMenuBarUpper(){
//
//    }
//
//    @Override
//    protected void makeMenuBarLower(){
//
//    }
}

