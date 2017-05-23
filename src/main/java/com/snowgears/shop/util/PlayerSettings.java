package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSettings {

    public enum Option{
        SALE_NOTIFICATIONS, STOCK_NOTIFICATIONS;
    }

    private UUID player;
    private HashMap<Option, Boolean> optionsMap;

    public PlayerSettings (Player player){
        this.player = player.getUniqueId();
    }

    private PlayerSettings (UUID player, HashMap<Option, Boolean> optionsMap){
        this.player = player;
        this.optionsMap = optionsMap;
    }

    public void setOption(Option option, boolean set){
        optionsMap.put(option, set);
        saveToFile();
    }

    public boolean getOption(Option option){
        if(optionsMap.containsKey(option))
            return optionsMap.get(option);
        optionsMap.put(option, true);
        saveToFile();
        return true;
    }

    private void saveToFile(){
        try {
            File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

            File settingsDirectory = new File(fileDirectory, "PlayerSettings");
            if (!settingsDirectory.exists())
                settingsDirectory.mkdir();

            File playerSettingsFile = new File(settingsDirectory, this.player.toString() + ".yml");
            if (!playerSettingsFile.exists())
                playerSettingsFile.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerSettingsFile);

            config.set("player.UUID", this.player.toString());
            for(Map.Entry<Option, Boolean> entry : optionsMap.entrySet()){
                config.set(entry.getKey().toString(), entry.getValue().toString());
            }

            config.save(playerSettingsFile);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static PlayerSettings loadFromFile(Player player){
        if(player == null)
            return null;
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");

        File settingsDirectory = new File(fileDirectory, "PlayerSettings");
        if (!settingsDirectory.exists())
            settingsDirectory.mkdir();

        File playerSettingsFile = new File(settingsDirectory, player.getUniqueId().toString() + ".yml");

        if (playerSettingsFile.exists()) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerSettingsFile);

            UUID uuid = UUID.fromString(config.getString("player.UUID"));
            HashMap<Option, Boolean> optionsMap = new HashMap<>();

            for(Option option : Option.values()){
                boolean value = config.getBoolean("player."+option.toString());
                optionsMap.put(option, value);
            }

            PlayerSettings settings = new PlayerSettings(uuid, optionsMap);
            return settings;
        }
        return null;
    }
}
