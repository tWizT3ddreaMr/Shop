package com.snowgears.shop.gui;

import org.bukkit.Bukkit;

import java.util.UUID;

public class PlayerSettingsWindow extends ShopGuiWindow {

    //TODO this window will call the player settings handler and set different variables in the associated player settings class
    public PlayerSettingsWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Player Settings");
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

