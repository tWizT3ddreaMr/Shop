package com.snowgears.shop;

import com.snowgears.shop.utils.UtilMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;


public class ShopHandler {

    public Shop plugin = Shop.getPlugin();

    private HashMap<Location, ShopObject> allShops = new HashMap<Location, ShopObject>();
    private ArrayList<Material> shopMaterials = new ArrayList<Material>();

    public ShopHandler(Shop instance) {
        plugin = instance;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                loadShops();
            }
        }, 1L);

        shopMaterials.add(Material.CHEST);
        shopMaterials.add(Material.TRAPPED_CHEST);
        shopMaterials.add(Material.ENDER_CHEST);
    }

    public ShopObject getShop(Location loc) {
        return allShops.get(loc);
    }

    public ShopObject getShopByChest(Block shopChest) {
        if (this.isChest(shopChest)) {
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(shopChest);
            Block signBlock = shopChest.getRelative(chestFacing);
            if(signBlock.getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) signBlock.getState().getData();
                if (chestFacing == sign.getFacing()) {
                    ShopObject shop = this.getShop(signBlock.getLocation());
                    if(shop != null)
                        return shop;
                }
            }
            return null;
        }
        return null;
    }

    public ShopObject getShopNearBlock(Block block){
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for(BlockFace face : faces){
            if(this.isChest(block.getRelative(face))){
                Block shopChest = block.getRelative(face);
                for(BlockFace newFace : faces){
                    if(shopChest.getRelative(newFace).getType() == Material.WALL_SIGN){
                        ShopObject shop = getShop(shopChest.getRelative(newFace).getLocation());
                        if(shop != null)
                            return shop;
                    }
                }
            }
        }
        return null;
    }

    public void addShop(ShopObject shop) {
        allShops.put(shop.getSignLocation(), shop);
    }

    //This method should only be used by ShopObject to delete
    public boolean removeShop(ShopObject shop) {
        if (allShops.containsKey(shop.getSignLocation())) {
            allShops.remove(shop.getSignLocation());
            return true;
        }
        return false;
    }

    public int getNumberOfShops() {
        return allShops.size();
    }

    public int getNumberOfShops(Player player) {
        int size = 0;
        for (ShopObject shop : allShops.values()) {
            if (shop.getOwnerUUID().equals(player.getUniqueId()))
                size++;
        }
        return size;
    }

    private ArrayList<ShopObject> orderedShopList() {
        ArrayList<ShopObject> list = new ArrayList<ShopObject>(allShops.values());
        Collections.sort(list, new Comparator<ShopObject>() {
            @Override
            public int compare(ShopObject o1, ShopObject o2) {
                //TODO for some reason there is a null pointer being thrown here
                //could have something to do with switching between online and offline mode
                return o1.getOwnerName().toLowerCase().compareTo(o2.getOwnerName().toLowerCase());
            }
        });
        return list;
    }

    public void refreshShopItems() {
        for (ShopObject shop : allShops.values()) {
            shop.getDisplay().spawn();
        }
    }

    public void saveShops() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            fileDirectory.mkdir();
        File shopFile = new File(fileDirectory + "/shops.yml");
        if (!shopFile.exists()) { // file doesn't exist
            try {
                shopFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //does exist, clear it for future saving
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(shopFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            writer.print("");
            writer.close();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        ArrayList<ShopObject> shopList = orderedShopList();

        String owner;

        int shopNumber = 1;
        for (int i = 0; i < shopList.size(); i++) {
            ShopObject s = shopList.get(i);
            //don't save shops that are not initialized with items
            if (s.isInitialized()) {
                owner = s.getOwnerName() + " (" + s.getOwnerUUID().toString() + ")";
                config.set("shops." + owner + "." + shopNumber + ".location", locationToString(s.getSignLocation()));
                config.set("shops." + owner + "." + shopNumber + ".price", s.getPrice());
                config.set("shops." + owner + "." + shopNumber + ".amount", s.getAmount());
                String type = "";
                if (s.isAdminShop())
                    type = "admin ";
                type = type + s.getType().toString();
                config.set("shops." + owner + "." + shopNumber + ".type", type);

                ItemStack itemStack = s.getItemStack();
                itemStack.setAmount(1);
                config.set("shops." + owner + "." + shopNumber + ".item", itemStack);

                if (s.getType() == ShopType.BARTER) {
                    ItemStack barterItemStack = s.getBarterItemStack();
                    barterItemStack.setAmount(1);
                    config.set("shops." + owner + "." + shopNumber + ".itemBarter", barterItemStack);
                }

                shopNumber++;
                //reset shop number if next shop has a different owner
                if (i < shopList.size() - 1) {
                    if (!(s.getOwnerUUID().equals(shopList.get(i + 1).getOwnerUUID())))
                        shopNumber = 1;
                }
            }
        }

        try {
            config.save(shopFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        plugin.getEnderChestHandler().saveEnderChests();
    }

    public void loadShops() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;
        File shopFile = new File(fileDirectory + "/shops.yml");
        if (!shopFile.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        backwardsCompatibleLoadShopsFromConfig(config);
    }

    private void loadShopsFromConfig(YamlConfiguration config) {

        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                Block b = signLoc.getBlock();
                if (b.getType() == Material.WALL_SIGN) {
                    org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();
                    //Location loc = b.getRelative(sign.getAttachedFace()).getLocation();
                    UUID owner = uidFromString(shopOwner);
                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                    boolean isAdmin = false;
                    if (type.contains("admin"))
                        isAdmin = true;
                    ShopType shopType = typeFromString(type);

                    ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                    ShopObject shop = new ShopObject(signLoc, owner, price, amount, isAdmin, shopType);
                    shop.setItemStack(itemStack);
                    if (shop.getType() == ShopType.BARTER) {
                        ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                        shop.setBarterItemStack(barterItemStack);
                    }
                    shop.updateSign();
                    this.addShop(shop);
                }
            }
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        String uidString = ownerString.substring(index + 1, ownerString.length() - 1);
        return UUID.fromString(uidString);
    }

    private ShopType typeFromString(String typeString) {
        if (typeString.contains("sell"))
            return ShopType.SELL;
        else if (typeString.contains("buy"))
            return ShopType.BUY;
        else
            return ShopType.BARTER;
    }

    public boolean isChest(Block b){
        return shopMaterials.contains(b.getType());
    }




    private void backwardsCompatibleLoadShopsFromConfig(YamlConfiguration config){
        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        boolean loadByOldConfig = false;
        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                if (itemStack == null)
                    loadByOldConfig = true;
                break;
            }
            break;
        }

        if(loadByOldConfig) {
            loadShopsFromOldConfig(config); //load as old
            saveShops(); //save as new
        }
        else
            //load normally
            loadShopsFromConfig(config);
    }


    //==============================================================================//
    //            OLD WAY OF LOADING SHOPS FROM CONFIG FOR TRANSFERRING             //
    //==============================================================================//

    private void loadShopsFromOldConfig(YamlConfiguration config) {

        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                Block b = signLoc.getBlock();
                if (b.getType() == Material.WALL_SIGN) {
                    org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();
                    Location loc = b.getRelative(sign.getAttachedFace()).getLocation();
                    UUID owner = uidFromString(shopOwner);
                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                    boolean isAdmin = false;
                    if (type.contains("admin"))
                        isAdmin = true;
                    ShopType shopType = typeFromString(type);

                    MaterialData itemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.data"));
                    ItemStack itemStack = new ItemStack(itemData.getItemType());
                    itemStack.setData(itemData);
                    short itemDurability = (short) (config.getInt("shops." + shopOwner + "." + shopNumber + ".item.durability"));
                    itemStack.setDurability(itemDurability);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if(itemMeta instanceof LeatherArmorMeta){
                        if(config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null)
                            ((LeatherArmorMeta)itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                    }
                    String itemName = config.getString("shops." + shopOwner + "." + shopNumber + ".item.name");
                    if (!itemName.isEmpty())
                        itemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".item.name"));
                    List<String> itemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore"));
                    if (itemLore.size() > 1)
                        itemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore")));
                    itemStack.setItemMeta(itemMeta);
                    itemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.enchantments")));

                    ItemStack barterItemStack = null;
                    if (shopType == ShopType.BARTER) {
                        MaterialData barterItemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.data"));
                        barterItemStack = new ItemStack(barterItemData.getItemType());
                        barterItemStack.setData(barterItemData);
                        short barterItemDurability = (short) (config.getInt("shops." + shopOwner + "." + shopNumber + ".itemBarter.durability"));
                        barterItemStack.setDurability(barterItemDurability);
                        ItemMeta barterItemMeta = barterItemStack.getItemMeta();
                        if(itemMeta instanceof LeatherArmorMeta){
                            if(config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null)
                                ((LeatherArmorMeta)itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                        }
                        String barterItemName = config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name");
                        if (!barterItemName.isEmpty())
                            barterItemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name"));
                        List<String> barterItemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore"));
                        if (barterItemLore.size() > 1)
                            barterItemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore")));
                        barterItemStack.setItemMeta(barterItemMeta);
                        barterItemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.enchantments")));
                    }

                    ShopObject shop = new ShopObject(signLoc, owner, price, amount, isAdmin, shopType);
                    shop.setItemStack(itemStack);
                    if (shop.getType() == ShopType.BARTER)
                        shop.setBarterItemStack(barterItemStack);
                    shop.updateSign();
                    this.addShop(shop);
                }
            }
        }
    }

    private List<String> loreFromString(String loreString) {
        loreString = loreString.substring(1, loreString.length() - 1); //get rid of []
        String[] loreParts = loreString.split(", ");
        return Arrays.asList(loreParts);
    }

    private HashMap<Enchantment, Integer> enchantmentsFromString(String enchantments) {
        HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
        enchantments = enchantments.substring(1, enchantments.length() - 1); //get rid of {}
        if (enchantments.isEmpty())
            return enchants;
        String[] enchantParts = enchantments.split(", ");
        for (String whole : enchantParts) {
            String[] pair = whole.split("=");
            enchants.put(Enchantment.getByName(pair[0]), Integer.parseInt(pair[1]));
        }
        return enchants;
    }

    private MaterialData dataFromString(String dataString) {
        int index = dataString.indexOf("(");
//		System.out.println(dataString.substring(0, index));
//		System.out.println(dataString.substring(index+1, dataString.length()-1));
        String materialString = dataString.substring(0, index);
        Material m = Material.getMaterial(materialString);
        int data = Integer.parseInt(dataString.substring(index + 1, dataString.length() - 1));

        return new MaterialData(m, (byte) data);
    }
}