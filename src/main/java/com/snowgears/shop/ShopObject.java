package com.snowgears.shop;

import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.pl3x.bukkit.chatapi.ComponentSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ShopObject {

    private Location signLocation;
    private Location chestLocation;
    private UUID owner;
    private ItemStack item;
    private ItemStack barterItem;
    private ItemStack gambleItem;
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
        display = new Display(this);
        signLines = ShopMessage.getSignLines(this);

        if(signLocation != null) {
            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signLocation.getBlock().getState().getData();
            chestLocation = signLocation.getBlock().getRelative(sign.getAttachedFace()).getLocation();

            this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
        }
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Inventory getInventory() {
        Block chestBlock = chestLocation.getBlock();
        if(chestBlock.getType() == Material.ENDER_CHEST) {
            return Shop.getPlugin().getEnderChestHandler().getInventory(this.getOwnerPlayer());
        }
        else if(chestBlock.getState() instanceof InventoryHolder){
            return ((InventoryHolder)(chestBlock.getState())).getInventory();
        }
        return null;
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
        //this.display.spawn();
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
        //this.display.spawn();
    }

    public ItemStack getGambleItemStack(){
        return gambleItem;
    }

    public void shuffleGambleItem(){
        if(this.type == ShopType.GAMBLE) {
            this.setItemStack(gambleItem);
            final DisplayType initialDisplayType = this.getDisplay().getType();
            this.getDisplay().setType(DisplayType.ITEM);
            this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);

            new BukkitRunnable() {
                @Override
                public void run() {
                    setItemStack(Shop.getPlugin().getGambleDisplayItem());
                    if(initialDisplayType == null)
                        display.setType(Shop.getPlugin().getDisplayType());
                    else
                        display.setType(initialDisplayType);
                }
            }.runTaskLater(Shop.getPlugin(), 20);
        }
    }

    public void setOwner(UUID newOwner){
        this.owner = newOwner;
    }

    public int getStock(){
        switch (type){
            case SELL:
                return InventoryUtils.getAmount(this.getInventory(), this.getItemStack()) / this.getAmount();
            case BUY:
                double funds = EconomyUtils.getFunds(this.getOwnerPlayer(), this.getInventory());
                if(this.getPrice() == 0)
                    return Integer.MAX_VALUE;
                return (int)(funds / this.getPrice());
            case BARTER:
                return InventoryUtils.getAmount(this.getInventory(), this.getItemStack()) / this.getAmount();
        }
        return 0;
    }

    public Display getDisplay() {
        return display;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price){
        this.price = price;
    }

    public String getPriceString() {
        return Shop.getPlugin().getPriceString(this.price, false);
    }

    public String getPricePerItemString() {
        double pricePer = this.getPrice() / this.getAmount();
        return Shop.getPlugin().getPriceString(pricePer, true);
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

    public void setAmount(int amount){
        this.amount = amount;
    }

    public boolean isAdminShop() {
        if(this.type == ShopType.GAMBLE)
            return true;
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

    public void teleportPlayer(Player player){
        if(player == null)
            return;

        BlockFace face = this.getFacing();
        Location loc = this.getSignLocation().getBlock().getRelative(face).getLocation().add(0.5, 0, 0.5);
        loc.setYaw(UtilMethods.faceToYaw(face.getOppositeFace()));
        loc.setPitch(25.0f);

        player.teleport(loc);
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
        if(message == null)
            return;

        String[] parts = message.split("(?=&[0-9A-FK-ORa-fk-or])");
        TextComponent fancyMessage = new TextComponent("");

        for(String part : parts){
            ComponentBuilder builder = new ComponentBuilder("");
            org.bukkit.ChatColor cc = UtilMethods.getChatColor(part);
            if(cc != null)
                part = part.substring(2, part.length());
            boolean barterItem = false;
            if(part.contains("[barter item]"))
                barterItem = true;
            part = ShopMessage.formatMessage(part, this, player, false);
            part = ChatColor.stripColor(part);
            builder.append(part);
            if(cc != null) {
                builder.color(ChatColor.valueOf(cc.name()));
            }

            if(part.startsWith("[")) {
                String itemJson;
                if (barterItem) {
                    itemJson = ReflectionUtil.convertItemStackToJson(this.getBarterItemStack());
                } else {
                    itemJson = ReflectionUtil.convertItemStackToJson(this.getItemStack());
                }
                // Prepare a BaseComponent array with the itemJson as a text component
                BaseComponent[] hoverEventComponents = new BaseComponent[]{ new TextComponent(itemJson) }; // The only element of the hover events basecomponents is the item json
                HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents);

                builder.event(event);
            }

            for(BaseComponent b : builder.create()) {
                fancyMessage.addExtra(b);
            }
        }

        //use special ComponentSender for MC 1.8+ and regular way for MC 1.7
        try {
            if (Material.AIR != Material.ARMOR_STAND) {
                ComponentSender.sendMessage(player, fancyMessage);
            }
        } catch (NoSuchFieldError e) {
            player.sendMessage(fancyMessage.getText());
        }
    }
}
