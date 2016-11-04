package com.snowgears.shop.util;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    public static boolean canCreateShop(Player player, Location location){
        try {
            if(Shop.getPlugin().hookWorldGuard()) {
                ApplicableRegionSet set = WGBukkit.getPlugin().getRegionManager(player.getWorld()).getApplicableRegions(location);
                if (set.queryState(null, DefaultFlag.ENABLE_SHOP) == StateFlag.State.ALLOW) {
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
            if(Shop.getPlugin().hookWorldGuard()) {
                ApplicableRegionSet set = WGBukkit.getPlugin().getRegionManager(player.getWorld()).getApplicableRegions(location);
                if (set.queryState(null, DefaultFlag.USE) == StateFlag.State.DENY) {
                    return false;
                }
                if(player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
                    return true;
                }
                return false;
            }
        } catch(NoClassDefFoundError e){}
        return true;
    }
}
