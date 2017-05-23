package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.util.ShopTypeComparator;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ListShopsWindow extends ShopGuiWindow {

    private UUID playerToList;

    public ListShopsWindow(UUID player, UUID playerToList){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Shop List Menu");
        this.playerToList = playerToList;
        initInvContents();
        //this.open(); //TODO delete this after testing
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();

        makeMenuBarUpper();
        makeMenuBarLower();

        List<ShopObject> shops = Shop.getPlugin().getShopHandler().getShops(playerToList);
        Collections.sort(shops, new ShopTypeComparator());

        //System.out.println(player.toString()+" number of shops "+shops.size());

        //TODO break up inventory into sections by type (by default. More sorting options to come)
        ItemStack icon;
        for (ShopObject shop : shops) {

            List<String> lore = new ArrayList<>();

            //if (shop.getStock() != 0) {
                icon = new ItemStack(shop.getItemStack());
            //} else {
            //    icon = new ItemStack(Material.BARRIER);
            //}

            lore.add("Stock: " + shop.getStock());
            lore.add("Location: " + UtilMethods.getCleanLocation(shop.getChestLocation(), true));

            //TODO encorporate gambling shops and bartering shops

            String name = UtilMethods.getItemName(shop.getItemStack()) + " (x" + shop.getAmount() + ")";
            ItemMeta iconMeta = icon.getItemMeta();
            iconMeta.setDisplayName(name);
            iconMeta.setLore(lore);

            icon.setItemMeta(iconMeta);

            this.addIcon(icon);
        }
    }

    @Override
    protected void makeMenuBarUpper(){
        super.makeMenuBarUpper();
    }

    @Override
    protected void makeMenuBarLower(){
        super.makeMenuBarLower();
    }
}

