package com.snowgears.shop.utils;

import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Set;


public class ShopMessage {

    private static HashMap<String, String> messageMap = new HashMap<String, String>();
    private static HashMap<String, String[]> shopSignTextMap = new HashMap<String, String[]>();
    private static YamlConfiguration config;

    public ShopMessage(Shop plugin) {

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        loadMessagesFromConfig();
        loadSignTextFromConfig();
    }

    public static String getMessage(String key, String subKey, ShopObject shop, Player player) {
        String message;
        if (subKey != null)
            message = messageMap.get(key + "_" + subKey);
        else
            message = messageMap.get(key);

        message = formatChatMessage(message, shop, player);
        return message;
    }

    //      # [item] : The name of the item in the transaction #
    //      # [item amount] : The amount of the item #
    //      # [barter item] : The name of the barter item in the transaction #
    //      # [barter item amount] : The amount of the barter item #
    //      # [user] : The name of the player who used the shop #
    //      # [owner] : The name of the shop owner #
    //      # [server name] : The name of the server #
    private static String formatChatMessage(String unformattedMessage, ShopObject shop, Player player){
        if(unformattedMessage == null) {
            loadMessagesFromConfig();
            return "";
        }
        if(shop != null && shop.getItemStack() != null) {
            unformattedMessage = unformattedMessage.replace("[item amount]", "" + shop.getItemStack().getAmount());
            unformattedMessage = unformattedMessage.replace("[item]", "" + UtilMethods.getItemName(shop.getItemStack()));
        }
        if(shop != null && shop.getBarterItemStack() != null) {
            unformattedMessage = unformattedMessage.replace("[barter item amount]", "" + shop.getBarterItemStack().getAmount());
            unformattedMessage = unformattedMessage.replace("[barter item]", "" + UtilMethods.getItemName(shop.getBarterItemStack()));
        }
        if(shop != null) {
            unformattedMessage = unformattedMessage.replace("[owner]", "" + shop.getOwnerName());
            unformattedMessage = unformattedMessage.replace("[price]", "" + shop.getPriceString());
            unformattedMessage = unformattedMessage.replace("[shop type]", "" + shop.getType().toString());
        }
        if(player != null)
            unformattedMessage = unformattedMessage.replace("[user]", "" + player.getName());
        unformattedMessage = unformattedMessage.replace("[server name]", "" + Bukkit.getServer().getServerName());

        unformattedMessage = ChatColor.translateAlternateColorCodes('&', unformattedMessage);
        return unformattedMessage;
    }

    //      # [amount] : The amount of items the shop is selling/buying/bartering #
    //      # [price] : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
    //      # [owner] : The name of the shop owner #
    //      # [server name] : The name of the server #
    public static String[] getSignLines(ShopObject shop){
        String[] lines;
        if(shop.isAdminShop())
            lines = getUnformattedShopSignLines(shop.getType(), "admin");
        else
            lines = getUnformattedShopSignLines(shop.getType(), "normal");

        for(int i=0; i<lines.length; i++) {
            lines[i] = lines[i].replace("[amount]", "" + shop.getAmount());
            if (shop.getType() == ShopType.BARTER)
                lines[i] = lines[i].replace("[price]", "" + (int) shop.getPrice());
            else
                lines[i] = lines[i].replace("[price]", shop.getPriceString());
            lines[i] = lines[i].replace("[owner]", "" + shop.getOwnerName());
            lines[i] = lines[i].replace("[server name]", "" + Bukkit.getServer().getServerName());

            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
        }
        return lines;
    }

    private static String[] getUnformattedShopSignLines(ShopType type, String subtype) {
        if (shopSignTextMap.get(type.toString()+"_"+subtype) == null) {
            String[] lines = new String[4];
            lines[0] = ChatColor.BOLD+"[shop]";
            if(type == ShopType.SELL || type == ShopType.BUY) {
                lines[1] = UtilMethods.capitalize(type.toString().toLowerCase()) + "ing: "+ChatColor.BOLD+"[amount]";
                lines[2] = ChatColor.GREEN+"[price]";
            }
            else {
                lines[1] = "Bartering:";
                lines[2] = ChatColor.GREEN+"[price]   for   [amount]";
            }
            if(subtype.equalsIgnoreCase("admin"))
                lines[3] = ChatColor.LIGHT_PURPLE+"[server name]";
            else
                lines[3] = "[owner]";
            return lines;
        }
        return shopSignTextMap.get(type.toString()+"_"+subtype).clone();
    }

    private static void loadMessagesFromConfig() {

        for (ShopType type : ShopType.values()) {
            messageMap.put(type.toString() + "_user", config.getString("transactionMessages." + type.toString().toUpperCase() + ".user"));
            messageMap.put(type.toString() + "_owner", config.getString("transactionMessages." + type.toString().toUpperCase() + ".owner"));

            messageMap.put(type.toString() + "_create", config.getString("interactionMessages." + type.toString().toUpperCase() + ".create"));
            messageMap.put(type.toString() + "_destroy", config.getString("interactionMessages." + type.toString().toUpperCase() + ".destroy"));
            messageMap.put(type.toString() + "_opDestroy", config.getString("interactionMessages." + type.toString().toUpperCase() + ".opDestroy"));
            messageMap.put(type.toString() + "_opOpen", config.getString("interactionMessages." + type.toString().toUpperCase() + ".opOpen"));

            messageMap.put(type.toString() + "_shopNoStock", config.getString("transactionIssueMessages." + type.toString().toUpperCase() + ".shopNoStock"));
            messageMap.put(type.toString() + "_shopNoSpace", config.getString("transactionIssueMessages." + type.toString().toUpperCase() + ".shopNoSpace"));
            messageMap.put(type.toString() + "_playerNoStock", config.getString("transactionIssueMessages." + type.toString().toUpperCase() + ".playerNoStock"));
            messageMap.put(type.toString() + "_playerNoSpace", config.getString("transactionIssueMessages." + type.toString().toUpperCase() + ".playerNoSpace"));
        }

        messageMap.put("permission_use", config.getString("permissionMessages.use"));
        messageMap.put("permission_create", config.getString("permissionMessages.create"));
        messageMap.put("permission_destroy", config.getString("permissionMessages.destroy"));
        messageMap.put("permission_buildLimit", config.getString("permissionMessages.buildLimit"));

        messageMap.put("interactionIssue_line2", config.getString("interactionIssueMessages.createLine2"));
        messageMap.put("interactionIssue_line3", config.getString("interactionIssueMessages.createLine3"));
        messageMap.put("interactionIssue_noItem", config.getString("interactionIssueMessages.createNoItem"));
        messageMap.put("interactionIssue_direction", config.getString("interactionIssueMessages.createDirection"));
        messageMap.put("interactionIssue_sameItem", config.getString("interactionIssueMessages.createSameItem"));
        messageMap.put("interactionIssue_displayRoom", config.getString("interactionIssueMessages.createDisplayRoom"));
        messageMap.put("interactionIssue_initialize", config.getString("interactionIssueMessages.initializeOtherShop"));
        messageMap.put("interactionIssue_destroyChest", config.getString("interactionIssueMessages.destroyChest"));
        messageMap.put("interactionIssue_useOwnShop", config.getString("interactionIssueMessages.useOwnShop"));
        messageMap.put("interactionIssue_adminOpen", config.getString("interactionIssueMessages.adminOpen"));


    }

    private void loadSignTextFromConfig() {
        Set<String> allTypes = config.getConfigurationSection("signs").getKeys(false);
        for (String typeString : allTypes) {
            ShopType type = ShopType.valueOf(typeString);

            String[] normalLines = new String[4];
            Set<String> normalLineNumbers = config.getConfigurationSection("signs."+typeString+".normal").getKeys(false);

            int i = 0;
            for(String number : normalLineNumbers){
                String message = config.getString("signs." + typeString + ".normal." + number);
                if(message == null)
                    normalLines[i] = "";
                else
                    normalLines[i] = message;
                i++;
            }

            this.shopSignTextMap.put(type.toString()+"_normal", normalLines);

            String[] adminLines = new String[4];
            Set<String> adminLineNumbers = config.getConfigurationSection("signs."+typeString+".admin").getKeys(false);

            i = 0;
            for(String number : adminLineNumbers){
                String message = config.getString("signs."+typeString+".admin."+number);
                if(message == null)
                    adminLines[i] = "";
                else
                    adminLines[i] = message;
                i++;
            }

            this.shopSignTextMap.put(type.toString()+"_admin", adminLines);
        }
    }
}