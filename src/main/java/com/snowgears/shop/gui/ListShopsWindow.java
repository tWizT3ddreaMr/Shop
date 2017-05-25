package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.util.ShopTypeComparator;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ListShopsWindow extends ShopGuiWindow {

    private UUID playerToList;

    public ListShopsWindow(UUID player, UUID playerToList){
        super(player);

        String name;
        if(Shop.getPlugin().getShopHandler().getAdminUUID().equals(playerToList))
            name = "Admin";
        else
            name = Bukkit.getOfflinePlayer(playerToList).getName();

        this.page = Bukkit.createInventory(null, INV_SIZE, name);
        this.playerToList = playerToList;
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        List<ShopObject> shops = Shop.getPlugin().getShopHandler().getShops(playerToList);
        Collections.sort(shops, new ShopTypeComparator());

        //System.out.println(player.toString()+" number of shops "+shops.size());

        //TODO break up inventory into sections by type (by default. More sorting options to come)

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i=startIndex; i< shops.size(); i++) {
            ShopObject shop = shops.get(i);
            icon = createIcon(shop);

            if(!this.addIcon(icon)){
                added = false;
                break;
            }
        }

        if(added){
            page.setItem(53, null);
        }
        else{
            page.setItem(53, this.getNextPageIcon());
        }
    }

    private ItemStack createIcon(ShopObject shop){
        List<String> lore = new ArrayList<>();

        //if (shop.getStock() != 0) {
        ItemStack icon = shop.getItemStack().clone();
        icon.setAmount(1);

        //} else {
        //    icon = new ItemStack(Material.BARRIER);
        //}

        lore.add("Stock: " + shop.getStock());
        lore.add("Location: " + UtilMethods.getCleanLocation(shop.getSignLocation(), true));

        //TODO encorporate gambling shops and bartering shops

        String name = UtilMethods.getItemName(shop.getItemStack()) + " (x" + shop.getAmount() + ")";
        ItemMeta iconMeta = icon.getItemMeta();
        iconMeta.setDisplayName(name);
        iconMeta.setLore(lore);

        icon.setItemMeta(iconMeta);

        return icon;
    }

    @Override
    protected void makeMenuBarUpper(){
        super.makeMenuBarUpper();

        ItemStack searchIcon = new ItemStack(Material.COMPASS);
        ItemMeta meta = searchIcon.getItemMeta();
        meta.setDisplayName("Search");
        searchIcon.setItemMeta(meta);

        page.setItem(8, searchIcon);
    }

    @Override
    protected void makeMenuBarLower(){
        super.makeMenuBarLower();
    }
}

