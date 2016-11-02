package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class ArmorStandListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public ArmorStandListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}