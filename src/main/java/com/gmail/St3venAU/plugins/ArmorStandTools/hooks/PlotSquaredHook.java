package com.gmail.St3venAU.plugins.ArmorStandTools.hooks;

import com.gmail.St3venAU.plugins.ArmorStandTools.Main;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.api.PlotAPI;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlotSquaredHook extends Hook {

    public static PlotAPI api;
    private static Main plugin;

    public PlotSquaredHook(Main main) {
        super(main, "PlotSquared");
        plugin = main;
    }

    public static boolean isPlotWorld(Location loc) {
        return api.getPlotSquared().hasPlotArea(loc.getWorld().getName());
    }

    public static boolean checkPermission(Player player, Location location) {
        final com.plotsquared.core.location.Location plotLocation = BukkitUtil.getLocation(location);
        final PlotArea plotArea = plotLocation.getPlotArea();
        if (plotArea == null) {
            plugin.debug("plots.admin.build.road: " + player.hasPermission("plots.admin.build.road"));
            return player.hasPermission("plots.admin.build.road");
        }
        final Plot plot = plotArea.getPlot(plotLocation);
        final PlotPlayer<?> pp = PlotPlayer.wrap(player);
        plugin.debug("Plot: " + plot);
        if (plot == null) {
            plugin.debug("plots.admin.build.road: " + pp.hasPermission("plots.admin.build.road"));
            return pp.hasPermission("plots.admin.build.road");
        }
        final UUID uuid = pp.getUUID();
        plugin.debug("plot.isAdded: " + plot.isAdded(uuid));
        plugin.debug("plots.admin.build.other: " + pp.hasPermission("plots.admin.build.other"));
        return plot.isAdded(uuid) || pp.hasPermission("plots.admin.build.other");
    }

    @Override
    public void register() {
        PlotSquaredHook.api = new PlotAPI();
    }

}
