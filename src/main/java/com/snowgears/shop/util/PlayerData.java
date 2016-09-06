package com.snowgears.shop.util;


import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.UUID;

public class PlayerData {

    private String playerName;
    private UUID playerUUID;
    private Location shopSignLocation;
    private GameMode oldGameMode;

    public PlayerData(UUID playerUUID, Location shopSignLocation, GameMode oldGameMode) {
        this.playerUUID = playerUUID;
        this.shopSignLocation = shopSignLocation;
        this.oldGameMode = oldGameMode;
        playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Location getShopSignLocation() {
        return shopSignLocation;
    }

    public ShopObject getShop() {
        return Shop.getPlugin().getShopHandler().getShop(shopSignLocation);
    }

    public GameMode getOldGameMode() {
        return oldGameMode;
    }
}
