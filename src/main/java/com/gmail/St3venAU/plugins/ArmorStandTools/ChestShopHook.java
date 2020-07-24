package com.gmail.St3venAU.plugins.ArmorStandTools;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChestShopHook implements Listener {

    public ChestShopHook(Main plugin) {
        PreTransactionEvent.getHandlerList().unregister(plugin); // Avoid multiple registers
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPreTransactionEvent(PreTransactionEvent event) {
        if (event.getTransactionType() == TransactionEvent.TransactionType.BUY) return;

        if (ArmorStandTool.get(event.getClient()) != null)
            event.setCancelled(PreTransactionEvent.TransactionOutcome.OTHER);
    }

}
