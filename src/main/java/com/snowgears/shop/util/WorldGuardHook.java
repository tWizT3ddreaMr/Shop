package com.snowgears.shop.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    public static boolean canCreateShop(Player player, Location location){
        if(!Shop.getPlugin().hookWorldGuard()) {
            return true;
        }
        //THERE IS NO LONGER AN ALLOW SHOP FLAG IN WORLDGUARD (API 7.x) . Returning true for now
        return true;
    }

    public static boolean canUseShop(Player player, Location location){
        if(!Shop.getPlugin().hookWorldGuard())
            return true;
        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            if(Shop.getPlugin().hookWorldGuard()) {
                if(player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
                    return true;
                }

                RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionQuery query = container.createQuery();
                ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
                return set.testState(null, new StateFlag[] {Flags.USE });
            }
        } catch(NoClassDefFoundError e){}
        return true;
    }
}
