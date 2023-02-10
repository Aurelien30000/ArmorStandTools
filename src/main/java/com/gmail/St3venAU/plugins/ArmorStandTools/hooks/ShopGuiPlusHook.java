package com.gmail.St3venAU.plugins.ArmorStandTools.hooks;

import com.gmail.St3venAU.plugins.ArmorStandTools.AST;
import net.brcdev.shopgui.event.ShopPreTransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ShopGuiPlusHook extends Hook implements Listener {

    public ShopGuiPlusHook(AST ast) {
        super(ast, "ShopGUIPlus");
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopPreTransactionEvent(ShopPreTransactionEvent event) {
        if (AST.savedInventories.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void register() {
        ShopPreTransactionEvent.getHandlerList().unregister(plugin); // Avoid multiple registers
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

}