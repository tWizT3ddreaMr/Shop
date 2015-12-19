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
    private static String freePriceWord;
    private static HashMap<String, String> creationWords = new HashMap<String, String>();
    private static YamlConfiguration chatConfig;
    private static YamlConfiguration signConfig;

    public ShopMessage(Shop plugin) {

        File chatConfigFile = new File(plugin.getDataFolder(), "chatConfig.yml");
        chatConfig = YamlConfiguration.loadConfiguration(chatConfigFile);
        File signConfigFile = new File(plugin.getDataFolder(), "signConfig.yml");
        signConfig = YamlConfiguration.loadConfiguration(signConfigFile);

        loadMessagesFromConfig();
        loadSignTextFromConfig();
        loadCreationWords();

        freePriceWord = signConfig.getString("sign_text.zeroPrice");
    }

    public static String getCreationWord(String type){
        return creationWords.get(type);
    }

    public static String getFreePriceWord(){
        return freePriceWord;
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
            unformattedMessage = unformattedMessage.replace("[shop type]", "" + ShopMessage.getCreationWord(shop.getType().toString().toUpperCase())); //sub in user's word for SELL,BUY,BARTER
        }
        if(player != null) {
            unformattedMessage = unformattedMessage.replace("[user]", "" + player.getName());
            unformattedMessage = unformattedMessage.replace("[build limit]", "" + Shop.getPlugin().getShopListener().getBuildLimit(player));
        }
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
            messageMap.put(type.toString() + "_user", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".user"));
            messageMap.put(type.toString() + "_owner", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".owner"));

            messageMap.put(type.toString() + "_create", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".create"));
            messageMap.put(type.toString() + "_destroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".destroy"));
            messageMap.put(type.toString() + "_opDestroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opDestroy"));
            messageMap.put(type.toString() + "_opOpen", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opOpen"));

            messageMap.put(type.toString() + "_shopNoStock", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoStock"));
            messageMap.put(type.toString() + "_shopNoSpace", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoSpace"));
            messageMap.put(type.toString() + "_playerNoStock", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoStock"));
            messageMap.put(type.toString() + "_playerNoSpace", chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoSpace"));
        }

        messageMap.put("permission_use", chatConfig.getString("permission.use"));
        messageMap.put("permission_create", chatConfig.getString("permission.create"));
        messageMap.put("permission_destroy", chatConfig.getString("permission.destroy"));
        messageMap.put("permission_buildLimit", chatConfig.getString("permission.buildLimit"));

        messageMap.put("interactionIssue_line2", chatConfig.getString("interaction_issue.createLine2"));
        messageMap.put("interactionIssue_line3", chatConfig.getString("interaction_issue.createLine3"));
        messageMap.put("interactionIssue_noItem", chatConfig.getString("interaction_issue.createNoItem"));
        messageMap.put("interactionIssue_direction", chatConfig.getString("interaction_issue.createDirection"));
        messageMap.put("interactionIssue_sameItem", chatConfig.getString("interaction_issue.createSameItem"));
        messageMap.put("interactionIssue_displayRoom", chatConfig.getString("interaction_issue.createDisplayRoom"));
        messageMap.put("interactionIssue_createInsufficientFunds", chatConfig.getString("interaction_issue.createInsufficientFunds"));
        messageMap.put("interactionIssue_destroyInsufficientFunds", chatConfig.getString("interaction_issue.destroyInsufficientFunds"));
        messageMap.put("interactionIssue_initialize", chatConfig.getString("interaction_issue.initializeOtherShop"));
        messageMap.put("interactionIssue_destroyChest", chatConfig.getString("interaction_issue.destroyChest"));
        messageMap.put("interactionIssue_useOwnShop", chatConfig.getString("interaction_issue.useOwnShop"));
        messageMap.put("interactionIssue_adminOpen", chatConfig.getString("interaction_issue.adminOpen"));
        messageMap.put("interactionIssue_worldBlacklist", chatConfig.getString("interaction_issue.worldBlacklist"));

    }

    private void loadSignTextFromConfig() {
        Set<String> allTypes = signConfig.getConfigurationSection("sign_text").getKeys(false);
        for (String typeString : allTypes) {

            ShopType type = null;
            try { type = ShopType.valueOf(typeString);}
            catch (IllegalArgumentException e){}

            if (type != null) {
                String[] normalLines = new String[4];
                Set<String> normalLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".normal").getKeys(false);

                int i = 0;
                for (String number : normalLineNumbers) {
                    String message = signConfig.getString("sign_text." + typeString + ".normal." + number);
                    if (message == null)
                        normalLines[i] = "";
                    else
                        normalLines[i] = message;
                    i++;
                }

                this.shopSignTextMap.put(type.toString() + "_normal", normalLines);

                String[] adminLines = new String[4];
                Set<String> adminLineNumbers = signConfig.getConfigurationSection("sign_text." + typeString + ".admin").getKeys(false);

                i = 0;
                for (String number : adminLineNumbers) {
                    String message = signConfig.getString("sign_text." + typeString + ".admin." + number);
                    if (message == null)
                        adminLines[i] = "";
                    else
                        adminLines[i] = message;
                    i++;
                }

                this.shopSignTextMap.put(type.toString() + "_admin", adminLines);
            }
        }
    }

    private void loadCreationWords(){
        String shopString = signConfig.getString("sign_creation.SHOP");
        if(shopString != null)
            creationWords.put("SHOP", shopString.toLowerCase());
        else
            creationWords.put("SHOP", "[shop]");

        String sellString = signConfig.getString("sign_creation.SELL");
        if(sellString != null)
            creationWords.put("SELL", sellString.toLowerCase());
        else
            creationWords.put("SELL", "sell");

        String buyString = signConfig.getString("sign_creation.BUY");
        if(buyString != null)
            creationWords.put("BUY", buyString.toLowerCase());
        else
            creationWords.put("BUY", "buy");

        String barterString = signConfig.getString("sign_creation.BARTER");
        if(barterString != null)
            creationWords.put("BARTER", barterString.toLowerCase());
        else
            creationWords.put("BARTER", "barter");

        String adminString = signConfig.getString("sign_creation.ADMIN");
        if(adminString != null)
            creationWords.put("ADMIN", adminString.toLowerCase());
        else
            creationWords.put("ADMIN", "admin");
    }
}