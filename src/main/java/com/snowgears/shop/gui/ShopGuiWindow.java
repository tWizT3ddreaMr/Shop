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

    //TODO FOR SORTING, HAVE AN ARRAY LIST OF SORT_OPTIONS HERE THAT YOU USE ALONG WITH PAGE INDEX TO GO TO NEXT PAGE
    //For example, sort by name_ascending, name_descending, keyword, number of stock, shop type, etc...
    protected int pageIndex;

    public ShopGuiWindow(UUID player){
        this.player = player;
        page = null;
        prevWindow = null;
        currentSlot = 9;
    }

    //TODO openNextPage will be implemented through what button is clicked and prevPage will be set from there
    //TODO also set currentSlot to 9 again

    public boolean scrollPageNext(){

        ItemStack nextPageIcon = page.getItem(53);

        if(nextPageIcon != null && nextPageIcon.getType() == Material.STAINED_GLASS_PANE){
            //set the previous scroll page
            page.setItem(45, new ItemStack(Material.STAINED_GLASS_PANE));

            this.pageIndex++;

            //TODO initMainContents again using page index and possibly remove the nextPageIcon in new page

            return true;
        }
        return false;
    }

    public boolean scrollPagePrev(){
        ItemStack nextPageIcon = page.getItem(45);

        if(nextPageIcon != null && nextPageIcon.getType() == Material.STAINED_GLASS_PANE){
            //set the next scroll page
            page.setItem(53, new ItemStack(Material.STAINED_GLASS_PANE));

            this.pageIndex--;

            //TODO initMainContents again using page index

            return true;
        }
        return false;
    }

    //TODO make paging (scrolling) and previous window different
    //previous window will be in top left always (BARRIER?), going back to another window
    //previous page will simply set the inventory to the next group of items to display within the current window

//    public void setPrevWindow(ShopGuiWindow prevWindow){
//        this.prevWindow = this;
//        page.setItem(45, new ItemStack(Material.STAINED_GLASS_PANE));
//    }

    public void setPrevWindow(ShopGuiWindow prevWindow){
        this.prevWindow = prevWindow;
        page.setItem(0, new ItemStack(Material.BARRIER));
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

    //TODO search will always be in top right and will search anything all items in page or in list
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
