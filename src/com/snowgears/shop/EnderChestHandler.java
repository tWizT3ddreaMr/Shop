package com.snowgears.shop;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class EnderChestHandler {

    private Shop plugin;
    private HashMap<UUID, Inventory> enderChestInventories = new HashMap<UUID, Inventory>(); //for use with ender chest shops while offline

    public EnderChestHandler(Shop plugin){
        this.plugin = plugin;

        loadEnderChests();
    }

    public Inventory getInventory(OfflinePlayer player){
        if(enderChestInventories.get(player.getUniqueId()) != null)
            return enderChestInventories.get(player.getUniqueId());
        return Bukkit.createInventory(null, InventoryType.ENDER_CHEST);
    }

    public void updateInventory(OfflinePlayer player, Inventory inv){
        enderChestInventories.put(player.getUniqueId(), inv);
    }

    public void saveEnderChests() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            fileDirectory.mkdir();
        File chestFile = new File(fileDirectory + "/enderchests.yml");
        if (!chestFile.exists()) { // file doesn't exist
            try {
                chestFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //does exist, clear it for future saving
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(chestFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            writer.print("");
            writer.close();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(chestFile);

        for (Map.Entry<UUID, Inventory> set : enderChestInventories.entrySet()) {
            String owner = set.getKey().toString();
            config.set("enderchests." + owner, set.getValue().getContents());
        }

        try {
            config.save(chestFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void loadEnderChests() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;
        File chestFile = new File(fileDirectory + "/enderchests.yml");
        if (!chestFile.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(chestFile);
        loadShopsFromConfig(config);
    }

    private void loadShopsFromConfig(YamlConfiguration config) {

        if (config.getConfigurationSection("enderchests") == null)
            return;
        Set<String> allChestUUIDs = config.getConfigurationSection("enderchests").getKeys(false);

        for (String chestUUID : allChestUUIDs) {
            ArrayList<ItemStack> contents = new ArrayList<ItemStack>();
            List<String> contentsList = config.getStringList("enderchests."+chestUUID);
            for(String itemstackString : contentsList){
                ItemStack itemStack = config.getItemStack(itemstackString);
                contents.add(itemStack);
            }
            Inventory inv = Bukkit.createInventory(null, InventoryType.ENDER_CHEST);
            inv.setContents(contents.toArray(new ItemStack[contents.size()]));
            enderChestInventories.put(UUID.fromString(chestUUID), inv);
        }
    }
}
