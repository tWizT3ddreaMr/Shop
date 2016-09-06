package com.snowgears.shop.listener;


import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class CreativeSelectionListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<UUID, PlayerData> gameModeHashMap = new HashMap<UUID, PlayerData>();

    public CreativeSelectionListener(Shop instance) {
        plugin = instance;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                loadPlayerData();
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (gameModeHashMap.get(player.getUniqueId()) != null)
            returnPlayerData(player);
    }

    //this method calls PlayerCreateShopEvent
    @EventHandler
    public void onPreShopSignClick(PlayerInteractEvent event) {
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
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "initialize", shop, player));
                    event.setCancelled(true);
                    return;
                }
                if (shop.getType() == ShopType.BARTER && shop.getItemStack() == null) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "noItem", shop, player));
                    event.setCancelled(true);
                    return;
                }

                if (player.getItemInHand().getType() == Material.AIR) {
                    if (shop.getType() == ShopType.SELL) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "noItem", shop, player));
                    } else {
                        if ((shop.getType() == ShopType.BARTER && shop.getItemStack() != null && shop.getBarterItemStack() == null)
                                || shop.getType() == ShopType.BUY) {
                            this.addPlayerData(player, clicked.getLocation());
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (gameModeHashMap.get(player.getUniqueId()) != null) {
            if (event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                    || event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()) {
                player.teleport(event.getFrom());
                player.sendMessage(ChatColor.RED + "You cannot move in locked creative mode.");
                player.sendMessage(ChatColor.GOLD + "Open your inventory and select the item you want to receive.");
                player.sendMessage(ChatColor.YELLOW + "To select the item, pick it up and drop it outside of the inventory window.");

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (gameModeHashMap.get(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();

        returnPlayerData(player);
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = gameModeHashMap.get(player.getUniqueId());
        if (playerData != null) {
            //player dropped item outside the inventory
            if (event.getSlot() == -999 && event.getCursor() != null) {
                ShopObject shop = playerData.getShop();
                if (shop != null) {
                    if (shop.getType() == ShopType.BUY) {
                        shop.setItemStack(event.getCursor());
                    } else if (shop.getType() == ShopType.BARTER) {
                        shop.setBarterItemStack(event.getCursor());
                    }
                    returnPlayerData(player);

                    PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
                    plugin.getServer().getPluginManager().callEvent(e);
                }
            }
            event.setCancelled(true);
        }
    }

    public void addPlayerData(Player player, Location shopSignLocation) {
        if (gameModeHashMap.get(player.getUniqueId()) != null)
            return;
        gameModeHashMap.put(player.getUniqueId(), new PlayerData(player.getUniqueId(), shopSignLocation, player.getGameMode()));
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage("_____________________________________________________");
        player.sendMessage(ChatColor.GRAY + "You are now in locked creative mode so you can choose the item you want to receive.");
        player.sendMessage(ChatColor.WHITE + "To select the item, pick it up and drop it outside of the inventory window.");
        player.sendMessage(ChatColor.GOLD + "Open your inventory and select the item you want to receive.");
        player.sendMessage("_____________________________________________________");
        this.savePlayerData();
    }

    private void addPlayerData(PlayerData playerData) {
        if (gameModeHashMap.get(playerData.getPlayerUUID()) != null)
            return;
        gameModeHashMap.put(playerData.getPlayerUUID(), playerData);
    }

    public boolean returnPlayerData(Player player) {
        PlayerData playerData = gameModeHashMap.get(player.getUniqueId());
        if (playerData != null) {
            player.setGameMode(playerData.getOldGameMode());
            gameModeHashMap.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ChatColor.GRAY + "You are no longer in locked creative-mode.");
            this.savePlayerData();
            return true;
        }
        return false;
    }

    public void savePlayerData() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            fileDirectory.mkdir();
        File selectionFile = new File(fileDirectory + "/creativeSelectionData.yml");
        if (!selectionFile.exists()) { // file doesn't exist
            try {
                selectionFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //does exist, clear it for future saving
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(selectionFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            writer.print("");
            writer.close();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(selectionFile);

        String player;
        for (PlayerData playerData : gameModeHashMap.values()) {
            player = playerData.getPlayerName() + " (" + playerData.getPlayerUUID().toString() + ")";
            config.set("players." + player + ".gameMode", playerData.getOldGameMode().toString());
            config.set("players." + player + ".shopLocation", locationToString(playerData.getShopSignLocation()));
        }

        try {
            config.save(selectionFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void loadPlayerData() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;
        File selectionFile = new File(fileDirectory + "/creativeSelectionData.yml");
        if (!selectionFile.exists())
            return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(selectionFile);

        if (config.getConfigurationSection("players") == null)
            return;

        Set<String> allPlayers = config.getConfigurationSection("players").getKeys(false);

        for (String player : allPlayers) {
            UUID playerUUID = uidFromString(player);
            GameMode gameMode = GameMode.valueOf(config.getString("players." + player + ".gameMode"));
            Location location = locationFromString(config.getString("players." + player + ".shopLocation"));
            PlayerData playerData = new PlayerData(playerUUID, location, gameMode);
            this.addPlayerData(playerData);
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private UUID uidFromString(String playerString) {
        int index = playerString.indexOf("(");
        String uidString = playerString.substring(index + 1, playerString.length() - 1);
        return UUID.fromString(uidString);
    }
}
