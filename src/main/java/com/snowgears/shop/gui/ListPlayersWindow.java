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

public class ListPlayersWindow extends ShopGuiWindow {

    public ListPlayersWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Shop Player Menu");
        initInvContents();
    }

    @Override
    protected void initInvContents(){
        makeMenuBarUpper();
        makeMenuBarLower();

        ItemStack icon;
        for(UUID owner : Shop.getPlugin().getShopHandler().getShopOwners()){

            List<String> lore = new ArrayList<>();
            lore.add("Shops: "+Shop.getPlugin().getShopHandler().getShops(owner).size());
            lore.add("UUID: "+ owner.toString());

            String name;
            if(Shop.getPlugin().getShopHandler().getAdminUUID().equals(owner)) {
                name = "Admin";
                icon = new ItemStack(Material.CHEST);

                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lore);

                icon.setItemMeta(meta);
            }
            else {
                name = Bukkit.getOfflinePlayer(owner).getName();
                icon = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);

                SkullMeta meta = (SkullMeta) icon.getItemMeta();
                meta.setOwner(name);
                meta.setDisplayName(name);
                meta.setLore(lore);

                icon.setItemMeta(meta);
            }
            this.addIcon(icon);
        }
    }

    @Override
    protected void makeMenuBarUpper(){

    }

    @Override
    protected void makeMenuBarLower(){

    }
}

