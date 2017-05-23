package com.snowgears.shop.gui;

import org.bukkit.Bukkit;

import java.util.UUID;

public class OptionsWindow extends ShopGuiWindow {

    //TODO this will have options for teleporting


    public OptionsWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Options");
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

