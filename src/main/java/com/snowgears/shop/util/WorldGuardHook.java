package com.snowgears.shop.util;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    public static boolean canCreateShop(Player player, Location location){
        try {
            LocalPlayer localPlayer = WGBukkit.getPlugin().wrapPlayer(player);

            if(Shop.getPlugin().hookWorldGuard()) {
                ApplicableRegionSet set = WGBukkit.getPlugin().getRegionManager(player.getWorld()).getApplicableRegions(location);
                if (set.queryState(localPlayer, DefaultFlag.ENABLE_SHOP) == StateFlag.State.ALLOW) {
                    return true;
                }
                if(player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
                    return true;
                }
                return false;
            }
        } catch(NoClassDefFoundError e){}
        return true;
    }

    public static boolean canUseShop(Player player, Location location){
        try {
            LocalPlayer localPlayer = WGBukkit.getPlugin().wrapPlayer(player);

            if(Shop.getPlugin().hookWorldGuard()) {
                if(player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
                    return true;
                }

                ApplicableRegionSet set = WGBukkit.getPlugin().getRegionManager(player.getWorld()).getApplicableRegions(location);

                if (set.queryState(localPlayer, DefaultFlag.USE) == StateFlag.State.DENY) {
                    return false;
                }

                return true;
            }
        } catch(NoClassDefFoundError e){}
        return true;
    }
}
