package com.gmail.St3venAU.plugins.ArmorStandTools.hooks;

import com.gmail.St3venAU.plugins.ArmorStandTools.AST;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public abstract class Hook {

    protected final AST plugin;
    protected final String name;

    public Hook(AST plugin, String name) {
        this.plugin = plugin;
        this.name = name;

        if (checkCompatbility())
            register();
    }

    public boolean checkCompatbility() {
        final Plugin hook = plugin.getServer().getPluginManager().getPlugin(name);

        if (hook != null && hook.isEnabled()) {
            try {
                plugin.getLogger().log(Level.INFO, name + " plugin was found. Support enabled.");
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                plugin.getLogger().log(Level.WARNING, name + " plugin was found, but there was an error initializing its support.");
                return false;
            }
        } else {
            plugin.getLogger().log(Level.INFO, name + " plugin not found. Continuing without its support.");
            return false;
        }
    }

    public abstract void register();

}
