package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class CommandsWindow extends ShopGuiWindow {

    public CommandsWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Commands");
        initInvContents();
    }

    @Override
    protected void initInvContents(){

        //TODO currency
        //TODO setCurrency (OP)
        //TODO setGamble (OP)
        //TODO item refresh (OP)
        //TODO refresh (OP)

        ItemStack currencyIcon = new ItemStack(Material.EMERALD);
        ItemMeta im = currencyIcon.getItemMeta();
        im.setDisplayName("Check Server Currency");
        currencyIcon.setItemMeta(im);

        page.setItem(10, currencyIcon);

        //list the operator commands if they have permission
        Player p = this.getPlayer();
        if(p != null){
            if ((Shop.getPlugin().usePerms() && p.hasPermission("shop.operator")) || p.isOp()) {

                ItemStack setCurrencyIcon = new ItemStack(Material.GLOWSTONE_DUST);
                im = setCurrencyIcon.getItemMeta();
                im.setDisplayName("Set Server Currency");
                setCurrencyIcon.setItemMeta(im);

                page.setItem(11, setCurrencyIcon);


                ItemStack setGambleIcon = Shop.getPlugin().getGambleDisplayItem();
                im = setGambleIcon.getItemMeta();
                im.setDisplayName("Set Gamble Display");
                setGambleIcon.setItemMeta(im);

                page.setItem(12, setGambleIcon);


                ItemStack refreshDisplaysIcon = new ItemStack(Material.BONE);
                im = refreshDisplaysIcon.getItemMeta();
                im.setDisplayName("Refresh Shop Displays");
                refreshDisplaysIcon.setItemMeta(im);

                page.setItem(13, refreshDisplaysIcon);


                ItemStack reloadIcon = new ItemStack(Material.WEB);
                im = reloadIcon.getItemMeta();
                im.setDisplayName("Reload Plugin");
                reloadIcon.setItemMeta(im);

                page.setItem(14, reloadIcon);

            }
        }
    }
}
