package com.snowgears.shop.listeners;

import com.javafx.tools.doclets.internal.toolkit.util.Util;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.events.PlayerCreateShopEvent;
import com.snowgears.shop.events.PlayerDestroyShopEvent;
import com.snowgears.shop.events.PlayerResizeShopEvent;
import com.snowgears.shop.utils.ShopMessage;
import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


public class MiscListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public MiscListener(Shop instance) {
        plugin = instance;
    }

    //prevent placing block above shop chest (need display item there)
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ShopObject shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    //prevent emptying of bucket when player clicks on shop sign
    //also prevent when emptying on display item itself
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block b = event.getBlockClicked();

        if (b.getType() == Material.WALL_SIGN) {
            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) event.getBlockClicked().getState().getData();
            ShopObject shop = plugin.getShopHandler().getShopByChest(b.getRelative(sign.getAttachedFace()));
            if (shop != null)
                event.setCancelled(true);
        }
        Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
        ShopObject shop = plugin.getShopHandler().getShopByChest(blockToFill.getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    //player places a sign on a chest and creates an initial shop with no item
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopCreation(SignChangeEvent event) {
        final Block b = event.getBlock();
        final Player player = event.getPlayer();
        final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();

        Block chest;
        if (sign.isWallSign())
            chest = b.getRelative(sign.getAttachedFace());
        else
            chest = b.getRelative(sign.getFacing().getOppositeFace());

        double price;
        int amount;
        ShopType type;
        if (plugin.getShopHandler().isChest(chest)) {
            final Sign signBlock = (Sign) b.getState();
            if (event.getLine(0).contains(ShopMessage.getCreationWord("SHOP"))) {

                int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
                int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);

                if (!player.isOp() && plugin.usePerms() && !player.hasPermission("shop.operator")) {
                    if (numberOfShops >= buildPermissionNumber) {
                        event.setCancelled(true);
                        ShopObject tempShop = new ShopObject(null, player.getUniqueId(), 0, 0, false, ShopType.SELL);
                        player.sendMessage(ShopMessage.getMessage("permission", "buildLimit", tempShop, player));
                        tempShop = null;
                        return;
                    }
                }

                try {
                    String line2 = UtilMethods.cleanNumberText(event.getLine(1));
                    amount = Integer.parseInt(line2);
                    if (amount < 1) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                    return;
                }

                if(plugin.useVault()){
                    try {
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));
                        price = Double.parseDouble(line3);
                        if (price <= 0) {
                            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                            return;
                        }
                    } catch (NumberFormatException e){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                }
                else{
                    try {
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));
                        price = Integer.parseInt(line3);
                        if (price < 1) {
                            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                            return;
                        }
                    } catch (NumberFormatException e){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                }

                if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("BUY")))
                    type = ShopType.BUY;
                else if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("BARTER")))
                    type = ShopType.BARTER;
                else
                    type = ShopType.SELL;

                String playerMessage = null;
                ShopObject tempShop = new ShopObject(null, player.getUniqueId(), 0, 0, false, type);
                if (type == ShopType.SELL) {
                    if (plugin.usePerms()) {
                        if (!(player.hasPermission("shop.create.selling") || player.hasPermission("shop.create")))
                            playerMessage = ShopMessage.getMessage("permission", "create", tempShop, player);
                    }
                } else if (type == ShopType.BUY) {
                    if (plugin.usePerms()) {
                        if (!(player.hasPermission("shop.create.buying") || player.hasPermission("shop.create")))
                            playerMessage = ShopMessage.getMessage("permission", "create", tempShop, player);
                    }
                } else {
                    if (plugin.usePerms()) {
                        if (!(player.hasPermission("shop.create.barter") || player.hasPermission("shop.create")))
                            playerMessage = ShopMessage.getMessage("permission", "create", tempShop, player);
                    }
                }

                if (plugin.usePerms() && player.hasPermission("shop.operator")) {
                    playerMessage = null;
                }

                if (playerMessage != null) {
                    player.sendMessage(playerMessage);
                    event.setCancelled(true);
                    return;
                }


                boolean isAdmin = false;
                if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("ADMIN"))) {
                    if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))
                        isAdmin = true;
                }

                //make sure that the sign is in front of the chest
             //   DirectionalContainer container = (DirectionalContainer) chest.getState().getData();
                BlockFace chestFacing = UtilMethods.getDirectionOfChest(chest);
                if (chestFacing == sign.getFacing() && chest.getRelative(sign.getFacing()).getLocation().equals(signBlock.getLocation())) {
                    chest.getRelative(sign.getFacing()).setType(Material.WALL_SIGN);
                } else {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "direction", null, player));
                    return;
                }

                if (!sign.isWallSign()) {
                    final Sign newSign = (Sign) chest.getRelative(sign.getFacing()).getState();

                    org.bukkit.material.Sign matSign = new org.bukkit.material.Sign(Material.WALL_SIGN);
                    matSign.setFacingDirection(sign.getFacing());

                    newSign.setData(matSign);
                    newSign.update();
                }
                signBlock.update();

                //add the shop with no item
                final ShopObject shop = new ShopObject(signBlock.getLocation(), player.getUniqueId(), price, amount, isAdmin, type);
                plugin.getShopHandler().addShop(shop);

                shop.updateSign();

                if (type == ShopType.SELL)
                    player.sendMessage(ChatColor.GOLD + "[Shop] Now just hit the sign with the item you want to sell to other players!");
                else if (type == ShopType.BUY) {
                    player.sendMessage(ChatColor.GOLD + "[Shop] Now just hit the sign with the item you want to buy from other players!");
                    player.sendMessage(ChatColor.GRAY + "[Shop] Alternatively, you can hit the shop with your hand to pick the item you want to receive from the creative menu.");
                } else {
                    player.sendMessage(ChatColor.GOLD + "[Shop] Now hit the sign with the item you want to barter to other players!");
                    //	player.sendMessage(ChatColor.GOLD+"[Shop] Then hit the sign again with the item you want receive!");
                    //	player.sendMessage(ChatColor.GRAY+"[Shop] For the item you want to receive, you can also hit the shop with your hand to pick the item from the creative menu.");
                }

                //give player a limited amount of time to finish creating the shop until it is deleted
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        //the shop has still not been initialized with an item from a player
                        if (!shop.isInitialized()) {
                            plugin.getShopHandler().removeShop(shop);
                            if (b.getType() == Material.WALL_SIGN) {
                                Sign sign = (Sign) b.getState();
                                sign.setLine(0, ChatColor.RED + "SHOP CLOSED");
                                sign.setLine(1, ChatColor.GRAY + "CREATION TIMEOUT");
                                sign.setLine(2, "");
                                sign.setLine(3, "");
                                sign.update(true);
                                plugin.getCreativeSelectionListener().returnPlayerData(player);
                            }
                        }
                    }
                }, 1200L); //1 minute
            }
        }
    }

    //this method calls PlayerCreateShopEvent
    @EventHandler
    public void onPreShopSignClick(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();

            if (clicked.getType() == Material.WALL_SIGN) {
                ShopObject shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "initialize", null, player));
                    event.setCancelled(true);
                    return;
                }

                if (player.getItemInHand().getType() == Material.AIR) {
                    return;
                }

                Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "displayRoom", null, player));
                    event.setCancelled(true);
                    return;
                }

                ItemStack shopItem = player.getItemInHand();
                if (shop.getItemStack() == null) {
                    shop.setItemStack(shopItem);
                    if (shop.getType() == ShopType.BARTER) {
                        player.sendMessage(ChatColor.GRAY + "[Shop] You have set this shop's barter item to " + shopItem.getType().name().replace("_", " ").toLowerCase() + "(s)");
                        player.sendMessage(ChatColor.GOLD + "[Shop] Now hit the sign again with the item you want barter for!");
                        player.sendMessage(ChatColor.GRAY + "[Shop] You can also hit the shop with your hand to pick the item from the creative menu.");

                    }
                } else if (shop.getBarterItemStack() == null) {
                    if (!shop.getItemStack().isSimilar(shopItem)) {
                        shop.setBarterItemStack(shopItem);
                    } else {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "sameItem", null, player));
                        event.setCancelled(true);
                        return;
                    }
                }

                if ((shop.getType() == ShopType.BARTER && shop.getBarterItemStack() != null)
                        || (shop.getType() != ShopType.BARTER && shop.getItemStack() != null)) {
                    PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
                    Bukkit.getServer().getPluginManager().callEvent(e);
                }
                event.setCancelled(true);
            }
        }
    }

    //player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
    @EventHandler(priority = EventPriority.HIGHEST)
    public void shopDestroy(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Block b = event.getBlock();
        Player player = event.getPlayer();

        if (b.getType() == Material.WALL_SIGN) {
            ShopObject shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop == null)
                return;
            else if (!shop.isInitialized()) {
                event.setCancelled(true);
                return;
            }
            //player trying to break their own shop
            if (shop.getOwnerName().equals(player.getName())) {
                if (plugin.usePerms() && !(player.hasPermission("shop.destroy") || player.hasPermission("shop.operator"))) {
                    event.setCancelled(true);
                    player.sendMessage(ShopMessage.getMessage("permission", "destroy", shop, player));
                    return;
                }
                PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled())
                    event.setCancelled(true);
                return;
            }
            //player trying to break other players shop
            else {
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                    plugin.getServer().getPluginManager().callEvent(e);
                    if (e.isCancelled())
                        event.setCancelled(true);
                } else
                    event.setCancelled(true);
            }
        } else if (plugin.getShopHandler().isChest(b)) {

            ShopObject shop = plugin.getShopHandler().getShopByChest(b);
            if (shop != null) {
                //player trying to break their own shop
                if (shop.getOwnerName().equals(player.getName())) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                    event.setCancelled(true);
                    return;
                }
                else{
                    if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                    event.setCancelled(true);
                }
            }
            //may be a double chest
            else {
                if(!(b.getState() instanceof org.bukkit.material.Chest))
                    return;

                Chest chest = (Chest) b.getState();
                InventoryHolder ih = chest.getInventory().getHolder();

                if (ih instanceof DoubleChest) {
                    DoubleChest dchest = (DoubleChest) ih;
                    Chest chestLeft = (Chest) dchest.getLeftSide();
                    Chest chestRight = (Chest) dchest.getRightSide();

                    ShopObject shopLeft = plugin.getShopHandler().getShopByChest(chestLeft.getLocation().getBlock());
                    ShopObject shopRight = plugin.getShopHandler().getShopByChest(chestRight.getLocation().getBlock());

                    if (shopLeft == null && shopRight == null)
                        return;
                        //player trying to break non-sign side of double-shop
                    else if (shopLeft == null) {
                        //owner is trying to
                        if (shopRight.getOwnerName().equals(player.getName())) {
                            PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shopRight, b.getLocation(), false);
                            Bukkit.getPluginManager().callEvent(e);
                            return;
                        }
                        //other player is trying to
                        else {
                            if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shopRight, b.getLocation(), false);
                                Bukkit.getPluginManager().callEvent(e);
                            } else
                                event.setCancelled(true);
                        }
                    } else if (shopRight == null) {
                        //owner is trying to
                        if (shopLeft.getOwnerName().equals(player.getName())) {
                            PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shopLeft, b.getLocation(), false);
                            Bukkit.getPluginManager().callEvent(e);
                            return;
                        }
                        //other player is trying to
                        else {
                            if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shopLeft, b.getLocation(), false);
                                Bukkit.getPluginManager().callEvent(e);
                            } else
                                event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onShopExpansion(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (plugin.getShopHandler().isChest(b)) {
            ArrayList<BlockFace> doubleChestFaces = new ArrayList<BlockFace>();
            doubleChestFaces.add(BlockFace.NORTH);
            doubleChestFaces.add(BlockFace.EAST);
            doubleChestFaces.add(BlockFace.SOUTH);
            doubleChestFaces.add(BlockFace.WEST);

            //find out if the player placed a chest next to an already active shop
            ShopObject shop = plugin.getShopHandler().getShopNearBlock(b);
            if (shop == null || (b.getType() != shop.getChestLocation().getBlock().getType()))
                return;
            else if(b.getType() == Material.ENDER_CHEST)
                return;

            Block shopChestBlock = shop.getChestLocation().getBlock();

            //prevent placing the chest behind current shop (changing its direction)
            if(shopChestBlock.getRelative(shop.getFacing().getOppositeFace()).getLocation().equals(b.getLocation())){
                event.setCancelled(true);
                return;
            }

         //   DirectionalContainer chest = (DirectionalContainer) b.getState().getData();
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(b);

            //prevent placing the chest next to the shop but facing the opposite direction (changing its direction)
            if(chestFacing == shop.getFacing().getOppositeFace()){
                event.setCancelled(true);
                return;
            }

            //owner is trying to
            if (shop.getOwnerName().equals(player.getName())) {
                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                Bukkit.getPluginManager().callEvent(e);
                return;
            }
            //other player is trying to
            else {
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                    Bukkit.getPluginManager().callEvent(e);
                } else
                    event.setCancelled(true);
            }
        }
    }
}