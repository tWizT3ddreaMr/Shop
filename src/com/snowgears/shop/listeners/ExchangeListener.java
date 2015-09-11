package com.snowgears.shop.listeners;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.utils.InventoryUtils;
import com.snowgears.shop.utils.ShopMessage;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class ExchangeListener implements Listener {

    private Shop plugin = Shop.getPlugin();
//    private Logger exchangeLogger;

    public ExchangeListener(Shop instance) {
        plugin = instance;
//        initializeLogger();
    }


    @EventHandler
    public void onShopSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        //player clicked the sign of a shop
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.WALL_SIGN) {
                ShopObject shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
                if (shop == null)
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

    public void playerBuyFromShop(Player player, ShopObject shop) {
        //Server is using a virtual economy
        if (plugin.useVault()) {
            //remove items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack());
                //revert back if shop does not have enough items in stock
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return;
                }
            }

            //remove money from player
            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, shop.getPrice());
            if (response.transactionSuccess() == false) {
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                return;
            }

            //pay that money to the shop owner
            if (!shop.isAdminShop()) {
                plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice());
            }

            //add items to buyer's inventory
            int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack());
            //revert back if player does not have enough room in inventory
            if (buyerOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - buyerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    plugin.getEconomy().withdrawPlayer(shop.getOwnerPlayer(), shop.getPrice()); //take money back from shop owner
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
                }
                plugin.getEconomy().depositPlayer(player, shop.getPrice()); //give buyer their money back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return;
            }
        }
        //Server is using a physical economy
        else {
            ItemStack itemCost = plugin.getItemCurrency();
            itemCost.setAmount((int) shop.getPrice());

            //remove items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack());
                //revert back if shop does not have enough items in stock
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return;
                }
            }

            //remove money items from player
            int playerMoneyOverflow = InventoryUtils.removeItem(player.getInventory(), itemCost);
            //revert back if player does not have enough money items in inventory
            if (playerMoneyOverflow > 0) {
                ItemStack revert = itemCost.clone();
                revert.setAmount(revert.getAmount() - playerMoneyOverflow);
                InventoryUtils.addItem(player.getInventory(), revert); //return underflow money items back to players inventory
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return;
            }

            //add those money items to the shop
            if (!shop.isAdminShop()) {
                int shopMoneyOverflow = InventoryUtils.addItem(shop.getInventory(), itemCost);
                //revert back if shop does not have enough room for money items
                if (shopMoneyOverflow > 0) {
                    ItemStack revert = itemCost.clone();
                    revert.setAmount(revert.getAmount() - shopMoneyOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert); //remove underflow money items from shop inventory
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
                    InventoryUtils.addItem(player.getInventory(), itemCost); //return money items back to buyer
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return;
                }
            }

            //add item to buyer's inventory
            int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack());
            //revert back if player does not have enough room in inventory
            if (buyerOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - buyerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    InventoryUtils.removeItem(shop.getInventory(), itemCost); //take money items back from shop
                    InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
                }
                InventoryUtils.addItem(player.getInventory(), itemCost); //give buyer their money items back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return;
            }
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
    }

    public void playerSellToShop(Player player, ShopObject shop) {
        if (plugin.useVault()) {

            //remove money from shop owner
            if (!shop.isAdminShop()) {
                EconomyResponse response = plugin.getEconomy().withdrawPlayer(shop.getOwnerPlayer(), shop.getPrice());
                if (response.transactionSuccess() == false) {
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return;
                }
            }

            //remove items from player
            int playerItemOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getItemStack());
            //revert back if player does not have enough money items in inventory
            if (playerItemOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - playerItemOverflow);
                InventoryUtils.addItem(player.getInventory(), revert); //return underflow items back to players inventory
                plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice()); //return money to shop owner

                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return;
            }

            //add those items to the shop
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getItemStack());
                //revert back if shop does not have enough room for money items
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert); //remove underflow items from shop inventory
                    plugin.getEconomy().depositPlayer(shop.getOwnerPlayer(), shop.getPrice()); //return money to shop owner
                    //TODO the line underneath this one may need to be commented out. TESTING REQUIRED
                    InventoryUtils.addItem(player.getInventory(), shop.getItemStack()); //return items back to seller
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return;
                }
            }

            //add money to seller (player)
            plugin.getEconomy().depositPlayer(player, shop.getPrice());
        } else {
            ItemStack itemCost = plugin.getItemCurrency();
            itemCost.setAmount((int) shop.getPrice());

            //remove money items from shop's inventory
            if (!shop.isAdminShop()) {
                int shopMoneyOverflow = InventoryUtils.removeItem(shop.getInventory(), itemCost);
                //revert back if shop does not have enough money items in stock
                if (shopMoneyOverflow > 0) {
                    ItemStack revert = itemCost.clone();
                    revert.setAmount(revert.getAmount() - shopMoneyOverflow);
                    InventoryUtils.addItem(shop.getInventory(), revert); //return underflow items back to shops inventory
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    return;
                }
            }

            //remove items from player
            int playerItemOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getItemStack());
            //revert back if player does not have enough money items in inventory
            if (playerItemOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - playerItemOverflow);
                InventoryUtils.addItem(player.getInventory(), revert); //return underflow items back to players inventory
                InventoryUtils.addItem(shop.getInventory(), itemCost); //return money items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                player.updateInventory();
                return;
            }

            //add those items to the shop
            if (!shop.isAdminShop()) {
                int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getItemStack());
                //revert back if shop does not have enough room for money items
                if (shopOverflow > 0) {
                    ItemStack revert = shop.getItemStack().clone();
                    revert.setAmount(revert.getAmount() - shopOverflow);
                    InventoryUtils.removeItem(shop.getInventory(), revert); //remove underflow items from shop inventory
                    InventoryUtils.addItem(shop.getInventory(), itemCost); //return money items back to shops inventory
                    InventoryUtils.addItem(player.getInventory(), shop.getItemStack()); //return items back to seller
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    player.updateInventory();
                    return;
                }
            }

            //add money items to player's inventory
            int sellerOverflow = InventoryUtils.addItem(player.getInventory(), itemCost);
            //revert back if player does not have enough room in inventory
            if (sellerOverflow > 0) {
                ItemStack revert = itemCost.clone();
                revert.setAmount(revert.getAmount() - sellerOverflow);
                InventoryUtils.removeItem(player.getInventory(), revert); //remove underflow items from players inventory
                if (!shop.isAdminShop()) {
                    InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack()); //take items back from shop
                    InventoryUtils.addItem(shop.getInventory(), itemCost); //return money items back to shops inventory
                }
                InventoryUtils.addItem(player.getInventory(), shop.getItemStack()); //give seller their items back
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                player.updateInventory();
                return;
            }
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
    }

    public void playerBarterWithShop(Player player, ShopObject shop) {
        //the shop is giving away its ItemStack
        //the shop is receiving its BarterItemStack

        //remove itemstack from shop's inventory
        if (!shop.isAdminShop()) {
            int shopOverflow = InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack());
            //revert back if shop does not have enough items in stock
            if (shopOverflow > 0) {
                ItemStack revert = shop.getItemStack().clone();
                revert.setAmount(revert.getAmount() - shopOverflow);
                InventoryUtils.addItem(shop.getInventory(), revert); //return underflow items back to shops inventory
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                return;
            }
        }

        //remove barteritemstack from player
        int playerOverflow = InventoryUtils.removeItem(player.getInventory(), shop.getBarterItemStack());
        //revert back if player does not have enough money items in inventory
        if (playerOverflow > 0) {
            ItemStack revert = shop.getBarterItemStack().clone();
            revert.setAmount(revert.getAmount() - playerOverflow);
            InventoryUtils.addItem(player.getInventory(), revert); //return underflow items back to players inventory
            InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
            player.updateInventory();
            return;
        }

        //add barteritemstack to the shop
        if (!shop.isAdminShop()) {
            int shopOverflow = InventoryUtils.addItem(shop.getInventory(), shop.getBarterItemStack());
            //revert back if shop does not have enough room for money items
            if (shopOverflow > 0) {
                ItemStack revert = shop.getBarterItemStack().clone();
                revert.setAmount(revert.getAmount() - shopOverflow);
                InventoryUtils.removeItem(shop.getInventory(), revert); //remove underflow items from shop inventory
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
                InventoryUtils.addItem(player.getInventory(), shop.getBarterItemStack()); //return barter items back to buyer
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                player.updateInventory();
                return;
            }
        }

        //add item to buyer's inventory
        int buyerOverflow = InventoryUtils.addItem(player.getInventory(), shop.getItemStack());
        //revert back if player does not have enough room in inventory
        if (buyerOverflow > 0) {
            ItemStack revert = shop.getItemStack().clone();
            revert.setAmount(revert.getAmount() - buyerOverflow);
            InventoryUtils.removeItem(player.getInventory(), revert); //remove underflow items from players inventory
            if (!shop.isAdminShop()) {
                InventoryUtils.removeItem(shop.getInventory(), shop.getBarterItemStack()); //take barteritem back from shop
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack()); //return items back to shops inventory
            }
            InventoryUtils.addItem(player.getInventory(), shop.getBarterItemStack()); //give buyer their money items back
            player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
            player.updateInventory();
            return;
        }
        player.updateInventory();
        sendExchangeMessages(shop, player);
    }

    private void sendExchangeMessages(ShopObject shop, Player player) {
        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "user", shop, player));

        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if (owner != null && !shop.isAdminShop())
            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "owner", shop, player));
    }
}
