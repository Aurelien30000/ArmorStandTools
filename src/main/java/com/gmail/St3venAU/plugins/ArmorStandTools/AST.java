package com.gmail.st3venau.plugins.armorstandtools;

import com.gmail.st3venau.plugins.armorstandtools.hooks.PlotSquaredHook;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AST extends JavaPlugin {

    private static Object WG_AST_FLAG;

    final static Map<UUID, ArmorStandTool> activeTool = new HashMap<>();
    final static Map<UUID, ArmorStand> selectedArmorStand = new HashMap<>();
    final static Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    static AST plugin;
    static String nmsVersion;

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                // Need to do this with reflection for some reason, otherwise plugin load fails when worldguard is not present, even though this code block is not actually executed unless worldguard is present ???
                WG_AST_FLAG = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag").getConstructor(String.class, boolean.class).newInstance("ast", true);
                final Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                final Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(worldGuardClass);
                final Object flagRegistry = worldGuardClass.getMethod("getFlagRegistry").invoke(worldGuard);
                flagRegistry.getClass().getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag")).invoke(flagRegistry, WG_AST_FLAG);
                getLogger().info("Registered custom WorldGuard flag: ast");
            } catch (Exception e) {
                getLogger().warning("Failed to register custom WorldGuard flag");
            }
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        nmsVersion = getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        if (nmsVersion.startsWith("v1_4") ||
                nmsVersion.startsWith("v1_5") ||
                nmsVersion.startsWith("v1_6") ||
                nmsVersion.startsWith("v1_7") ||
                nmsVersion.startsWith("v1_8") ||
                nmsVersion.startsWith("v1_9") ||
                nmsVersion.startsWith("v1_10") ||
                nmsVersion.startsWith("v1_11") ||
                nmsVersion.startsWith("v1_12") ||
                nmsVersion.startsWith("v1_13") ||
                nmsVersion.startsWith("v1_14") ||
                nmsVersion.startsWith("v1_15") ||
                nmsVersion.startsWith("v1_16")) {
            getLogger().warning("This Craftbukkit/Spigot version is not supported. Craftbukkit/Spigot 1.17+ required. Loading plugin failed.");
            setEnabled(false);
            return;
        }
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        final Commands cmds = new Commands();
        PluginCommand command = getCommand("astools");
        if (command != null) {
            command.setExecutor(cmds);
        }
        command = getCommand("ascmd");
        if (command != null) {
            command.setExecutor(cmds);
            command.setTabCompleter(cmds);
        }

        Config.reload();
        ArmorStandGUI.init();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeTool.keySet()) {
                    final Player p = getServer().getPlayer(uuid);
                    final ArmorStandTool tool;
                    if (p != null && p.isOnline()
                            && (tool = activeTool.get(uuid)) != null && selectedArmorStand.containsKey(uuid)) {
                        tool.use(p, selectedArmorStand.get(uuid));
                    }
                }
            }
        }.runTaskTimer(this, 5L, 5L);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        for (UUID uuid : activeTool.keySet()) {
            if (ArmorStandTool.MOVE != activeTool.get(uuid))
                continue;
            final ArmorStand as = selectedArmorStand.get(uuid);
            if (as != null && !as.isDead()) {
                returnArmorStand(as);
                selectedArmorStand.remove(uuid);
                activeTool.remove(uuid);
            }
        }

        for (UUID uuid : savedInventories.keySet()) {
            final Player player = getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreInventory(player);
            }
        }
        savedInventories.clear();
    }

    static void returnArmorStand(ArmorStand as) {
        if (as == null)
            return;
        if (as.hasMetadata("clone")) {
            as.remove();
            return;
        }
        if (as.hasMetadata("startLoc")) {
            for (MetadataValue metaData : as.getMetadata("startLoc")) {
                if (metaData.getOwningPlugin() == plugin) {
                    final Location l = (Location) metaData.value();
                    if (l != null) {
                        as.teleport(l);
                        as.removeMetadata("startLoc", plugin);
                        return;
                    }
                }
            }
        }
        as.remove();
    }

    private static void removeAllTools(Player p) {
        final PlayerInventory i = p.getInventory();
        for (ArmorStandTool t : ArmorStandTool.values()) {
            i.remove(t.getItem());
        }
    }

    void saveInventoryAndClear(Player p) {
        final ItemStack[] inv = p.getInventory().getContents().clone();
        savedInventories.put(p.getUniqueId(), inv);
        p.getInventory().clear();
    }

    static void restoreInventory(Player p) {
        removeAllTools(p);
        final UUID uuid = p.getUniqueId();
        final ItemStack[] savedInv = savedInventories.get(uuid);
        if (savedInv == null)
            return;
        final PlayerInventory plrInv = p.getInventory();
        final ItemStack[] newItems = plrInv.getContents().clone();
        plrInv.setContents(savedInv);
        savedInventories.remove(uuid);
        for (ItemStack i : newItems) {
            if (i == null)
                continue;
            final Map<Integer, ItemStack> couldntFit = plrInv.addItem(i);
            for (ItemStack is : couldntFit.values()) {
                p.getWorld().dropItem(p.getLocation(), is);
            }
        }
        p.sendMessage(ChatColor.GREEN + Config.invReturned);
    }

    static ArmorStand getCarryingArmorStand(Player p) {
        final UUID uuid = p.getUniqueId();
        return ArmorStandTool.MOVE == AST.activeTool.get(uuid) ? AST.selectedArmorStand.get(uuid) : null;
    }

    static void pickUpArmorStand(ArmorStand as, Player p, boolean newlySummoned) {
        final UUID uuid = p.getUniqueId();
        final ArmorStand carrying = getCarryingArmorStand(p);
        if (carrying != null && !carrying.isDead()) {
            returnArmorStand(carrying);
        }
        activeTool.put(uuid, ArmorStandTool.MOVE);
        selectedArmorStand.put(uuid, as);
        if (newlySummoned)
            return;
        as.setMetadata("startLoc", new FixedMetadataValue(AST.plugin, as.getLocation()));
    }

    static void setName(Player p, ArmorStand as) {
        final Block b = findAnAirBlock(p.getLocation());
        if (b == null) {
            p.sendMessage(ChatColor.RED + Config.noAirError);
            return;
        }
        b.setType(Material.OAK_SIGN);
        Utils.openSign(p, b);
        b.setMetadata("armorStand", new FixedMetadataValue(AST.plugin, as.getUniqueId()));
        b.setMetadata("setName", new FixedMetadataValue(AST.plugin, true));
    }

    static void setPlayerSkull(Player p, ArmorStand as) {
        final Block b = findAnAirBlock(p.getLocation());
        if (b == null) {
            p.sendMessage(ChatColor.RED + Config.noAirError);
            return;
        }
        b.setType(Material.OAK_SIGN);
        Utils.openSign(p, b);
        b.setMetadata("armorStand", new FixedMetadataValue(AST.plugin, as.getUniqueId()));
        b.setMetadata("setSkull", new FixedMetadataValue(AST.plugin, true));
    }

    private static Block findAnAirBlock(Location l) {
        while (l.getY() < 255 && l.getBlock().getType() != Material.AIR) {
            l.add(0, 1, 0);
        }
        return l.getY() < 255 && l.getBlock().getType() == Material.AIR ? l.getBlock() : null;
    }

    static boolean checkBlockPermission(Player p, Block b) {
        if (b == null)
            return true;
        if (PlotSquaredHook.API != null) {
            final Location l = b.getLocation();
            if (PlotSquaredHook.isPlotWorld(l)) {
                return PlotSquaredHook.checkPermission(p, l);
            }
        }
        if (Config.worldGuardPlugin != null) {
            if (!Utils.hasPermissionNode(p, "astools.bypass-wg-flag") && !getWorldGuardAstFlag(b.getLocation())) {
                return false;
            }
            if (!Config.worldGuardPlugin.createProtectionQuery().testBlockBreak(p, b)) {
                return false;
            }
        }
        final BlockBreakEvent breakEvent = new BlockBreakEvent(b, p);
        Bukkit.getServer().getPluginManager().callEvent(breakEvent);
        return !breakEvent.isCancelled();
    }

    private static boolean getWorldGuardAstFlag(Location l) {
        if (l != null && l.getWorld() != null) {
            final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            final RegionManager regions = regionContainer.get(BukkitAdapter.adapt(l.getWorld()));
            if (regions == null)
                return true;
            return regions.getApplicableRegions(BukkitAdapter.asBlockVector(l)).testState(null, (StateFlag) WG_AST_FLAG);
        } else {
            return false;
        }
    }

    static boolean playerHasPermission(Player p, Block b, ArmorStandTool tool) {
        final String permNode = tool == null ? "astools.use" : tool.getPermission();
        final boolean enabled = tool == null || tool.isEnabled();
        final boolean hasNode = Utils.hasPermissionNode(p, permNode);
        final boolean blockPerm = checkBlockPermission(p, b);
        if (Config.showDebugMessages) {
            AST.debug("Plr: " + p.getName() + ", Tool: " + tool + ", Tool En: " + enabled + ", Perm: " + permNode + ", Has Perm: " + hasNode + ", Location Perm: " + blockPerm);
        }
        return enabled && hasNode && blockPerm;
    }

    static void debug(String msg) {
        if (!Config.showDebugMessages)
            return;
        Bukkit.getLogger().log(Level.INFO, "[AST DEBUG] " + msg);
    }

}