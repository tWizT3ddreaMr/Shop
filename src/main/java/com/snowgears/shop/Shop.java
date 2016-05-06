package com.snowgears.shop;

import com.snowgears.shop.listeners.*;
import com.snowgears.shop.utils.ItemNameUtil;
import com.snowgears.shop.utils.Metrics;
import com.snowgears.shop.utils.ShopMessage;
import com.snowgears.shop.utils.UtilMethods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Shop extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Shop plugin;

    private ShopListener shopListener = new ShopListener(this);
    private DisplayItemListener displayListener;
    private ExchangeListener exchangeListener = new ExchangeListener(this);
    private MiscListener miscListener = new MiscListener(this);
    private CreativeSelectionListener creativeSelectionListener;
    private ClearLaggListener clearLaggListener;

    private ShopHandler shopHandler;
    private EnderChestHandler enderChestHandler;
    private ShopMessage shopMessage;
    private ItemNameUtil itemNameUtil;

    private boolean usePerms = false;
    private boolean useVault = false;
    private ItemStack itemCurrency = null;
    private String itemCurrencyName = "";
    private String vaultCurrencySymbol = "";
    private Economy econ = null;
    private double creationCost;
    private double destructionCost;
    private ArrayList<String> worldBlackList;

    private YamlConfiguration config;

    public static Shop getPlugin() {
        return plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {
        plugin = this;

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        File chatConfigFile = new File(getDataFolder(), "chatConfig.yml");
        if (!chatConfigFile.exists()) {
            chatConfigFile.getParentFile().mkdirs();
            copy(getResource("chatConfig.yml"), chatConfigFile);
        }

        File signConfigFile = new File(getDataFolder(), "signConfig.yml");
        if (!signConfigFile.exists()) {
            signConfigFile.getParentFile().mkdirs();
            copy(getResource("signConfig.yml"), signConfigFile);
        }

        File itemNameFile = new File(getDataFolder(), "items.tsv");
        if (!itemNameFile.exists()) {
            itemNameFile.getParentFile().mkdirs();
            copy(getResource("items.tsv"), itemNameFile);
        }

        shopHandler = new ShopHandler(this);
        enderChestHandler = new EnderChestHandler(this);
        creativeSelectionListener = new CreativeSelectionListener(this);
        displayListener = new DisplayItemListener(this);

        getServer().getPluginManager().registerEvents(shopListener, this);
        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(exchangeListener, this);
        getServer().getPluginManager().registerEvents(miscListener, this);
        getServer().getPluginManager().registerEvents(creativeSelectionListener, this);

        if (getServer().getPluginManager().getPlugin("ClearLag") != null) {
            clearLaggListener = new ClearLaggListener(this);
            getServer().getPluginManager().registerEvents(clearLaggListener, this);
        }

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats
        }

        shopMessage = new ShopMessage(this);
        itemNameUtil = new ItemNameUtil();

        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                getServer().getConsoleSender().sendMessage("[Shop]" + ChatColor.RED + " Data folder could not be created.");
            }
        }

        usePerms = config.getBoolean("usePermissions");
        useVault = config.getBoolean("useVault");
        String itemCurrencyIDString = config.getString("itemCurrencyID");
        int itemCurrencyId;
        int itemCurrencyData = 0;
        if (itemCurrencyIDString.contains(";")) {
            itemCurrencyId = Integer.parseInt(itemCurrencyIDString.substring(0, itemCurrencyIDString.indexOf(";")));
            itemCurrencyData = Integer.parseInt(itemCurrencyIDString.substring(itemCurrencyIDString.indexOf(";") + 1, itemCurrencyIDString.length()));
        } else {
            itemCurrencyId = Integer.parseInt(itemCurrencyIDString.substring(0, itemCurrencyIDString.length()));
        }

        itemCurrency = new ItemStack(itemCurrencyId);
        itemCurrency.setData(new MaterialData(itemCurrencyId, (byte) itemCurrencyData));
        itemCurrencyName = config.getString("itemCurrencyName");
        vaultCurrencySymbol = config.getString("vaultCurrencySymbol");

        creationCost = config.getDouble("creationCost");
        destructionCost = config.getDouble("destructionCost");

        worldBlackList = new ArrayList<String>();
        for(String world : config.getConfigurationSection("worldBlacklist").getKeys(true)){
            worldBlackList.add(world);
        }

        if (useVault) {
            if (!setupEconomy()) {
                log.severe("[Shop] PLUGIN DISABLED DUE TO NO VAULT DEPENDENCY FOUND ON SERVER!");
                log.info("[Shop] If you do not wish to use Vault with Shop, make sure to set 'useVault' in the config file to false.");
                getServer().getPluginManager().disablePlugin(plugin);
                return;
            } else {
                log.info("[Shop] Vault dependency found. Using the Vault economy (" + vaultCurrencySymbol + ") for currency on the server.");
            }
        } else {
            if (itemCurrency == null) {
                log.severe("[Shop] PLUGIN DISABLED DUE TO INVALID VALUE IN CONFIGURATION SECTION: \"itemCurrencyID\"");
                getServer().getPluginManager().disablePlugin(plugin);
            } else
                log.info("[Shop] Shops will use " + itemCurrency.getType().name().replace("_", " ").toLowerCase() + " as the currency on the server.");
        }

     //   shopHandler.refreshShopItems(); //REFRESH WILL NO LONGER BE USED
    }

    @Override
    public void onDisable(){
        enderChestHandler.saveEnderChests();
        shopHandler.saveShops();
    }

    //TODO replace all of these messages
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (cmd.getName().equalsIgnoreCase("shop") || cmd.getName().equalsIgnoreCase("chestshop")) {
                sender.sendMessage("[Shop] Available Commands:");
                sender.sendMessage("   /(shop / chestshop) list");
                sender.sendMessage("   /(shop / chestshop) item refresh");
                sender.sendMessage("   /(shop / chestshop) item hardreset");
            }
        } else if (args.length == 1) {
            if(!(cmd.getName().equalsIgnoreCase("shop") || cmd.getName().equalsIgnoreCase("chestshop")))
                return true;
            if (args[0].equalsIgnoreCase("list")) {
                if (sender instanceof Player)
                    sender.sendMessage("There are " + ChatColor.GOLD + shopHandler.getNumberOfShops() + ChatColor.WHITE + " shops registered on the server.");
                else
                    sender.sendMessage("[Shop] There are " + shopHandler.getNumberOfShops() + " shops registered on the server.");
            }
            else if (args[0].equalsIgnoreCase("save")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((usePerms && !player.hasPermission("shop.operator")) || !player.isOp()) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                    shopHandler.saveShops();
                    sender.sendMessage("[Shop] The shops have been saved to the shops.yml file.");
                } else {
                    shopHandler.saveShops();
                    sender.sendMessage("[Shop] The shops have been saved to the shops.yml file.");
                }
            }
        } else if (args.length == 2) {
            if(!(cmd.getName().equalsIgnoreCase("shop") || cmd.getName().equalsIgnoreCase("chestshop")))
                return true;
//            if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("refresh")) {
//                if (sender instanceof Player) {
//                    Player player = (Player) sender;
//                    if ((usePerms && !player.hasPermission("shop.operator")) || !player.isOp()) {
//                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
//                        return true;
//                    }
//                    shopHandler.refreshShopItems();
//                    sender.sendMessage(ChatColor.GRAY + "The display items on all of the shops have been refreshed.");
//                } else {
//                    shopHandler.refreshShopItems();
//                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
//                }
            //REPLACE REFRESH WITH HARDRESET
            if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("refresh")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((usePerms && !player.hasPermission("shop.operator")) || !player.isOp()) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                    for (World world : plugin.getServer().getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity.getType() == EntityType.DROPPED_ITEM) {
                                ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                                if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                                    entity.remove();
                                }
                            }
                        }
                    }
                    shopHandler.refreshShopItems();
                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
                } else {
                    for (World world : plugin.getServer().getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity.getType() == EntityType.DROPPED_ITEM) {
                                ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                                if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                                    entity.remove();
                                }
                            }
                        }
                    }
                    shopHandler.refreshShopItems();
                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
                }
            }
        }
        return true;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ShopListener getShopListener() {
        return shopListener;
    }

    public DisplayItemListener getDisplayListener() {
        return displayListener;
    }

    public CreativeSelectionListener getCreativeSelectionListener() {
        return creativeSelectionListener;
    }

    public ShopHandler getShopHandler() {
        return shopHandler;
    }

    public EnderChestHandler getEnderChestHandler(){
        return enderChestHandler;
    }

    public boolean usePerms() {
        return usePerms;
    }

    public boolean useVault() {
        return useVault;
    }

    public ItemStack getItemCurrency() {
        return itemCurrency;
    }

    public String getItemCurrencyName() {
        return itemCurrencyName;
    }

    public String getVaultCurrencySymbol() {
        return vaultCurrencySymbol;
    }

    public Economy getEconomy() {
        return econ;
    }

    public double getCreationCost(){
        return creationCost;
    }

    public double getDestructionCost(){
        return destructionCost;
    }

    public ItemNameUtil getItemNameUtil(){
        return itemNameUtil;
    }

    public ArrayList<String> getWorldBlacklist(){
        return worldBlackList;
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}