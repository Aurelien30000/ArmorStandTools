package com.gmail.St3venAU.plugins.ArmorStandTools;


import com.destroystokyo.paper.MaterialTags;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

class ArmorStandGUI implements Listener {

    private static final int INV_SLOT_HELMET = 1;
    private static final int INV_SLOT_CHEST = 10;
    private static final int INV_SLOT_LEGS = 19;
    private static final int INV_SLOT_BOOTS = 28;
    private static final int INV_SLOT_MAIN_HAND = 9;
    private static final int INV_SLOT_OFF_HAND = 11;

    private static final IntSet inUse = new IntOpenHashSet();
    private static final IntSet invSlots = new IntOpenHashSet();
    private static ItemStack filler;

    private Inventory i;
    private ArmorStand as;

    ArmorStandGUI(ArmorStand as, Player p) {
        if (isInUse(as)) {
            p.sendMessage(ChatColor.RED + Config.guiInUse);
            return;
        }
        AST.plugin.getServer().getPluginManager().registerEvents(this, AST.plugin);
        this.as = as;
        String name = as.getCustomName();
        if (name == null) {
            name = Config.armorStand;
        } else if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        i = Bukkit.createInventory(null, 54, name);
        for (int slot = 0; slot < i.getSize(); slot++) {
            i.setItem(slot, filler);
        }
        for (ArmorStandTool tool : ArmorStandTool.values()) {
            if (tool.isForGui() && tool.isEnabled()) {
                i.setItem(tool.getSlot(), tool.updateLore(as));
            }
        }
        final EntityEquipment entityEquipment = as.getEquipment();
        i.setItem(INV_SLOT_MAIN_HAND, entityEquipment.getItemInMainHand());
        i.setItem(INV_SLOT_OFF_HAND, entityEquipment.getItemInOffHand());
        i.setItem(INV_SLOT_HELMET, entityEquipment.getHelmet());
        i.setItem(INV_SLOT_CHEST, entityEquipment.getChestplate());
        i.setItem(INV_SLOT_LEGS, entityEquipment.getLeggings());
        i.setItem(INV_SLOT_BOOTS, entityEquipment.getBoots());
        inUse.add(as.getEntityId());
        p.openInventory(i);
    }

    static void init() {
        filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        final ItemMeta im = filler.getItemMeta();
        if (im != null) {
            im.setDisplayName(" ");
            filler.setItemMeta(im);
        }
        invSlots.add(INV_SLOT_HELMET);
        invSlots.add(INV_SLOT_CHEST);
        invSlots.add(INV_SLOT_LEGS);
        invSlots.add(INV_SLOT_BOOTS);
        invSlots.add(INV_SLOT_MAIN_HAND);
        invSlots.add(INV_SLOT_OFF_HAND);
    }

    static boolean isInUse(ArmorStand as) {
        return inUse.contains(as.getEntityId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(i))
            return;
        HandlerList.unregisterAll(this);
        inUse.remove(as.getEntityId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(i) || !(event.getWhoClicked() instanceof final Player p))
            return;
        if (event.getClick() == ClickType.SHIFT_RIGHT || event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }
        final int slot = event.getRawSlot();
        final Location l = as.getLocation();
        if (invSlots.contains(slot)) {
            if (AST.checkBlockPermission(p, l.getBlock())) {
                updateArmorStandInventory();
            } else {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + Config.wgNoPerm);
            }
            return;
        }
        if (event.getClick() == ClickType.SHIFT_LEFT) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (slot >= i.getSize() && item != null && !ArmorStandTool.isTool(item) && event.getClickedInventory() != null) {
                if (AST.checkBlockPermission(p, l.getBlock())) {
                    final Material m = item.getType();
                    final int newSlot;
                    if (MaterialTags.HEAD_EQUIPPABLE.isTagged(m) && slotIsEmpty(INV_SLOT_HELMET)) {
                        newSlot = INV_SLOT_HELMET;
                    } else if (MaterialTags.CHEST_EQUIPPABLE.isTagged(m) && slotIsEmpty(INV_SLOT_CHEST)) {
                        newSlot = INV_SLOT_CHEST;
                    } else if (MaterialTags.LEGGINGS.isTagged(m) && slotIsEmpty(INV_SLOT_LEGS)) {
                        newSlot = INV_SLOT_LEGS;
                    } else if (MaterialTags.BOOTS.isTagged(m) && slotIsEmpty(INV_SLOT_BOOTS)) {
                        newSlot = INV_SLOT_BOOTS;
                    } else if (slotIsEmpty(INV_SLOT_MAIN_HAND)) {
                        newSlot = INV_SLOT_MAIN_HAND;
                    } else if (slotIsEmpty(INV_SLOT_OFF_HAND)) {
                        newSlot = INV_SLOT_OFF_HAND;
                    } else {
                        newSlot = -1;
                    }
                    if (newSlot != -1) {
                        i.setItem(newSlot, item);
                        event.getClickedInventory().setItem(event.getSlot(), null);
                        updateArmorStandInventory();
                    }
                } else {
                    p.sendMessage(ChatColor.RED + Config.wgNoPerm);
                }
            }
            return;
        }
        if (slot >= i.getSize())
            return;
        event.setCancelled(true);
        final ArmorStandTool t = ArmorStandTool.get(event.getCurrentItem());
        if (t == null)
            return;
        if (!AST.playerHasPermission(p, l.getBlock(), t)) {
            p.sendMessage(ChatColor.RED + Config.generalNoPerm);
            return;
        }
        switch (t) {
            case HEAD, BODY, LARM, RARM, LLEG, RLEG -> {
                final UUID uuid = p.getUniqueId();
                AST.activeTool.put(uuid, t);
                AST.selectedArmorStand.put(uuid, as);
                p.closeInventory();
                t.showTitle(p);
            }
            case INVIS -> {
                as.setVisible(!as.isVisible());
                Utils.title(p, Config.asVisible + ": " + (as.isVisible() ? Config.isTrue : Config.isFalse));
            }
            case CLONE -> {
                ArmorStand clone = Utils.cloneArmorStand(p, as);
                if (clone != null) {
                    AST.pickUpArmorStand(clone, p, true);
                    Utils.title(p, Config.carrying);
                }
            }
            case GEN_CMD -> {
                final String command = Utils.createSummonCommand(as);
                p.sendMessage(command);
                if (Config.saveToolCreatesCommandBlock) {
                    if (Config.requireCreative && p.getGameMode() != GameMode.CREATIVE) {
                        p.sendMessage(ChatColor.RED + Config.creativeRequired);
                    } else {
                        Utils.generateCmdBlock(p.getLocation(), command);
                        Utils.title(p, Config.cbCreated);
                    }
                }
                if (Config.logGeneratedSummonCommands) {
                    Config.logSummonCommand(p.getName(), command);
                }
            }
            case SIZE -> {
                as.setSmall(!as.isSmall());
                Utils.title(p, Config.size + ": " + (as.isSmall() ? Config.small : Config.normal));
            }
            case BASE -> {
                as.setBasePlate(!as.hasBasePlate());
                Utils.title(p, Config.basePlate + ": " + (as.hasBasePlate() ? Config.isOn : Config.isOff));
            }
            case GRAV -> {
                as.setGravity(!as.hasGravity());
                Utils.title(p, Config.gravity + ": " + (as.hasGravity() ? Config.isOn : Config.isOff));
            }
            case ARMS -> {
                as.setArms(!as.hasArms());
                Utils.title(p, Config.arms + ": " + (as.hasArms() ? Config.isOn : Config.isOff));
            }
            case NAME -> {
                p.closeInventory();
                AST.setName(p, as);
            }
            case PHEAD -> {
                p.closeInventory();
                AST.setPlayerSkull(p, as);
            }
            case INVUL -> {
                final boolean inv = !as.isInvulnerable();
                as.setInvulnerable(inv);
                Utils.title(p, Config.invul + ": " + (inv ? Config.isOn : Config.isOff));
            }
            case SLOTS ->
                    Utils.title(p, Config.equip + ": " + (Utils.toggleSlotsDisabled(as) ? Config.locked : Config.unLocked));
            case MOVE -> {
                p.closeInventory();
                as.removeMetadata("clone", AST.plugin);
                AST.pickUpArmorStand(as, p, false);
                Utils.title(p, Config.carrying);
            }
            case GLOW -> {
                boolean glowing = !as.isGlowing();
                as.setGlowing(glowing);
                Utils.title(p, Config.glow + ": " + (glowing ? Config.isOn : Config.isOff));
            }
            case ITEM -> {
                ItemStack stack = Utils.createArmorStandItem(as);
                if (stack == null) {
                    p.closeInventory();
                    break;
                }
                p.getWorld().dropItemNaturally(p.getLocation(), stack).setPickupDelay(0);
                p.closeInventory();
                if (p.getGameMode() != GameMode.CREATIVE) {
                    as.remove();
                }
            }
            default -> {
                return;
            }
        }
        i.setItem(t.getSlot(), t.updateLore(as));
    }

    private boolean slotIsEmpty(int slot) {
        final ItemStack item = i.getItem(slot);
        return item == null || item.getType().isAir() || item.getAmount() == 0;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(i) || !(event.getWhoClicked() instanceof final Player p))
            return;
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
            if (AST.checkBlockPermission(p, as.getLocation().getBlock())) {
                updateArmorStandInventory();
            } else {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + Config.wgNoPerm);
            }
        }
    }

    private void updateArmorStandInventory() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final EntityEquipment equipment = as.getEquipment();
                if (as == null || i == null)
                    return;
                equipment.setItemInMainHand(i.getItem(INV_SLOT_MAIN_HAND));
                equipment.setItemInOffHand(i.getItem(INV_SLOT_OFF_HAND));
                equipment.setHelmet(i.getItem(INV_SLOT_HELMET));
                equipment.setChestplate(i.getItem(INV_SLOT_CHEST));
                equipment.setLeggings(i.getItem(INV_SLOT_LEGS));
                equipment.setBoots(i.getItem(INV_SLOT_BOOTS));
            }
        }.runTaskLater(AST.plugin, 1L);
    }

}