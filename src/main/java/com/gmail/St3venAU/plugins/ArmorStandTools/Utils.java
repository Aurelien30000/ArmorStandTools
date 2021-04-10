package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;

public class Utils {

    private static DecimalFormat twoDec;

    public static boolean containsItems(Collection<ItemStack> items) {
        for (ItemStack i : items) {
            if (ArmorStandTool.get(i) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPermissionNode(Player player, String perm, boolean ignoreOp) {
        if (!ignoreOp && ((player == null) || player.isOp())) {
            return true;
        }
        if (player.hasPermission(perm)) {
            return true;
        }
        final String[] nodes = perm.split("\\.");
        final StringBuilder n = new StringBuilder();
        for (int i = 0; i < (nodes.length - 1); i++) {
            n.append(nodes[i]).append(".");
            if (player.hasPermission(n + "*")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPermissionNode(Player player, String perm) {
        return hasPermissionNode(player, perm, false);
    }

    public static boolean hasAnyTools(Player p) {
        for (ItemStack i : p.getInventory()) {
            if (ArmorStandTool.isTool(i)) {
                return true;
            }
        }
        return false;
    }

    public static Location getLocationFacing(Location loc) {
        final Location l = loc.clone();
        final Vector v = l.getDirection();
        v.setY(0);
        v.multiply(3);
        l.add(v);
        l.setYaw(l.getYaw() + 180);
        boolean ok = false;
        for (int n = 0; n < 5; n++) {
            if (l.getBlock().getType().isSolid()) {
                l.add(0, 1, 0);
            } else {
                ok = true;
                break;
            }
        }
        if (!ok) {
            l.subtract(0, 5, 0);
        }
        return l;
    }

    public static void cycleInventory(Player p) {
        final Inventory i = p.getInventory();
        for (int n = 0; n < 9; n++) {
            final ItemStack temp = i.getItem(n);
            i.setItem(n, i.getItem(27 + n));
            i.setItem(27 + n, i.getItem(18 + n));
            i.setItem(18 + n, i.getItem(9 + n));
            i.setItem(9 + n, temp);
        }
        p.updateInventory();
    }

    public static String angle(double d) {
        return twoDec(d * 180.0 / Math.PI);
    }

    public static String twoDec(double d) {
        if (twoDec == null) {
            twoDec = new DecimalFormat("0.0#");
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            twoDec.setDecimalFormatSymbols(symbols);
        }
        return twoDec.format(d);
    }

    public static Block findAnAirBlock(Location l) {
        while (l.getY() < 255 && l.getBlock().getType() != Material.AIR) {
            l.add(0, 1, 0);
        }
        return l.getY() < 255 && l.getBlock().getType() == Material.AIR ? l.getBlock() : null;
    }

    public static ItemStack setLore(ItemStack is, String... lore) {
        final ItemMeta meta = is.getItemMeta();
        meta.setLore(Arrays.asList(lore));
        is.setItemMeta(meta);
        return is;
    }

    public static boolean toggleInvulnerability(ArmorStand as) {
        final boolean inv = !as.isInvulnerable();
        as.setInvulnerable(inv);
        return inv;
    }

    public static void actionBarMsg(Player p, String msg) {
        p.sendTitle("", msg, 0, 70, 0);
    }

    public static boolean toggleGlow(ArmorStand as) {
        boolean glowing = !as.isGlowing();
        as.setGlowing(glowing);
        return glowing;
    }

}
