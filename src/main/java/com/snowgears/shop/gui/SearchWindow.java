package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SearchWindow extends ShopGuiWindow {

    //TODO make this inventory of type anvil and do an acition on pressing enter after searching
    public SearchWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Search");
        initInvContents();
    }

    @Override
    protected void initInvContents(){

    }

    @Override
    protected void makeMenuBarUpper(){

    }

    @Override
    protected void makeMenuBarLower(){

    }
}

