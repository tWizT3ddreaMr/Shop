package com.snowgears.shop;

import com.snowgears.shop.display.Display;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import mkremins.fanciful.FancyMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.UUID;

public class ShopObject {

    private Location signLocation;
    private Location chestLocation;
    private UUID owner;
    private ItemStack item;
    private ItemStack barterItem;
    private Display display;
    private double price;
    private int amount;
    private boolean isAdminShop;
    private ShopType type;
    private String[] signLines;

    public ShopObject(Location signLoc,
                      UUID player,
                      double pri,
                      int amt,
                      Boolean admin,
                      ShopType t) {
        signLocation = signLoc;
        owner = player;
        price = pri;
        amount = amt;
        isAdminShop = admin;
        type = t;
        item = null;
        signLines = ShopMessage.getSignLines(this);
        display = new Display(this);

        if(signLocation != null) {
            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signLocation.getBlock().getState().getData();
            chestLocation = signLocation.getBlock().getRelative(sign.getAttachedFace()).getLocation();
        }

    }

    public Location getSignLocation() {
        return signLocation;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Inventory getInventory() {
        if(chestLocation.getBlock().getType() == Material.ENDER_CHEST) {
            return Shop.getPlugin().getEnderChestHandler().getInventory(this.getOwnerPlayer());
        }
        return ((Chest) chestLocation.getBlock().getState()).getInventory();
    }

    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        if(this.isAdminShop())
            return "admin";
        if (Bukkit.getOfflinePlayer(this.owner) != null)
            return Bukkit.getOfflinePlayer(this.owner).getName();
        return ChatColor.RED + "CLOSED";
    }

    public OfflinePlayer getOwnerPlayer() {
        return Bukkit.getOfflinePlayer(this.owner);
    }

    public ItemStack getItemStack() {
        if (item != null) {
            ItemStack is = item.clone();
            is.setAmount(this.getAmount());
            return is;
        }
        return null;
    }

    public void setItemStack(ItemStack is) {
        this.item = is.clone();
        if(!Shop.getPlugin().checkItemDurability()) {
            if (item.getType().getMaxDurability() > 0)
                item.setDurability((short) 0); //set item to full durability
        }
        this.display.spawn();
    }

    public ItemStack getBarterItemStack() {
        if (barterItem != null) {
            ItemStack is = barterItem.clone();
            is.setAmount((int) this.getPrice());
            return is;
        }
        return null;
    }

    public void setBarterItemStack(ItemStack is) {
        this.barterItem = is.clone();
        if(!Shop.getPlugin().checkItemDurability()) {
            if (barterItem.getType().getMaxDurability() > 0)
                barterItem.setDurability((short) 0); //set item to full durability
        }
        this.display.spawn();
    }

    public void setOwner(UUID newOwner){
        this.owner = newOwner;
    }

    public int getStock(){
        switch (type){
            case SELL:
                return InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
            case BUY:
                double funds = EconomyUtils.getFunds(this.getOwnerPlayer(), this.getInventory());
                if(this.getPrice() == 0)
                    return Integer.MAX_VALUE;
                return (int)(funds / this.getPrice());
            case BARTER:
                return InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
        }
        return 0;
    }

    public Display getDisplay() {
        return display;
    }

    public double getPrice() {
        return price;
    }

    public String getPriceString() {
        if(price == 0){
            return ShopMessage.getFreePriceWord();
        }
        if (Shop.getPlugin().useVault())
            //$12.00
            return Shop.getPlugin().getVaultCurrencySymbol() + new DecimalFormat("0.00").format(price).toString();
        else
            //12 Emerald(s)
            return (int) price + " " + Shop.getPlugin().getItemCurrencyName();
    }

    public String getPricePerItemString() {
        double pricePer = this.getPrice() / this.getAmount();
        if(price == 0){
            return ShopMessage.getFreePriceWord();
        }
        if (Shop.getPlugin().useVault())
            //$12.00
            return Shop.getPlugin().getVaultCurrencySymbol() + new DecimalFormat("#.00").format(pricePer).toString();
        else
            //1.50 Dirt(s)
            return new DecimalFormat("#.##").format(pricePer).toString() + " " + Shop.getPlugin().getItemCurrencyName();
    }

    public int getItemDurabilityPercent(boolean barterItem){
        ItemStack item = this.getItemStack().clone();
        if(barterItem)
            item = this.getBarterItemStack().clone();

        if (item.getType().getMaxDurability() > 0) {
            double dur = ((double)(item.getType().getMaxDurability() - item.getDurability()) / (double)item.getType().getMaxDurability());
            return (int)(dur * 100);
        }
        return 100;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isAdminShop() {
        return isAdminShop;
    }

    public ShopType getType() {
        return type;
    }

    public void updateSign() {

        signLines = ShopMessage.getSignLines(this);

        Shop.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Shop.getPlugin(), new Runnable() {
            public void run() {

                Sign signBlock = (Sign) signLocation.getBlock().getState();

                String[] lines = signLines.clone();

                if (!isInitialized()) {
                    signBlock.setLine(0, ChatColor.RED + ChatColor.stripColor(lines[0]));
                    signBlock.setLine(1, ChatColor.RED + ChatColor.stripColor(lines[1]));
                    signBlock.setLine(2, ChatColor.RED + ChatColor.stripColor(lines[2]));
                    signBlock.setLine(3, ChatColor.RED + ChatColor.stripColor(lines[3]));
                } else {
                    signBlock.setLine(0, lines[0]);
                    signBlock.setLine(1, lines[1]);
                    signBlock.setLine(2, lines[2]);
                    signBlock.setLine(3, lines[3]);
                }

                signBlock.update(true);
            }
        }, 2L);
    }

    public void delete() {
        display.remove();

        Block b = this.getSignLocation().getBlock();
        if (b.getType() == Material.WALL_SIGN) {
            Sign signBlock = (Sign) b.getState();
            signBlock.setLine(0, "");
            signBlock.setLine(1, "");
            signBlock.setLine(2, "");
            signBlock.setLine(3, "");
            signBlock.update(true);
        }

        //finally remove the shop from the shop handler
        Shop.getPlugin().getShopHandler().removeShop(this);
    }

    public BlockFace getFacing(){
        if(signLocation.getBlock().getType() == Material.WALL_SIGN) {
            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signLocation.getBlock().getState().getData();
            return sign.getFacing();
        }
        return null;
    }

    public boolean isInitialized() {
        if (type == ShopType.BARTER)
            return (item != null && barterItem != null);
        else
            return (item != null);
    }

    public void printSalesInfo(Player player) {
        player.sendMessage("");

        String message = ShopMessage.getUnformattedMessage(this.getType().toString(), "descriptionItem");
        formatAndSendFancyMessage(message, player);

        if (this.getType() == ShopType.BARTER) {
            message = ShopMessage.getUnformattedMessage(this.getType().toString(), "descriptionBarterItem");
            formatAndSendFancyMessage(message, player);
        }
        player.sendMessage("");


        if(price != 0) {
            message = ShopMessage.getMessage(this.getType().toString(), "descriptionPrice", this, player);
            player.sendMessage(message);

            message = ShopMessage.getMessage(this.getType().toString(), "descriptionPricePerItem", this, player);
            player.sendMessage(message);
            player.sendMessage("");
        }

        if(this.isAdminShop()){
            message = ShopMessage.getMessage("description", "stockAdmin", this, player);
            player.sendMessage(message);
        }
        else {
            message = ShopMessage.getMessage("description", "stock", this, player);
            player.sendMessage(message);
        }

        return;
    }

    private void formatAndSendFancyMessage(String message, Player player){
        String[] parts = message.split("(?=&[0-9A-FK-ORa-fk-or])");
        FancyMessage fancyMessage = new FancyMessage("");

        for(String part : parts){
            ChatColor color = UtilMethods.getChatColor(part);
            if(color != null)
                part = part.substring(2, part.length());
            boolean barterItem = false;
            if(part.contains("[barter item]"))
                barterItem = true;
            part = ShopMessage.formatMessage(part, this, player, false);
            part = ChatColor.stripColor(part);
            fancyMessage.then(part);
            if(color != null)
                fancyMessage.color(color);

            if(part.startsWith("[")) {
                if (barterItem) {
                    fancyMessage.itemTooltip(this.getBarterItemStack());
                } else {
                    fancyMessage.itemTooltip(this.getItemStack());
                }
            }
        }
        fancyMessage.send(player);
    }
}
