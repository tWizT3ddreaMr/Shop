package com.snowgears.shop;

import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopGUI {

    public enum ShopGUIType{
        LIST_OWN;
    }

    private final Inventory inventory;
    private final ShopGUIType type;
    private UUID player;

    //TODO also block interacting with inventory in any way except selection with custom listener


    public ShopGUI(Player player, ShopGUIType type){
        String name = "Your Shop List";
        int size = calculateInvSize(player);
        this.inventory = Bukkit.createInventory(null, size, name);
        this.type = type;
        this.player = player.getUniqueId();

        initInvContents();
        open();
    }

    public void open(){
        Player p = Bukkit.getPlayer(player);
        if(p != null){
           p.openInventory(inventory);
        }
    }

    public void close(){
        Player p = Bukkit.getPlayer(player);
        if(p != null){
            p.closeInventory();
        }
    }

    private void initInvContents(){
        List<ShopObject> shops = Shop.getPlugin().getShopHandler().getShops(player);

        //TODO sort by ShopType and break up inventory into sections by type
        ItemStack icon;
        for(ShopObject shop : shops){

            List<String> lore = new ArrayList<>();

            if(shop.getStock() != 0) {
                icon = new ItemStack(shop.getChestLocation().getBlock().getType());
            }
            else {
                icon = new ItemStack(Material.BARRIER);
            }

            lore.add("Stock: "+shop.getStock());
            lore.add("Location: "+UtilMethods.getCleanLocation(shop.getChestLocation(), true));

            //TODO encorporate gambling shops and bartering shops

            String name = UtilMethods.getItemName(shop.getItemStack()) + " (x" + shop.getAmount() + ")";
            ItemMeta iconMeta = icon.getItemMeta();
            iconMeta.setDisplayName(name);
            iconMeta.setLore(lore);

            icon.setItemMeta(iconMeta);

            inventory.addItem(icon);
        }
        //TODO set materials of items in GUI to be the chest types
        //chest, trapped chest, ender chest, shulker box, etc...
        //names will be what the shop sells
        //descrription will be how many transactions are left, world, and x,y,z location
        //if shop is out of stock, item type of icon will be the RED X
    }


    private int calculateInvSize(Player player){
        int shopAmt = Shop.getPlugin().getShopHandler().getNumberOfShops(player);

        if(shopAmt == 0)
            return 9;

        double dec = Math.abs((double)shopAmt/9);
        int ceil = (int)(Math.ceil(dec));
        return 9 * ceil;
    }

    // Getters for "inventory" and "type" fields
}
