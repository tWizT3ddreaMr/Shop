package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.HomeWindow;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.snowgears.shop.util.PlayerSettings;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ShopGuiHandler {

    public Shop plugin = Shop.getPlugin();

    private HashMap<UUID, ShopGuiWindow> playerGuiWindows = new HashMap<>();
    private HashMap<UUID, PlayerSettings> playerSettings = new HashMap<>();

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

    //TODO have a change window to type method here that can be called from the button listener to clean things up?


    public boolean getSettingsOption(Player player, PlayerSettings.Option option){
        if(playerSettings.get(player.getUniqueId()) != null){
            PlayerSettings settings = playerSettings.get(player.getUniqueId());
            return settings.getOption(option);
        }

        PlayerSettings settings = PlayerSettings.loadFromFile(player);
        if(settings == null)
            settings = new PlayerSettings(player);

        playerSettings.put(player.getUniqueId(), settings);
        return settings.getOption(option);
    }

    public void toggleSettingsOption(Player player, PlayerSettings.Option option){
        PlayerSettings settings;

        if(playerSettings.get(player.getUniqueId()) != null){
            settings = playerSettings.get(player.getUniqueId());
        }
        else {
            settings = PlayerSettings.loadFromFile(player);
            if (settings == null)
                settings = new PlayerSettings(player);
        }

        settings.setOption(option, !getSettingsOption(player, option)); //this also handles saving to file internally
        playerSettings.put(player.getUniqueId(), settings);
    }
}
