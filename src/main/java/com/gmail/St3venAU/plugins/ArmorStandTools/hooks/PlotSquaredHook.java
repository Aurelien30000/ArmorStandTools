package com.gmail.st3venau.plugins.armorstandtools.hooks;

import com.gmail.st3venau.plugins.armorstandtools.AST;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlotSquaredHook extends Hook {

    public static PlotAPI api;

    public PlotSquaredHook(AST AST) {
        super(AST, "PlotSquared");
    }

    public static boolean isPlotWorld(Location l) {
        return l.getWorld() != null && api.getPlotSquared().getPlotAreaManager().hasPlotArea(l.getWorld().getName());
    }

    public static Boolean checkPermission(Player p, Location l) {
        if (l.getWorld() == null)
            return null;
        final com.plotsquared.core.location.Location plotLocation = com.plotsquared.core.location.Location.at(l.getWorld().getName(), BlockVector3.at(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
        final PlotArea plotArea = plotLocation.getPlotArea();
        if (plotArea == null) {
            return p.hasPermission("plots.admin.build.road");
        }
        final Plot plot = plotArea.getPlot(plotLocation);
        final PlotPlayer<?> pp = api.wrapPlayer(p.getUniqueId());
        if (pp == null)
            return null;
        if (plot == null) {
            return pp.hasPermission("plots.admin.build.road");
        }
        final UUID uuid = pp.getUUID();
        return plot.isAdded(uuid) || pp.hasPermission("plots.admin.build.other");
    }

    @Override
    public void register() {
        api = new PlotAPI();
    }

}
