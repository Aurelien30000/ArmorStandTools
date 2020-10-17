package com.gmail.St3venAU.plugins.ArmorStandTools.hooks;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.gmail.St3venAU.plugins.ArmorStandTools.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChestShopHook extends Hook implements Listener {

    public ChestShopHook(Main plugin) {
        super(plugin, "ChestShop");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPreTransactionEvent(PreTransactionEvent event) {
        if (plugin.savedInventories.containsKey(event.getClient().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void register() {
        PreTransactionEvent.getHandlerList().unregister(plugin); // Avoid multiple registers
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

}
