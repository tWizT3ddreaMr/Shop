
package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return; // off hand packet, ignore.
            }
        } catch (NoSuchMethodError error) {}
        Player player = event.getPlayer();

        //player clicked the sign of a shop
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.WALL_SIGN) {
                ShopObject shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
                if (shop == null || !shop.isInitialized())
                    return;

                //delete shop if it does not have a chest attached to it
                if(!(plugin.getShopHandler().isChest(shop.getChestLocation().getBlock()))){
                    shop.delete();
                    return;
                }

                //player did not click their own shop
                if ((!shop.getOwnerName().equals(player.getName())) || shop.isAdminShop()) {
                    if (shop.getType() == ShopType.BUY) {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.buying") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        executeExchange(player, shop, true);

                    } else if (shop.getType() == ShopType.SELL) {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.selling") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        executeExchange(player, shop, true);
                    } else {
                        if (plugin.usePerms() && !(player.hasPermission("shop.use.barter") || player.hasPermission("shop.use"))) {
                            if (!player.hasPermission("shop.operator")) {
                                player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                                return;
                            }
                        }
                        executeExchange(player, shop, true);
                    }
                } else {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "useOwnShop", shop, player));
                    sendEffects(false, player, shop);
                }
                event.setCancelled(true);
            }
        }
    }

    private void executeExchange(Player player, ShopObject shop, boolean isCheck){
        ExchangeIssue issue = null;
        switch(shop.getType()){
            case BUY:
                issue = playerSellToShop(player, shop, isCheck);
                break;
            case SELL:
                issue = playerBuyFromShop(player, shop, isCheck);
                break;
            case BARTER:
                issue = playerBarterWithShop(player, shop, isCheck);
                break;
        }

        //if there are no issues with the transaction
        if(issue == ExchangeIssue.NONE && isCheck){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, shop);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled())
                return;

            //run the exchange again without the check clause
            executeExchange(player, shop, false);
            return;
        }
        //there was an issue when checking, send reason to player
        else if(issue != ExchangeIssue.NONE){
            switch (issue){
                case INSUFFICIENT_FUNDS_SHOP:
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    break;
                case INSUFFICIENT_FUNDS_PLAYER:
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                    break;
                case INVENTORY_FULL_SHOP:
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoSpace", shop, player));
                    break;
                case INVENTORY_FULL_PLAYER:
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoSpace", shop, player));
                    break;
            }
            sendEffects(false, player, shop);
            return;
        }

        //the transaction has finished and the exchange event has not been cancelled
        sendExchangeMessages(shop, player);
        sendEffects(true, player, shop);
    }

    public ExchangeIssue playerBuyFromShop(Player player, ShopObject shop, boolean isCheck){

        //check if shop has enough items
        if (!shop.isAdminShop()) {
            int shopItems = InventoryUtils.getAmount(shop.getInventory(), shop.getItemStack());
            if (shopItems < shop.getItemStack().getAmount())
                return ExchangeIssue.INSUFFICIENT_FUNDS_SHOP;
            //remove items from shop
            if(!isCheck)
                InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
        }

        //check if player has enough currency
        boolean hasFunds = EconomyUtils.hasSufficientFunds(player,player.getInventory(), shop.getPrice());
        if(!hasFunds)
            return ExchangeIssue.INSUFFICIENT_FUNDS_PLAYER;
        //remove currency from player
        if(!isCheck)
            EconomyUtils.removeFunds(player,player.getInventory(),shop.getPrice());

        //check if shop has enough room to accept currency
        if (!shop.isAdminShop()) {
            boolean hasRoom = EconomyUtils.canAcceptFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
            if(!hasRoom)
                return ExchangeIssue.INVENTORY_FULL_SHOP;
            //add currency to shop
            if(!isCheck)
                EconomyUtils.addFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
        }

        //check if player has enough room to accept items
        boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), shop.getItemStack(), player);
        if(!hasRoom)
            return ExchangeIssue.INVENTORY_FULL_PLAYER;
        //add items to player's inventory
        if(!isCheck)
            InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player);

        player.updateInventory();
        return ExchangeIssue.NONE;
    }

    public ExchangeIssue playerSellToShop(Player player, ShopObject shop, boolean isCheck){

        //TODO change all of these around as method below and then do barter
        //check if player has enough items
        int shopItems = InventoryUtils.getAmount(shop.getInventory(), shop.getItemStack());
        if (shopItems < shop.getItemStack().getAmount())
            return ExchangeIssue.INSUFFICIENT_FUNDS_SHOP;
        //remove items from shop
        if(!isCheck)
            InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());

        //check if player has enough currency
        boolean hasFunds = EconomyUtils.hasSufficientFunds(player,player.getInventory(), shop.getPrice());
        if(!hasFunds)
            return ExchangeIssue.INSUFFICIENT_FUNDS_PLAYER;
        //remove currency from player
        if(!isCheck)
            EconomyUtils.removeFunds(player,player.getInventory(),shop.getPrice());

        //check if shop has enough room to accept currency
        if (!shop.isAdminShop()) {
            boolean hasRoom = EconomyUtils.canAcceptFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
            if(!hasRoom)
                return ExchangeIssue.INVENTORY_FULL_SHOP;
            //add currency to shop
            if(!isCheck)
                EconomyUtils.addFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
        }

        //check if player has enough room to accept items
        boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), shop.getItemStack(), player);
        if(!hasRoom)
            return ExchangeIssue.INVENTORY_FULL_PLAYER;
        //add items to player's inventory
        if(!isCheck)
            InventoryUtils.addItem(player.getInventory(), shop.getItemStack(), player);

        player.updateInventory();
        return ExchangeIssue.NONE;
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
        return true;
    }

    private void sendExchangeMessages(ShopObject shop, Player player) {
        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "user", shop, player));

        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if ((owner != null) && (!shop.isAdminShop()))
            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "owner", shop, player));
    }

    public void sendEffects(boolean success, Player player, ShopObject shop){
        try {
            //only send effects to player if server is above MC 1.8 (when OFF_HAND was introduced)
            if(EquipmentSlot.OFF_HAND == EquipmentSlot.OFF_HAND) {
                if (success) {
                    if (plugin.playSounds())
                        player.playSound(shop.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_TOUCH, 1.0F, 1.0F);
                    if (plugin.playEffects())
                        player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK.getId());
                } else {
                    if (plugin.playSounds())
                        player.playSound(shop.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    if (plugin.playEffects())
                        player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
                }
            }
        } catch (NoSuchFieldError e){ }
    }

    private enum ExchangeIssue {
        INSUFFICIENT_FUNDS_SHOP,
        INSUFFICIENT_FUNDS_PLAYER,
        INVENTORY_FULL_SHOP,
        INVENTORY_FULL_PLAYER,
        NONE;
    }
}