package com.snowgears.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class HomeWindow extends ShopGuiWindow {

    public HomeWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Shop Menu");
        initInvContents();
    }

    @Override
    protected void initInvContents(){

        //put list own shops, toggle options, list players with shops, etc...

        ItemStack listShopsIcon = new ItemStack(Material.CHEST);
        ItemMeta im = listShopsIcon.getItemMeta();
        im.setDisplayName("Your Shops");
        listShopsIcon.setItemMeta(im);

        page.setItem(21, listShopsIcon);


        ItemStack listPlayersIcon = new ItemStack(Material.SKULL_ITEM, 1, (short)3); //player skull
        im = listPlayersIcon.getItemMeta();
        im.setDisplayName("Player Shops");
        listPlayersIcon.setItemMeta(im);

        page.setItem(22, listPlayersIcon);


        ItemStack settingsIcon = new ItemStack(Material.GOLD_AXE);
        im = settingsIcon.getItemMeta();
        im.setDisplayName("Settings");
        settingsIcon.setItemMeta(im);

        page.setItem(23, settingsIcon);
    }
}