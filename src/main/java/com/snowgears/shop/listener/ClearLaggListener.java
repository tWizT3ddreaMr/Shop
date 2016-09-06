package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.Display;
import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Iterator;

public class ClearLaggListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public ClearLaggListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onClearLagg(EntityRemoveEvent event) {
        Iterator<Entity> iterator = event.getEntityList().iterator();
        while (iterator.hasNext()) {
            Entity e = iterator.next();
            if (Display.isDisplay(e)) {
                iterator.remove();
            }
        }
    }
}