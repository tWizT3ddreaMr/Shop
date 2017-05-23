package com.snowgears.shop.gui;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopGuiWindow {

    public enum GUIArea{
        TOP_BAR, BOTTOM_BAR, BODY;
    }

    protected int INV_SIZE = 54;
    protected Inventory page;
    protected ShopGuiWindow prevWindow;
    protected UUID player;
    private int currentSlot;

    public ShopGuiWindow(UUID player){
        this.player = player;
        page = null;
        prevWindow = null;
        currentSlot = 9;
    }

    //TODO openNextPage will be implemented through what button is clicked and prevPage will be set from there
    //TODO also set currentSlot to 9 again

    public boolean openPrevWindow(){
        if(prevWindow == null)
            return false;

        Player p = this.getPlayer();
        if(p != null){
            p.openInventory(prevWindow.getInventory());
            return true;
        }

        return false;
    }

    public void setPrevWindow(ShopGuiWindow prevWindow){
        this.prevWindow = prevWindow;
        page.setItem(45, new ItemStack(Material.STAINED_GLASS_PANE));
    }

    public boolean hasPrevWindow(){
        if(prevWindow == null)
            return false;
        return true;
    }

    //this method will add to the GUI with taking into account top and bottom menu bars
    protected boolean addIcon(ItemStack icon){

        //this page has been filled with icons
        if(currentSlot == 44)
            return false;

        page.setItem(currentSlot, icon);
        currentSlot++;

        return true;
    }

    public boolean open(){
        Player p = this.getPlayer();
        if(p != null){
            p.openInventory(this.page);
            return true;
        }
        return false;
    }

    public boolean close(){
        Player p = this.getPlayer();
        if(p != null){
            p.closeInventory();
            return true;
        }
        return false;
    }


    protected void initInvContents(){
        //override in subclasses
    }

    protected void makeMenuBarUpper(){
        //override in subclasses
    }

    protected void makeMenuBarLower(){
        //override in subclasses
    }

    public Player getPlayer(){
        return Bukkit.getPlayer(player);
    }

    public Inventory getInventory(){
        return this.page;
    }
}
