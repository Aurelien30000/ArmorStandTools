package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

class ArmorStandGUI implements Listener {

    private static final HashSet<Integer> inUse = new HashSet<>();
    private static final HashSet<Integer> invSlots = new HashSet<>();
    private static ItemStack filler;

    private Inventory i;
    private ArmorStand as;
    private Main plugin;

    ArmorStandGUI(Main plugin, ArmorStand as, Player p) {
        if (isInUse(as)) {
            p.sendMessage(ChatColor.RED + Config.guiInUse);
            return;
        }
        if (filler == null) {
            filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
            final ItemMeta im = filler.getItemMeta();
            im.setDisplayName(" ");
            filler.setItemMeta(im);
            invSlots.add(10);
            invSlots.add(12);
            invSlots.add(2);
            invSlots.add(11);
            invSlots.add(20);
            invSlots.add(29);
        }
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.as = as;
        String name = as.getCustomName();
        if (name == null) {
            name = Config.armorStand;
        } else if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        i = Bukkit.createInventory(null, 36, name);
        for (int slot = 0; slot < i.getSize(); slot++) {
            i.setItem(slot, filler);
        }
        for (ArmorStandTool tool : ArmorStandTool.values()) {
            if (tool.isForGui() && tool.isEnabled()) {
                i.setItem(tool.getSlot(), updateLore(tool));
            }
        }
        i.setItem(10, as.getItem(EquipmentSlot.HAND));
        i.setItem(12, as.getItem(EquipmentSlot.OFF_HAND));
        i.setItem(2, as.getItem(EquipmentSlot.HEAD));
        i.setItem(11, as.getItem(EquipmentSlot.CHEST));
        i.setItem(20, as.getItem(EquipmentSlot.LEGS));
        i.setItem(29, as.getItem(EquipmentSlot.FEET));
        inUse.add(as.getEntityId());
        p.openInventory(i);
    }

    static boolean isInUse(ArmorStand as) {
        return inUse.contains(as.getEntityId());
    }

    private ItemStack updateLore(ArmorStandTool tool) {
        final ItemStack item = tool.getItem();
        switch (tool) {
            case INVIS:
                return Utils.setLore(item, ChatColor.AQUA + Config.asVisible + ": " + (as.isVisible() ? (ChatColor.GREEN + Config.isTrue) : (ChatColor.RED + Config.isFalse)));
            case SIZE:
                return Utils.setLore(item, ChatColor.AQUA + Config.size + ": " + (as.isSmall() ? (ChatColor.BLUE + Config.small) : (ChatColor.GREEN + Config.normal)));
            case BASE:
                return Utils.setLore(item, ChatColor.AQUA + Config.basePlate + ": " + (as.hasBasePlate() ? (ChatColor.GREEN + Config.isOn) : (ChatColor.RED + Config.isOff)));
            case GRAV:
                return Utils.setLore(item, ChatColor.AQUA + Config.gravity + ": " + (as.hasGravity() ? (ChatColor.GREEN + Config.isOn) : (ChatColor.RED + Config.isOff)));
            case ARMS:
                return Utils.setLore(item, ChatColor.AQUA + Config.arms + ": " + (as.hasArms() ? (ChatColor.GREEN + Config.isOn) : (ChatColor.RED + Config.isOff)));
            case INVUL:
                return Utils.setLore(item, ChatColor.AQUA + Config.invul + ": " + (as.isInvulnerable() ? (ChatColor.GREEN + Config.isOn) : (ChatColor.RED + Config.isOff)));
            case SLOTS:
                return Utils.setLore(item, ChatColor.AQUA + Config.equip + ": " + (Main.nms.equipmentLocked(as) ? (ChatColor.GREEN + Config.locked) : (ChatColor.RED + Config.unLocked)));
            case NAME:
                return Utils.setLore(item, ChatColor.AQUA + Config.currently + ": " + (as.getCustomName() == null ? (ChatColor.BLUE + Config.none) : (ChatColor.GREEN + as.getCustomName())));
            case PHEAD:
                String name = plrHeadName(as);
                return Utils.setLore(item, ChatColor.AQUA + Config.currently + ": " + (name == null ? (ChatColor.BLUE + Config.none) : (ChatColor.GREEN + name)));
            case GLOW:
                return Utils.setLore(item, ChatColor.AQUA + Config.glow + ": " + (as.isGlowing() ? (ChatColor.GREEN + Config.isOn) : (ChatColor.RED + Config.isOff)));
            default:
                return item;
        }
    }

    private String plrHeadName(ArmorStand as) {
        if (as.getItem(EquipmentSlot.HEAD).getType() == Material.AIR) return null;
        if (!(as.getItem(EquipmentSlot.HEAD).getItemMeta() instanceof final SkullMeta meta)) return null;
        if (!meta.hasOwner()) return null;
        return meta.getOwningPlayer().getName();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(i)) return;
        HandlerList.unregisterAll(this);
        inUse.remove(as.getEntityId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(i)) return;
        final Player p = (Player) event.getWhoClicked();
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT || event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }
        final int slot = event.getRawSlot();
        if (slot > i.getSize()) return;
        if (invSlots.contains(slot)) {
            if (plugin.checkBlockPermission(p, as.getLocation().getBlock())) {
                updateInventory();
            } else {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + Config.wgNoPerm);
            }
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        final ArmorStandTool t = ArmorStandTool.get(event.getCurrentItem());
        if (t == null) return;
        if (!plugin.playerHasPermission(p, as.getLocation().getBlock(), t)) {
            p.sendMessage(ChatColor.RED + Config.generalNoPerm);
            return;
        }
        switch (t) {
            case INVIS:
                as.setVisible(!as.isVisible());
                Utils.actionBarMsg(p, Config.asVisible + ": " + (as.isVisible() ? Config.isTrue : Config.isFalse));
                break;
            case CLONE:
                p.closeInventory();
                plugin.pickUpArmorStand(Main.nms.clone(as), p, true);
                Utils.actionBarMsg(p, Config.carrying);
                break;
            case SAVE:
                if (Config.requireCreative && p.getGameMode() != GameMode.CREATIVE) {
                    p.sendMessage(ChatColor.RED + Config.creativeRequired);
                } else {
                    Main.nms.generateCmdBlock(p.getLocation(), as);
                    Utils.actionBarMsg(p, Config.cbCreated);
                }
                break;
            case SIZE:
                as.setSmall(!as.isSmall());
                Utils.actionBarMsg(p, Config.size + ": " + (as.isSmall() ? Config.small : Config.normal));
                break;
            case BASE:
                as.setBasePlate(!as.hasBasePlate());
                Utils.actionBarMsg(p, Config.basePlate + ": " + (as.hasBasePlate() ? Config.isOn : Config.isOff));
                break;
            case GRAV:
                as.setGravity(!as.hasGravity());
                Utils.actionBarMsg(p, Config.gravity + ": " + (as.hasGravity() ? Config.isOn : Config.isOff));
                break;
            case ARMS:
                as.setArms(!as.hasArms());
                Utils.actionBarMsg(p, Config.arms + ": " + (as.hasArms() ? Config.isOn : Config.isOff));
                break;
            case NAME:
                p.closeInventory();
                plugin.setName(p, as);
                break;
            case PHEAD:
                p.closeInventory();
                plugin.setPlayerSkull(p, as);
                break;
            case INVUL:
                Utils.actionBarMsg(p, Config.invul + ": " + (Utils.toggleInvulnerability(as) ? Config.isOn : Config.isOff));
                break;
            case SLOTS:
                Utils.actionBarMsg(p, Config.equip + ": " + (Main.nms.toggleSlotsDisabled(as) ? Config.locked : Config.unLocked));
                break;
            case MOVE:
                p.closeInventory();
                final UUID uuid = p.getUniqueId();
                if (plugin.carryingArmorStand.containsKey(uuid)) {
                    plugin.carryingArmorStand.remove(uuid);
                    Utils.actionBarMsg(p, Config.asDropped);
                } else {
                    plugin.pickUpArmorStand(as, p, false);
                    Utils.actionBarMsg(p, Config.carrying);
                }
                break;
            case GLOW:
                Utils.actionBarMsg(p, Config.glow + ": " + (Utils.toggleGlow(as) ? Config.isOn : Config.isOff));
                break;
            default:
                return;
        }
        i.setItem(t.getSlot(), updateLore(t));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(i) || !(event.getWhoClicked() instanceof final Player p)) return;
        boolean invModified = false;
        for (int slot : event.getRawSlots()) {
            if (slot < i.getSize()) {
                if (invSlots.contains(slot)) {
                    invModified = true;
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (invModified) {
            if (plugin.checkBlockPermission(p, as.getLocation().getBlock())) {
                updateInventory();
            } else {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + Config.wgNoPerm);
            }
        }
    }

    private void updateInventory() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (as == null || i == null) return;
                as.setItem(EquipmentSlot.HAND, i.getItem(10));
                as.setItem(EquipmentSlot.OFF_HAND, i.getItem(12));
                as.setItem(EquipmentSlot.HEAD, i.getItem(2));
                as.setItem(EquipmentSlot.CHEST, i.getItem(11));
                as.setItem(EquipmentSlot.LEGS, i.getItem(20));
                as.setItem(EquipmentSlot.FEET, i.getItem(29));
            }
        }.runTaskLater(plugin, 1L);
    }

}