package com.snowgears.shop;

import com.snowgears.shop.utils.EconomyUtils;
import com.snowgears.shop.utils.InventoryUtils;
import com.snowgears.shop.utils.ShopMessage;
import com.snowgears.shop.utils.UtilMethods;
import mkremins.fanciful.FancyMessage;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

import java.text.DecimalFormat;
import java.util.Map;
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
        if (item.getType().getMaxDurability() > 0)
            item.setDurability((short) 0); //set item to full durability
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
        if (barterItem.getType().getMaxDurability() > 0)
            barterItem.setDurability((short) 0); //set item to full durability
        this.display.spawn();
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

        String message = ShopMessage.getMessage(this.getType().toString(), "descriptionItem", this, player);

        String[] parts = message.split("[\\[\\]]");

        String toSend = new FancyMessage(parts[0])
                .then(parts[1])
                    .itemTooltip(this.getItemStack())
                .then(parts[2])
                .toJSONString();

        player.sendRawMessage(toSend);


        //TODO instead of this make a clickable chat string in the in-game text
        printItemStackToPlayer(item, this.getAmount(), player);

        if (this.getType() == ShopType.BARTER) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "Item(s) you trade to this shop:");
            printItemStackToPlayer(barterItem, (int) this.getPrice(), player);
        }
        player.sendMessage("");


        if(price != 0) {
            double pricePer = this.getPrice() / this.getAmount();
            String pricePerString;
            if (Shop.getPlugin().useVault())
                pricePerString = Shop.getPlugin().getVaultCurrencySymbol() + new DecimalFormat("#.00").format(pricePer).toString();
            else
                pricePerString = new DecimalFormat("#.##").format(pricePer).toString() + " " + Shop.getPlugin().getItemCurrencyName();

            if (this.getType() == ShopType.SELL) {
                player.sendMessage(ChatColor.GREEN + "You can buy " + ChatColor.WHITE + this.getAmount() + ChatColor.GREEN + " " + item.getType().name().replace("_", " ").toLowerCase() + "(s) from this shop for " + ChatColor.WHITE + this.getPriceString() + ".");
                player.sendMessage(ChatColor.GRAY + "That is " + pricePerString + " per " + item.getType().name().replace("_", " ").toLowerCase() + ".");
            } else if (this.getType() == ShopType.BUY) {
                player.sendMessage(ChatColor.GREEN + "You can sell " + ChatColor.WHITE + this.getAmount() + ChatColor.GREEN + " " + item.getType().name().replace("_", " ").toLowerCase() + "(s) to this shop for " + ChatColor.WHITE + this.getPriceString() + ".");
                player.sendMessage(ChatColor.GRAY + "That is " + pricePerString + " per " + item.getType().name().replace("_", " ").toLowerCase() + ".");
            } else {
                String amountPerString = new DecimalFormat("#.##").format(pricePer).toString();
                player.sendMessage(ChatColor.GREEN + "You can barter " + ChatColor.WHITE + (int) this.getPrice() + ChatColor.GREEN + " of your " + ChatColor.WHITE + "" + barterItem.getType().name().replace("_", " ").toLowerCase() + "(s)"
                        + ChatColor.GREEN + " to this shop for " + ChatColor.WHITE + this.getAmount() + " " + item.getType().name().replace("_", " ").toLowerCase() + "(s)" + ChatColor.WHITE + ".");
                player.sendMessage(ChatColor.GRAY + "That is " + amountPerString + " " + barterItem.getType().name().replace("_", " ").toLowerCase() + "(s) per " + item.getType().name().replace("_", " ").toLowerCase() + ".");
            }
            player.sendMessage("");
        }

        int stock = this.getStock();
        String stacks = ""+stock;
        ChatColor cc = ChatColor.GREEN;
        if (stock <= 0)
            cc = ChatColor.RED;
        if(stock == Integer.MAX_VALUE || isAdminShop()) {
            stacks = "unlimited";
            cc = ChatColor.GREEN;
        }
        player.sendMessage(ChatColor.GRAY + "There are currently " + cc + stacks + ChatColor.GRAY + " stacks in stock.");

        return;
    }

    private void printItemStackToPlayer(ItemStack item, int amount, Player player) {
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta.getDisplayName() != null)
            player.sendMessage(ChatColor.WHITE + itemMeta.getDisplayName() + " (" + amount + ")");
        else
            player.sendMessage(ChatColor.WHITE + UtilMethods.capitalize(item.getType().name().replace("_", " ").toLowerCase()) + " (" + amount + ")");

        if (itemMeta.getLore() != null) {
            for (String s : itemMeta.getLore()) {
                player.sendMessage(s);
            }
        }
        //TODO also get durability of stained_glass, stained_glass_pane, stained_clay,... etc
        //TODO then print that color as well
        if(itemMeta instanceof LeatherArmorMeta) {
            Color leatherColor = ((LeatherArmorMeta)itemMeta).getColor();
            if(leatherColor != null)
                player.sendMessage(ChatColor.GRAY+"Color: "+DyeColor.getByColor(leatherColor).name());
        }

        Map<Enchantment, Integer> itemEnchantments = item.getEnchantments();

        if (!itemEnchantments.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Enchantments:");
            for (Map.Entry<Enchantment, Integer> s : itemEnchantments.entrySet()) {
                player.sendMessage(ChatColor.YELLOW + UtilMethods.capitalize(s.getKey().getName().replace("_", " ").toLowerCase()) + ChatColor.WHITE + " level: " + s.getValue());
            }
            player.sendMessage("");
        }
        if (item.getType() == Material.POTION) {
            Potion potion = Potion.fromItemStack(item);
            player.sendMessage(ChatColor.AQUA + "Potion Type: " + ChatColor.WHITE + UtilMethods.capitalize(potion.getType().name().replace("_", " ").toLowerCase()) + ChatColor.GRAY + ", Level " + potion.getLevel());
            player.sendMessage(ChatColor.AQUA + "Potion Effects: ");
            for (PotionEffect effect : potion.getEffects()) {
                player.sendMessage(ChatColor.WHITE + "   - " + ChatColor.LIGHT_PURPLE + UtilMethods.capitalize(effect.getType().getName().replace("_", " ").toLowerCase()) + effect.getAmplifier() + ChatColor.GRAY + " (" + UtilMethods.convertDurationToString(effect.getDuration()) + ")");
            }
        }
    }

    @Override
    public String toString() {
        return owner + "." + item.getType().toString();
    }
}
