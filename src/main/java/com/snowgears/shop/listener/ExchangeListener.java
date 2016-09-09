
package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;


public class ExchangeListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<Location, UUID> shopMessageCooldown = new HashMap<>(); //shop location, shop owner
//    private Logger exchangeLogger;

    public ExchangeListener(Shop instance) {
        plugin = instance;
//        initializeLogger();
    }

    //TODO will need to update ender chest contents at the end of every transaction involving an ender chest

    @EventHandler
    public void onShopSignClick(PlayerInteractEvent event) {
        if(event.isCancelled())
            return;

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
                if (!shop.getOwnerName().equals(player.getName())) {

                    if (plugin.usePerms() && !(player.hasPermission("shop.use."+shop.getType().toString().toLowerCase()) || player.hasPermission("shop.use"))) {
                        if (!player.hasPermission("shop.operator")) {
                            player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                            return;
                        }
                    }
                    executeExchange(player, shop, true);
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
            case GAMBLE:
                if(!shop.getItemStack().equals(plugin.getGambleDisplayItem()))
                    return;
            case SELL:
                issue = playerBuyFromShop(player, shop, isCheck);
                break;
            case BARTER:
                issue = playerBarterWithShop(player, shop, isCheck);
                break;
        }

        //if there are no issues with the test/check transaction
        if(issue == ExchangeIssue.NONE && isCheck){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, shop);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled())
                return;

            //run the transaction again without the check clause
            executeExchange(player, shop, false);
            return;
        }
        //there was an issue when checking transaction, send reason to player
        else if(issue != ExchangeIssue.NONE){
            switch (issue){
                case INSUFFICIENT_FUNDS_SHOP:
                    if(!shop.isAdminShop()){
                        Player owner = shop.getOwnerPlayer().getPlayer();
                        //the shop owner is online
                        if(owner != null && notifyOwner(shop))
                            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "ownerNoStock", shop, owner));
                    }
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "shopNoStock", shop, player));
                    break;
                case INSUFFICIENT_FUNDS_PLAYER:
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "playerNoStock", shop, player));
                    break;
                case INVENTORY_FULL_SHOP:
                    if(!shop.isAdminShop()){
                        Player owner = shop.getOwnerPlayer().getPlayer();
                        //the shop owner is online
                        if(owner != null && notifyOwner(shop))
                            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "ownerNoSpace", shop, owner));
                    }
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

        ItemStack shopItem;
        if(shop.getType() == ShopType.GAMBLE)
            shopItem = shop.getGambleItemStack();
        else
            shopItem = shop.getItemStack();
        //check if shop has enough items
        if (!shop.isAdminShop()) {
            int shopItems = InventoryUtils.getAmount(shop.getInventory(), shopItem);
            if (shopItems < shopItem.getAmount())
                return ExchangeIssue.INSUFFICIENT_FUNDS_SHOP;
            //remove items from shop
            if(!isCheck)
                InventoryUtils.removeItem(shop.getInventory(), shopItem, shop.getOwnerPlayer());
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
        boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), shopItem, player);
        if(!hasRoom)
            return ExchangeIssue.INVENTORY_FULL_PLAYER;
        //add items to player's inventory
        if(!isCheck)
            InventoryUtils.addItem(player.getInventory(), shopItem, player);

        player.updateInventory();
        return ExchangeIssue.NONE;
    }

    public ExchangeIssue playerSellToShop(Player player, ShopObject shop, boolean isCheck){

        //check if player has enough items
        int playerItems = InventoryUtils.getAmount(player.getInventory(), shop.getItemStack());
        if (playerItems < shop.getItemStack().getAmount())
            return ExchangeIssue.INSUFFICIENT_FUNDS_PLAYER;
        //remove items from player
        if(!isCheck)
            InventoryUtils.removeItem(player.getInventory(), shop.getItemStack(), player);

        //check if shop has enough currency
        if(!shop.isAdminShop()) {
            boolean hasFunds = EconomyUtils.hasSufficientFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
            if (!hasFunds)
                return ExchangeIssue.INSUFFICIENT_FUNDS_SHOP;
            //remove currency from shop
            if (!isCheck)
                EconomyUtils.removeFunds(shop.getOwnerPlayer(), shop.getInventory(), shop.getPrice());
        }

        //check if player has enough room to accept currency
        boolean hasRoom = EconomyUtils.canAcceptFunds(player, player.getInventory(), shop.getPrice());
        if(!hasRoom)
            return ExchangeIssue.INVENTORY_FULL_PLAYER;
        //add currency to player
        if(!isCheck)
            EconomyUtils.addFunds(player, player.getInventory(), shop.getPrice());

        //check if shop has enough room to accept items
        if(!shop.isAdminShop()) {
            boolean shopHasRoom = InventoryUtils.hasRoom(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
            if (!shopHasRoom)
                return ExchangeIssue.INVENTORY_FULL_SHOP;
            //add items to shop's inventory
            if (!isCheck)
                InventoryUtils.addItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
        }

        player.updateInventory();
        return ExchangeIssue.NONE;
    }

    public ExchangeIssue playerBarterWithShop(Player player, ShopObject shop, boolean isCheck){

        //check if shop has enough items
        if (!shop.isAdminShop()) {
            int shopItems = InventoryUtils.getAmount(shop.getInventory(), shop.getItemStack());
            if (shopItems < shop.getItemStack().getAmount())
                return ExchangeIssue.INSUFFICIENT_FUNDS_SHOP;
            //remove items from shop
            if(!isCheck)
                InventoryUtils.removeItem(shop.getInventory(), shop.getItemStack(), shop.getOwnerPlayer());
        }

        //check if player has enough barter items
        int playerItems = InventoryUtils.getAmount(player.getInventory(), shop.getBarterItemStack());
        if (playerItems < shop.getBarterItemStack().getAmount())
            return ExchangeIssue.INSUFFICIENT_FUNDS_PLAYER;
        //remove barter items from player
        if(!isCheck)
            InventoryUtils.removeItem(player.getInventory(), shop.getBarterItemStack(), player);

        //check if shop has enough room to accept barter items
        if (!shop.isAdminShop()) {
            boolean hasRoom = InventoryUtils.hasRoom(shop.getInventory(), shop.getBarterItemStack(), shop.getOwnerPlayer());
            if(!hasRoom)
                return ExchangeIssue.INVENTORY_FULL_SHOP;
            //add barter items to shop
            if(!isCheck)
                InventoryUtils.addItem(shop.getInventory(), shop.getBarterItemStack(), shop.getOwnerPlayer());
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

    private void sendExchangeMessages(ShopObject shop, Player player) {
        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "user", shop, player));

        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if ((owner != null) && (!shop.isAdminShop()))
            owner.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "owner", shop, player));
        if(shop.getType() == ShopType.GAMBLE)
            shop.shuffleGambleItem();
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

    private boolean notifyOwner(final ShopObject shop){
        if(shop.isAdminShop())
            return false;
        if(shopMessageCooldown.containsKey(shop.getSignLocation()))
            return false;
        else{
            shopMessageCooldown.put(shop.getSignLocation(), shop.getOwnerUUID());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(shop != null){
                        if(shopMessageCooldown.containsKey(shop.getSignLocation())){
                            shopMessageCooldown.remove(shop.getSignLocation());
                        }
                    }
                    //TODO if shop is null, should you clear the entire cooldown list so that that location isn't messed up?
                }
            }.runTaskLater(this.plugin, 2400); //make cooldown 2 minutes
        }
        return true;
    }

    private enum ExchangeIssue {
        INSUFFICIENT_FUNDS_SHOP,
        INSUFFICIENT_FUNDS_PLAYER,
        INVENTORY_FULL_SHOP,
        INVENTORY_FULL_PLAYER,
        NONE;
    }
}