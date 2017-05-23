package com.snowgears.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HomeWindow extends ShopGuiWindow {

    public HomeWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Shop Home Menu");
        initInvContents();
    }

    @Override
    protected void initInvContents(){
        makeMenuBarUpper();
    }

    @Override
    protected void makeMenuBarUpper(){
        //put list own shops, toggle options, list players with shops, etc...

        ItemStack listShopsIcon = new ItemStack(Material.CHEST);
        page.setItem(0, listShopsIcon);

        ItemStack listPlayersIcon = new ItemStack(Material.SKULL_ITEM);
        page.setItem(1, listPlayersIcon);
    }
}
