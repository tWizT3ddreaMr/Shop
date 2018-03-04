package com.snowgears.shop;

import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ComboShop extends AbstractShop {

    private double priceBuy;
    private double priceSell;

    public ComboShop(Location signLoc, UUID player, double pri, double priSell, int amt, Boolean admin) {
        super(signLoc, player, pri, amt, admin);

        this.type = ShopType.COMBO;
        this.signLines = ShopMessage.getSignLines(this, this.type);
        this.priceBuy = pri;
        this.priceSell = priSell;
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        if(transactionType == ShopType.SELL){
            return executeSellTransaction(orders, player, isCheck);
        }
        else{
            return executeBuyTransaction(orders, player, isCheck);
        }
    }

    private TransactionError executeBuyTransaction(int orders, Player player, boolean isCheck){
        return TransactionError.NONE;
    }

    private TransactionError executeSellTransaction(int orders, Player player, boolean isCheck){
        return TransactionError.NONE;
    }

    public String getPriceSellString() {
        return Shop.getPlugin().getPriceString(this.priceSell, false);
    }

    public String getPriceSellPerItemString() {
        double pricePer = this.getPriceSell() / this.getAmount();
        return Shop.getPlugin().getPriceString(pricePer, true);
    }

    public String getPriceComboString() {
        return Shop.getPlugin().getPriceComboString(this.price, this.priceSell, false);
    }

    @Override
    public void printSalesInfo(Player player) {
        player.sendMessage("");

        String message = ShopMessage.getUnformattedMessage(ShopType.BUY.toString(), "descriptionItem");
        formatAndSendFancyMessage(message, player);
        player.sendMessage("");


        if(priceBuy != 0) {
            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPrice", this, player);
            player.sendMessage(message);

            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPricePerItem", this, player);
            player.sendMessage(message);
            player.sendMessage("");
        }

        if(priceSell != 0) {
            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionItem");
            formatAndSendFancyMessage(message, player);
            player.sendMessage("");

            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionPrice");
            message = message.replaceAll("price]", "priceSell]");
            message = ShopMessage.formatMessage(message, this, player, false);
            player.sendMessage(message);


            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionPricePerItem");
            message = message.replaceAll("price per item]", "price sell per item]");
            message = ShopMessage.formatMessage(message, this, player, false);
            player.sendMessage(message);
        }

        if(this.isAdmin()){
            message = ShopMessage.getMessage("description", "stockAdmin", this, player);
            player.sendMessage(message);
        }
        else {
            message = ShopMessage.getMessage("description", "stock", this, player);
            player.sendMessage(message);
        }

        return;
    }

    public double getPriceSell(){
        return priceSell;
    }
}
