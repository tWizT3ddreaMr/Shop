package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.HomeWindow;
import com.snowgears.shop.gui.ShopGuiWindow;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ShopGuiHandler {

    public Shop plugin = Shop.getPlugin();

    private HashMap<UUID, ShopGuiWindow> playerGuiWindows = new HashMap<>();

    public ShopGuiHandler(Shop instance){
        plugin = instance;
    }

    public ShopGuiWindow getWindow(Player player){
        if(playerGuiWindows.get(player.getUniqueId()) != null){
            return playerGuiWindows.get(player.getUniqueId());
        }
        HomeWindow window = new HomeWindow(player.getUniqueId());
        playerGuiWindows.put(player.getUniqueId(), window);
        return window;
    }

    public void setWindow(Player player, ShopGuiWindow window){
        playerGuiWindows.put(player.getUniqueId(), window);

        window.open();

    }

    //TODO have a change window to type method here that can be called from the button listener?
}
