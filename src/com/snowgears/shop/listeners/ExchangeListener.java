
package com.snowgears.shop.listeners;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.utils.InventoryUtils;
import com.snowgears.shop.utils.ShopMessage;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


public class ExchangeListener implements Listener {

    private Shop plugin = Shop.getPlugin();
//    private Logger exchangeLogger;

    public ExchangeListener(Shop instance) {
        plugin = instance;
//        initializeLogger();
    }

    //TODO will need to update ender chest contents at the end of every transaction involving an ender chest

    @EventHandler
    public void onShopSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        //player clicked the sign of a shop
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.WALL_SIGN) {
                ShopObject shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
                if (shop == null || !shop.isInitialized())
                    return;

                //player did not click their own shop
                if ((!shop.getOwnerName().equals(player.getName())) || shop.isAdminShop()) {
                    if (shop.getType() == ShopType.BUY) {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.buying") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        playerSellToShop(player, shop);
                    } else if (shop.getType() == ShopType.SELL) {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.selling") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        playerBuyFromShop(player, shop);
                    } else {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.barter") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        playerBarterWithShop(player, shop);
                    }
                } else {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "useOwnShop", shop, player));
                }
                event.setCancelled(true);
            }
        }
    }

    //TODO TRY TO REWRITE THIS USING THE EconomyUtils METHODS LATER ON TO CLEAN THIS UP

    public boolean playerBuyFromShop(Player player, ShopObject shop) {

        //Server is using a virtual economy
        if (plugin.useVault()) {
            //remove items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
                //revert back if shop does not have enough items in stock
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return false;
                }
            }

            //remove money from player
            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, shop.getPrice());
            if (!response.transactionSuccess()) {
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                return false;
            }

            //pay that money to the shop owner
            if (!shop.isAdminShop()) {
                plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice());
            }

            //add items to buyer's inventory
            int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player);
            //revert back if player does not have enough room in inventory
            if (buyerOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - buyerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert, player); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    plugin.getEconomy().withdrawPlayer(shop.getOwnerPlayer(), shop.getPrice()); //take money back from shop owner
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
                }
                plugin.getEconomy().depositPlayer(player, shop.getPrice()); //give buyer their money back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return false;
            }
        }
        //Server is using a physical economy
        else {
            ItemStack itemCost = plugin.getItemCurrency();
            itemCost.setAmount((int) shop.getPrice());

            //remove items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
                //revert back if shop does not have enough items in stock
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return false;
                }
            }

            //remove money items from player
            int playerMoneyOverflow = InventoryUtils.removeItem(player.getInventory(), itemCost, player);
            //revert back if player does not have enough money items in inventory
            if (playerMoneyOverflow > 0) {
                ItemStack revert = itemCost.clone();
                revert.setAmount(revert.getAmount() - playerMoneyOverflow);
                InventoryUtils.addItem(player.getInventory(), revert, player); //return underflow money items back to players inventory
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return false;
            }

            //add those money items to the shop
            if (!shop.isAdminShop()) {
                int shopMoneyOverflow = InventoryUtils.addItem(shop.getInventory(), itemCost, shop.getOwnerPlayer());
                //revert back if shop does not have enough room for money items
                if (shopMoneyOverflow > 0) {
                    ItemStack revert = itemCost.clone();
                    revert.setAmount(revert.getAmount() - shopMoneyOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //remove underflow money items from shop inventory
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
                    InventoryUtils.addItem(player.getInventory(), itemCost, player); //return money items back to buyer
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return false;
                }
            }

            //add item to buyer's inventory
            int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player);
            //revert back if player does not have enough room in inventory
            if (buyerOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - buyerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert, player); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    InventoryUtils.removeItem(shop.getInventory(), itemCost, shop.getOwnerPlayer()); //take money items back from shop
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
                }
                InventoryUtils.addItem(player.getInventory(), itemCost, player); //give buyer their money items back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return false;
            }
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
        return true;
    }

    public boolean playerSellToShop(Player player, ShopObject shop) {
        if (plugin.useVault()) {

            //remove money from shop owner
            if (!shop.isAdminShop()) {
                EconomyResponse response = plugin.getEconomy().withdrawPlayer(shop.getOwnerPlayer(), shop.getPrice());
                if (!response.transactionSuccess()) {
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return false;
                }
            }

            //remove items from player
            int playerItemOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getItemStack(), player);
            //revert back if player does not have enough items in inventory
            if (playerItemOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - playerItemOverflow);
                InventoryUtils.addItem(player.getInventory(), revert, player); //return underflow items back to players inventory
                if (!shop.isAdminShop())
                    plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice()); //return money to shop owner

                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return false;
            }

            //add those items to the shop
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
                //revert back if shop does not have enough room for money items
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //remove underflow items from shop inventory
                    plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice()); //return money to shop owner
                    InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player); //return items back to seller
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return false;
                }
            }

            //add money to seller (player)
            plugin.getEconomy().depositPlayer(player, shop.getPrice());
        } else {
            ItemStack itemCost = plugin.getItemCurrency();
            itemCost.setAmount((int) shop.getPrice());

            //remove money items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopMoneyOverflow = InventoryUtils.removeItem(shop.getInventory(), itemCost, shop.getOwnerPlayer());
                //revert back if shop does not have enough money items in stock
                if (shopMoneyOverflow > 0) {
                    ItemStack revert = itemCost.clone();
                    revert.setAmount(revert.getAmount() - shopMoneyOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return false;
                }
            }

            //remove items from player
            int playerItemOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getItemStack(), player);
            //revert back if player does not have enough money items in inventory
            if (playerItemOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - playerItemOverflow);
                InventoryUtils.addItem(player.getInventory(), revert, player); //return underflow items back to players inventory
                InventoryUtils.addItem(shop.getInventory(), itemCost, shop.getOwnerPlayer()); //return money items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return false;
            }

            //add those items to the shop
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
                //revert back if shop does not have enough room for money items
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //remove underflow items from shop inventory
                    InventoryUtils.addItem(shop.getInventory(), itemCost, shop.getOwnerPlayer()); //return money items back to shops inventory
                    InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player); //return items back to seller
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return false;
                }
            }

            //add money items to player's inventory
            int sellerOverflow = InventoryUtils.addItem(player.getInventory(), itemCost, player);
            //revert back if player does not have enough room in inventory
            if (sellerOverflow > 0) {
                ItemStack revert = itemCost.clone();
                revert.setAmount(revert.getAmount() - sellerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert, player); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //take items back from shop
                    InventoryUtils.addItem(shop.getInventory(), itemCost, shop.getOwnerPlayer()); //return money items back to shops inventory
                }
                InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player); //give seller their items back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return false;
            }
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
        return true;
    }

    public boolean playerBarterWithShop(Player player, ShopObject shop) {
        //the shop is giving away its ItemStack
        //the shop is receiving its BarterItemStack

        //remove itemstack from shop's inventory
        if (!shop.isAdminShop()) {
            int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
            //revert back if shop does not have enough items in stock
            if (shopOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - shopOverflow);
                InventoryUtils.addItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //return underflow items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                return false;
            }
        }

        //remove barteritemstack from player
        int playerOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getBarterItemStack(), player);
        //revert back if player does not have enough money items in inventory
        if (playerOverflow > 0) {
            ItemStack revert = shop.getBarterItemStack().clone();
            revert.setAmount(revert.getAmount() - playerOverflow);
            InventoryUtils.addItem(player.getInventory(), revert, player); //return underflow items back to players inventory
            InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
            player.updateInventory();
            return false;
        }

        //add barteritemstack to the shop
        if (!shop.isAdminShop()) {
            int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getBarterItemStack(), shop.getOwnerPlayer());
            //revert back if shop does not have enough room for money items
            if (shopOverflow > 0) {
                ItemStack revert = shop.getBarterItemStack().clone();
                revert.setAmount(revert.getAmount() - shopOverflow);
                InventoryUtils.removeItem(shop.getInventory(), revert, shop.getOwnerPlayer()); //remove underflow items from shop inventory
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
                InventoryUtils.addItem(player.getInventory(), shop.getBarterItemStack(), player); //return barter items back to buyer
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                player.updateInventory();
                return false;
            }
        }

        //add item to buyer's inventory
        int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player);
        //revert back if player does not have enough room in inventory
        if (buyerOverflow > 0) {
            ItemStack revert = shop.getItemStack().clone();
            revert.setAmount(revert.getAmount() - buyerOverflow);
            InventoryUtils.removeItem(player.getInventory(), revert, player); //remove underflow items from players inventory
            if (!shop.isAdminShop()) {
                InventoryUtils.removeItem(shop.getInventory(), shop.getBarterItemStack(), shop.getOwnerPlayer()); //take barteritem back from shop
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer()); //return items back to shops inventory
            }
            InventoryUtils.addItem(player.getInventory(), shop.getBarterItemStack(), player); //give buyer their money items back
            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
            player.updateInventory();
            return false;
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
        return true;
    }

    private void sendExchangeMessages(ShopObject shop, Player player) {
        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "user", shop, player));

        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if ((owner != null) && (!shop.isAdminShop()))
            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "owner", shop, player));
    }
}