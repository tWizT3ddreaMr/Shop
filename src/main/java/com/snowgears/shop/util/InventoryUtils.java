package com.snowgears.shop.util;


import com.snowgears.shop.Shop;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;

public class InventoryUtils {

    //removes itemstack from inventory
    //returns the amount of items it could not remove
    public static int removeItem(Inventory inventory, ItemStack itemStack, OfflinePlayer inventoryOwner) {
        if(inventory == null)
            return itemStack.getAmount();
        if (itemStack.getAmount() <= 0)
            return 0;
        ItemStack[] contents = inventory.getContents();
        int amount = itemStack.getAmount();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null) {
                if (itemstacksAreSimilar(is, itemStack)) {
                    if (is.getAmount() > amount) {
                        contents[i].setAmount(is.getAmount() - amount);
                        inventory.setContents(contents);
                        return 0;
                    } else if (is.getAmount() == amount) {
                        contents[i].setType(Material.AIR);
                        inventory.setContents(contents);
                        return 0;
                    } else {
                        amount -= is.getAmount();
                        contents[i].setType(Material.AIR);
                    }
                }
            }
        }
        inventory.setContents(contents);
        if(inventory.getType() == InventoryType.ENDER_CHEST){
            Shop.getPlugin().getEnderChestHandler().updateInventory(inventoryOwner, inventory);
        }
        return amount;
    }

    //takes an ItemStack and splits it up into multiple ItemStacks with correct stack sizes
    //then adds those items to the given inventory
    public static int addItem(Inventory inventory, ItemStack itemStack, OfflinePlayer inventoryOwner) {
        if(inventory == null)
            return itemStack.getAmount();
        if (itemStack.getAmount() <= 0)
            return 0;
        ArrayList<ItemStack> itemStacksAdding = new ArrayList<ItemStack>();

        //break up the itemstack into multiple ItemStacks with correct stack size
        int fullStacks = itemStack.getAmount() / itemStack.getMaxStackSize();
        int partialStack = itemStack.getAmount() % itemStack.getMaxStackSize();
        for (int i = 0; i < fullStacks; i++) {
            ItemStack is = itemStack.clone();
            is.setAmount(is.getMaxStackSize());
            itemStacksAdding.add(is);
        }
        ItemStack is = itemStack.clone();
        is.setAmount(partialStack);
        if (partialStack > 0)
            itemStacksAdding.add(is);

        //try adding all items from itemStacksAdding and return number of ones you couldnt add
        int amount = 0;
        for (ItemStack addItem : itemStacksAdding) {
            HashMap<Integer, ItemStack> noAdd = inventory.addItem(addItem);
            amount += noAdd.size();
        }
        if(inventory.getType() == InventoryType.ENDER_CHEST){
            Shop.getPlugin().getEnderChestHandler().updateInventory(inventoryOwner, inventory);
        }
        return amount;
    }

    public static boolean hasRoom(Inventory inventory, ItemStack itemStack, OfflinePlayer inventoryOwner) {
        if (inventory == null)
            return false;
        if (itemStack.getAmount() <= 0)
            return true;

        int overflow = addItem(inventory, itemStack, inventoryOwner);

        //revert back if inventory cannot hold all of the items
        if (overflow > 0) {
            ItemStack revert = itemStack.clone();
            revert.setAmount(revert.getAmount() - overflow);
            InventoryUtils.removeItem(inventory, revert, inventoryOwner);
            return false;
        }
        removeItem(inventory, itemStack, inventoryOwner);
        return true;
    }

    //gets the amount of items in inventory
    public static int getAmount(Inventory inventory, ItemStack itemStack){
        if(inventory == null)
            return 0;
        ItemStack[] contents = inventory.getContents();
        int amount = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null) {
                if (itemstacksAreSimilar(is, itemStack)) {
                    amount += is.getAmount();
                }
            }
        }
        return amount;
    }

    public static boolean itemstacksAreSimilar(ItemStack i1, ItemStack i2){
        if(i1 == null || i2 == null)
            return false;
        if(i1.getType() == i2.getType()){
            ItemMeta meta1 = i1.getItemMeta();
            ItemMeta meta2 = i2.getItemMeta();

            if(meta1.hasDisplayName() && meta2.hasDisplayName()){
                if(!meta1.getDisplayName().equals(meta2.getDisplayName()))
                    return false;
            }
            if(meta1.hasLore() && meta2.hasLore()){
                if(!meta1.getLore().equals(meta2.getLore()))
                    return false;
            }
            if(!meta1.getItemFlags().equals(meta2.getItemFlags()))
                return false;
            if(!meta1.getEnchants().equals(meta2.getEnchants()))
                return false;

            if(meta1 instanceof EnchantmentStorageMeta && meta2 instanceof EnchantmentStorageMeta){
                if(!((EnchantmentStorageMeta)meta1).getStoredEnchants().equals(((EnchantmentStorageMeta)meta2).getStoredEnchants()))
                    return false;
            }

            if (i1.getEnchantments().equals(i2.getEnchantments())) {
                //only have the option to ignore durability if the item can be damaged
                if(i1.getType().getMaxDurability() != 0) {
                    if ((!Shop.getPlugin().checkItemDurability() || (Shop.getPlugin().checkItemDurability() && i1.getDurability() == i2.getDurability()))) {
                        return true;
                    }
                }
                else{
                    if (i1.getDurability() == i2.getDurability()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
