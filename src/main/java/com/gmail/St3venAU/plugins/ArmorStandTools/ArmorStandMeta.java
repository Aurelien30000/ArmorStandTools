package com.gmail.st3venau.plugins.armorstandtools;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ArmorStandMeta implements Serializable {

    final String name;
    final float yaw;
    final ItemStack helmet, chest, leggings, boots, mainHand, offHand;
    final double[] headPose, bodyPose, leftArmPose, rightArmPose, leftLegPose, rightLegPose;
    final boolean gravity, visible, basePlate, arms, small, invulnerable, glowing, nameVisible, marker;
    final Map<EquipmentSlot, List<ArmorStand.LockType>> equipmentLocks;
    final List<String> commandTags = new ArrayList<>();
    String cooldownTag;

    ArmorStandMeta(ArmorStand as) {
        name = as.getCustomName();
        yaw = as.getLocation().getYaw();
        final EntityEquipment equipment = as.getEquipment();
        helmet = equipment.getHelmet();
        chest = equipment.getChestplate();
        leggings = equipment.getLeggings();
        boots = equipment.getBoots();
        mainHand = equipment.getItemInMainHand();
        offHand = equipment.getItemInOffHand();
        headPose = new double[3];
        headPose[0] = as.getHeadPose().getX();
        headPose[1] = as.getHeadPose().getY();
        headPose[2] = as.getHeadPose().getZ();
        bodyPose = new double[3];
        bodyPose[0] = as.getBodyPose().getX();
        bodyPose[1] = as.getBodyPose().getY();
        bodyPose[2] = as.getBodyPose().getZ();
        leftArmPose = new double[3];
        leftArmPose[0] = as.getLeftArmPose().getX();
        leftArmPose[1] = as.getLeftArmPose().getY();
        leftArmPose[2] = as.getLeftArmPose().getZ();
        rightArmPose = new double[3];
        rightArmPose[0] = as.getRightArmPose().getX();
        rightArmPose[1] = as.getRightArmPose().getY();
        rightArmPose[2] = as.getRightArmPose().getZ();
        leftLegPose = new double[3];
        leftLegPose[0] = as.getLeftLegPose().getX();
        leftLegPose[1] = as.getLeftLegPose().getX();
        leftLegPose[2] = as.getLeftLegPose().getZ();
        rightLegPose = new double[3];
        rightLegPose[0] = as.getRightLegPose().getX();
        rightLegPose[1] = as.getRightLegPose().getY();
        rightLegPose[2] = as.getRightLegPose().getZ();
        gravity = as.hasGravity();
        visible = as.isVisible();
        basePlate = as.hasBasePlate();
        arms = as.hasArms();
        small = as.isSmall();
        invulnerable = as.isInvulnerable();
        glowing = as.isGlowing();
        nameVisible = as.isCustomNameVisible();
        marker = as.isMarker();
        equipmentLocks = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            final List<ArmorStand.LockType> lockTypes = new ArrayList<>();
            for (ArmorStand.LockType lockType : ArmorStand.LockType.values()) {
                if (as.hasEquipmentLock(slot, lockType)) {
                    lockTypes.add(lockType);
                }
            }
            equipmentLocks.put(slot, lockTypes);
        }
        for (ArmorStandCmd cmd : new ArmorStandCmdManager(as).getCommands()) {
            commandTags.add(cmd.getTag());
        }
        for (String tag : as.getScoreboardTags()) {
            if (tag.startsWith("ast-cdn-")) {
                cooldownTag = tag;
            }
        }
    }

    void applyToArmorStand(ArmorStand as) {
        as.setCustomName(name);
        final EntityEquipment equipment = as.getEquipment();
        equipment.setHelmet(helmet);
        equipment.setChestplate(chest);
        equipment.setLeggings(leggings);
        equipment.setBoots(boots);
        equipment.setItemInMainHand(mainHand);
        equipment.setItemInOffHand(offHand);
        as.setHeadPose(new EulerAngle(headPose[0], headPose[1], headPose[2]));
        as.setBodyPose(new EulerAngle(bodyPose[0], bodyPose[1], bodyPose[2]));
        as.setLeftArmPose(new EulerAngle(leftArmPose[0], leftArmPose[1], leftArmPose[2]));
        as.setRightArmPose(new EulerAngle(rightArmPose[0], rightArmPose[1], rightArmPose[2]));
        as.setLeftLegPose(new EulerAngle(leftLegPose[0], leftLegPose[1], leftLegPose[2]));
        as.setRightLegPose(new EulerAngle(rightLegPose[0], rightLegPose[1], rightLegPose[2]));
        as.setGravity(gravity);
        as.setVisible(visible);
        as.setBasePlate(basePlate);
        as.setArms(arms);
        as.setSmall(small);
        as.setInvulnerable(invulnerable);
        as.setGlowing(glowing);
        as.setCustomNameVisible(nameVisible);
        as.setMarker(marker);
        for (EquipmentSlot slot : equipmentLocks.keySet()) {
            for (ArmorStand.LockType lockType : equipmentLocks.get(slot)) {
                as.addEquipmentLock(slot, lockType);
            }
        }
        for (String tag : commandTags) {
            as.addScoreboardTag(tag);
        }
        if (cooldownTag != null) {
            as.addScoreboardTag(cooldownTag);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                final Location l = as.getLocation();
                l.setYaw(yaw);
                as.teleport(l);
            }
        }.runTaskLater(AST.plugin, 2L);
    }

    ItemStack convertToItem() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            final BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(outputStream);
            objectStream.writeObject(this);
            objectStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        final ItemStack is = new ItemStack(Material.ARMOR_STAND, 1);
        final ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + Config.configuredArmorStand);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(createLore());
        is.setItemMeta(meta);
        try {
            final Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + AST.nmsVersion + ".inventory.CraftItemStack");
            final Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, is);
            final Object tag = nmsStack.getClass().getMethod("getTag")
                    .invoke(nmsStack);
            final Object nbtArray = Class.forName("net.minecraft.nbt.NBTTagByteArray")
                    .getConstructor(byte[].class)
                    .newInstance((Object) outputStream.toByteArray());
            tag.getClass().getMethod("set", String.class, Class.forName("net.minecraft.nbt.NBTBase"))
                    .invoke(tag, "ArmorStandMeta", nbtArray);
            nmsStack.getClass().getMethod("setTag", tag.getClass())
                    .invoke(nmsStack, tag);
            return (ItemStack) craftItemStackClass.getMethod("asBukkitCopy", nmsStack.getClass())
                    .invoke(null, nmsStack);
        } catch (Exception e) {
            if (Config.showDebugMessages) {
                e.printStackTrace();
                AST.debug("Failed to convert armor stand into an item");
            }
            return null;
        }
    }

    static ArmorStandMeta fromItem(ItemStack is) {
        if (is == null || !is.hasItemMeta())
            return null;
        final Object armorStandMetaObject;
        try {
            final Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + AST.nmsVersion + ".inventory.CraftItemStack");
            final Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, is);
            final Object tag = nmsStack.getClass().getMethod("getTag")
                    .invoke(nmsStack);
            if (tag == null)
                return null;
            final byte[] byteArray = (byte[]) tag.getClass().getMethod("getByteArray", String.class)
                    .invoke(tag, "ArmorStandMeta");
            if (byteArray == null)
                return null;
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
            final BukkitObjectInputStream objectStream = new BukkitObjectInputStream(inputStream);
            armorStandMetaObject = objectStream.readObject();
            objectStream.close();
        } catch (Exception e) {
            return null;
        }
        return armorStandMetaObject instanceof ArmorStandMeta ? (ArmorStandMeta) armorStandMetaObject : null;
    }

    List<String> createLore() {
        final List<String> lore = new ArrayList<>();
        if (name != null && name.length() > 0) {
            lore.add(Config.name + ": " + ChatColor.YELLOW + name);
        }
        int stacks = 0;
        int items = 0;
        if (helmet != null) {
            stacks++;
            items += helmet.getAmount();
        }
        if (chest != null) {
            stacks++;
            items += chest.getAmount();
        }
        if (leggings != null) {
            stacks++;
            items += leggings.getAmount();
        }
        if (boots != null) {
            stacks++;
            items += boots.getAmount();
        }
        if (mainHand != null) {
            stacks++;
            items += mainHand.getAmount();
        }
        if (offHand != null) {
            stacks++;
            items += offHand.getAmount();
        }
        if (stacks > 0) {
            lore.add(Config.inventory + ": " + ChatColor.YELLOW + items + " " + Config.items + " (" + stacks + " " + Config.stacks + ")");
        }
        final List<String> attribs = new ArrayList<>();
        if (commandTags.size() > 0)
            attribs.add(commandTags.size() + ": " + Config.cmdsAssigned);
        if (equipmentLocks.size() > 0)
            attribs.add(Config.equip + ": " + Config.locked);
        if (!gravity)
            attribs.add(Config.gravity + ": " + Config.isOff);
        if (!visible)
            attribs.add(Config.invisible + ": " + Config.isOff);
        if (arms)
            attribs.add(Config.arms + ": " + Config.isOn);
        if (small)
            attribs.add(Config.small + ": " + Config.isOn);
        if (invulnerable)
            attribs.add(Config.invuln + ": " + Config.isOn);
        if (glowing)
            attribs.add(Config.glowing + ": " + Config.isOn);
        if (attribs.size() > 0) {
            StringBuilder sb = new StringBuilder(Config.attributes + ": " + ChatColor.YELLOW);
            for (Iterator<String> iterator = attribs.iterator(); iterator.hasNext(); ) {
                final String attrib = iterator.next();
                sb.append(attrib);
                if (iterator.hasNext()) {
                    sb.append(ChatColor.GRAY).append(", ");
                }
                if (sb.length() >= 40) {
                    lore.add(sb.toString());
                    sb = new StringBuilder("" + ChatColor.YELLOW);
                }
            }
        }
        return lore;
    }

}