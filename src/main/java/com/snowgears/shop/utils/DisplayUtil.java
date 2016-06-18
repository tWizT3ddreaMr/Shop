package com.snowgears.shop.utils;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class DisplayUtil {

    //main groups of angles
    public static EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0));
    public static EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100), Math.toRadians(-90), Math.toRadians(0));

    //very specific case angles
    public static EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80), Math.toRadians(-90), Math.toRadians(0));
    public static EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(5), Math.toRadians(-10));

    //this spawns an armorstand at a location, with the item on it
    public static ArmorStand createDisplay(ItemStack itemStack, Location blockLocation, BlockFace facing){
        ItemType itemType = getItemType(itemStack);

        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, itemType);
        ArmorStand stand = null;

        switch (itemType){
            case HEAD:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setHelmet(itemStack);
                stand.setSmall(true);
                break;
            case BODY:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.setChestplate(itemStack);
                break;
            case LEGS:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.setLeggings(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case FEET:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.setBoots(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case HAND:
                stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setItemInHand(itemStack);
                stand.setRightArmPose(getArmAngle(itemStack));
                break;
        }

        if(stand != null) {
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setBasePlate(false);
        }

        return stand;
    }

    public static ItemType getItemType(ItemStack itemStack){

        Material type = itemStack.getType();
        String sType = type.toString().toUpperCase();

        if(sType.contains("_HELMET") || type == Material.SKULL_ITEM || type.isBlock()){
            return ItemType.HEAD;
        }
        else if(sType.contains("_CHESTPLATE")){ //TODO  || type == Material.ELYTRA (when Minecraft lets players put ELYTRA on ArmorStands)
            return ItemType.BODY;
        }
        else if(sType.contains("_LEGGINGS")){
            return ItemType.LEGS;
        }
        else if(sType.contains("_BOOTS")){
            return ItemType.FEET;
        }
        return ItemType.HAND;
    }

    public static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, ItemType itemType){

        Location standLocation = null;
        switch (itemType) {
            case HEAD:
                standLocation = blockLocation.clone().add(0.5, -.7, 0.5);
                break;
            case BODY:
                standLocation = blockLocation.clone().add(0.5, -0.3, 0.5);
                break;
            case LEGS:
                standLocation = blockLocation.clone().add(0.5, -0.1 ,0.5);
                break;
            case FEET:
                standLocation = blockLocation.clone().add(0.5, 0.05 ,0.5);
                break;
            case HAND:
                standLocation = blockLocation;

                if(isTool(material)){
                    double rodOffset = 0.1;
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.7, -1.3, 0.6);
                            if(material == Material.FISHING_ROD)
                                standLocation = standLocation.add(rodOffset, 0, 0);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
                            if(material == Material.FISHING_ROD)
                                standLocation = standLocation.add(0, 0, rodOffset);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
                            if(material == Material.FISHING_ROD)
                                standLocation = standLocation.add(-rodOffset, 0, 0);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
                            if(material == Material.FISHING_ROD)
                                standLocation = standLocation.add(0, 0, -rodOffset);
                            break;
                    }
                }
                else if(material == Material.BOW){
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(1, -0.8, 0.85);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.15, -0.8, 1);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0, -0.8, 0.15);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.85, -0.8, 0);
                            break;
                    }
                }
                //the material is a simple, default item
                else{
                    switch (facing){
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.15, -1.4, 1.075);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(-0.075, -1.4, 0.15);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.87, -1.4, -0.075);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(1.07, -1.4, 0.85);
                            break;
                    }
                }
                break;
        }

        //make the stand face the correct direction when it spawns
        standLocation.setYaw(blockfaceToYaw(facing));
        //fences and bows are always 90 degrees off
        if(isFence(material) || material == Material.BOW){
            standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
        }

        //material is an item
        return standLocation;
    }

    public static EulerAngle getArmAngle(ItemStack itemStack){

        Material material = itemStack.getType();

        if(isTool(material) && material != Material.FISHING_ROD){
            return toolAngle;
        }
        else if(material == Material.BOW){
            return bowAngle;
        }
        else if(material == Material.FISHING_ROD){
            return rodAngle;
        }
        return itemAngle;
    }

    /**
     * Converts a BlockFace direction to a yaw (float) value
     * Return:
     * - float: the yaw value of the BlockFace direction provided
     */
    public static float blockfaceToYaw(BlockFace bf) {
        if (bf.equals(BlockFace.SOUTH))
            return 0F;
        else if (bf.equals(BlockFace.WEST))
            return 90F;
        else if (bf.equals(BlockFace.NORTH))
            return 180F;
        else if (bf.equals(BlockFace.EAST))
            return 270F;
        return 0F;
    }

    public static boolean isBlock(Material material){
        if(material.isBlock() && !material.toString().toUpperCase().contains("CHEST"))
            return true;
        return false;
    }

    public static boolean isHeldNonItem(Material material){
        String sType = material.toString().toUpperCase();
        if(isTool(material) || sType.contains("SWORD") || material == Material.BOW || material == Material.FISHING_ROD)
            return true;
        return false;
    }

    public static boolean isTool(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("_AXE") || sMaterial.contains("_HOE") || sMaterial.contains("_PICKAXE")
                || sMaterial.contains("_SPADE") || sMaterial.contains("_SWORD") || material == Material.FISHING_ROD);
    }

    public static boolean isChest(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("CHEST") && !sMaterial.contains("CHESTPLATE"));
    }

    public static boolean isFence(Material material){
        String sMaterial = material.toString().toUpperCase();
        return (sMaterial.contains("FENCE") && (material != Material.IRON_FENCE));
    }

    public static boolean isArmor(Material material){
        String sMaterial = material.toString().toUpperCase();
        if(material == Material.SKULL || material == Material.SKULL_ITEM){
            return true;
        }
        else if(sMaterial.contains("_BOOTS") || sMaterial.contains("_CHESTPLATE") || sMaterial.contains("_LEGGINGS") || sMaterial.contains("HELMET")){
            return true;
        }
        return false;
    }

    public static BlockFace nextFace(BlockFace face){
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
        BlockFace direction = null;
        if(face == faces[faces.length-1])
            direction = faces[0];
        else{
            for(int i=0; i<faces.length; i++){
                if(face == faces[i]){
                    direction = faces[i+1];
                    break;
                }
            }
        }
        return direction;
    }

    public static enum ItemType {
        HEAD, BODY, LEGS, FEET, HAND;
    }
}
