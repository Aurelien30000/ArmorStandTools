package com.gmail.St3venAU.plugins.ArmorStandTools.nms;

import com.gmail.St3venAU.plugins.ArmorStandTools.ArmorStandCmd;
import com.gmail.St3venAU.plugins.ArmorStandTools.Main;
import com.gmail.St3venAU.plugins.ArmorStandTools.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class NMS {

    private final String nmsVersion;
    private final String disabledSlotsFieldName;

    public NMS(String nmsVersion, String disabledSlotsFieldName) {
        this.nmsVersion = nmsVersion;
        this.disabledSlotsFieldName = disabledSlotsFieldName;
    }

    private Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        return Class.forName("net.minecraft." + nmsClassString);
    }

    private Object getNmsEntity(org.bukkit.entity.Entity entity) {
        try {
            return entity.getClass().getMethod("getHandle").invoke(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void openSign(final Player p, final Block b) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    final Object world = b.getWorld().getClass().getMethod("getHandle").invoke(b.getWorld());
                    final Object blockPos = getNMSClass("core.BlockPosition").getConstructor(int.class, int.class, int.class).newInstance(b.getX(), b.getY(), b.getZ());
                    final Object sign = world.getClass().getMethod("getTileEntity", getNMSClass("core.BlockPosition")).invoke(world, blockPos);
                    final Object player = p.getClass().getMethod("getHandle").invoke(p);
                    player.getClass().getMethod("openSign", getNMSClass("world.level.block.entity.TileEntitySign")).invoke(player, sign);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskLater(Main.plugin, 2L);
    }

    public boolean toggleSlotsDisabled(ArmorStand as) {
        final boolean slotsDisabled = getDisabledSlots(as) == 0;
        setSlotsDisabled(as, slotsDisabled);
        return slotsDisabled;
    }

    private int getDisabledSlots(ArmorStand as) {
        final Object nmsEntity = getNmsEntity(as);
        if (nmsEntity == null) return 0;
        final Field f;
        try {
            f = nmsEntity.getClass().getDeclaredField(disabledSlotsFieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return 0;
        }
        f.setAccessible(true);
        try {
            return (Integer) f.get(nmsEntity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void setSlotsDisabled(ArmorStand as, boolean slotsDisabled) {
        final Object nmsEntity = getNmsEntity(as);
        if (nmsEntity == null) return;
        final Field f;
        try {
            f = nmsEntity.getClass().getDeclaredField(disabledSlotsFieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return;
        }
        f.setAccessible(true);
        try {
            f.set(nmsEntity, slotsDisabled ? 0xFFFFFF : 0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean equipmentLocked(ArmorStand as) {
        return getDisabledSlots(as) == 0xFFFFFF;
    }

    private String getItemStackTags(ItemStack is) {
        if (is == null) {
            return "";
        }
        final StringBuilder tags = new StringBuilder();
        if (is.getItemMeta() != null && is.getItemMeta() instanceof LeatherArmorMeta armorMeta) {
            tags.append("display:{color:");
            tags.append(armorMeta.getColor().asRGB());
            tags.append("}");
        }
        final Map<Enchantment, Integer> enchants = is.getEnchantments();
        if (enchants.size() > 0) {
            if (tags.length() > 0) {
                tags.append(",");
            }
            tags.append("Enchantments:[");

            for (Enchantment e : enchants.keySet()) {
                tags.append("{id:");
                tags.append(e.getKey().getKey());
                tags.append(",lvl:");
                tags.append(enchants.get(e));
                tags.append("},");
            }

            tags.setCharAt(tags.length() - 1, ']');
        }
        return tags.length() == 0 ? "" : ("," + tags);
    }

    private String skullOwner(ItemStack is) {
        if (is == null || is.getItemMeta() == null || !(is.getItemMeta() instanceof final SkullMeta skull)) {
            return "";
        }
        if (skull.hasOwner()) {
            return ",SkullOwner:\"" + skull.getOwningPlayer().getName() + "\"";
        } else {
            return "";
        }
    }

    public void generateCmdBlock(Location l, ArmorStand as) {
        final Block b = l.getBlock();
        b.setType(Material.COMMAND_BLOCK);
        final CommandBlock cb = (CommandBlock) b.getState();
        cb.setCommand("summon minecraft:armor_stand " + Utils.twoDec(as.getLocation().getX()) + " " + Utils.twoDec(as.getLocation().getY()) + " " + Utils.twoDec(as.getLocation().getZ()) + " "
                + "{"
                + (as.isVisible() ? "" : "Invisible:1,")
                + (as.hasBasePlate() ? "" : "NoBasePlate:1,")
                + (as.hasGravity() ? "" : "NoGravity:1,")
                + (as.hasArms() ? "ShowArms:1," : "")
                + (as.isSmall() ? "Small:1," : "")
                + (as.isInvulnerable() ? "Invulnerable:1," : "")
                + (as.isGlowing() ? "Glowing:1," : "")
                + (getDisabledSlots(as) == 0 ? "" : ("DisabledSlots:" + getDisabledSlots(as) + ","))
                + (as.isCustomNameVisible() ? "CustomNameVisible:1," : "")
                + (as.getCustomName() == null ? "" : ("CustomName:\"\\\"" + as.getCustomName() + "\\\"\","))
                + (as.getLocation().getYaw() == 0F ? "" : ("Rotation:[" + Utils.twoDec(as.getLocation().getYaw()) + "f],"))
                + "ArmorItems:["
                + (as.getItem(EquipmentSlot.FEET).getType() == Material.AIR ? "{}," : ("{id:" + as.getItem(EquipmentSlot.FEET).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.FEET).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.FEET).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.FEET)) + "}},"))
                + (as.getItem(EquipmentSlot.LEGS).getType() == Material.AIR ? "{}," : ("{id:" + as.getItem(EquipmentSlot.LEGS).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.LEGS).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.LEGS).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.LEGS)) + "}},"))
                + (as.getItem(EquipmentSlot.CHEST).getType() == Material.AIR ? "{}," : ("{id:" + as.getItem(EquipmentSlot.CHEST).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.CHEST).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.CHEST).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.CHEST)) + "}},"))
                + (as.getItem(EquipmentSlot.HEAD).getType() == Material.AIR ? "{}" : ("{id:" + as.getItem(EquipmentSlot.HEAD).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.HEAD).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.HEAD).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.HEAD)) + skullOwner(as.getItem(EquipmentSlot.HEAD)) + "}}"))
                + "],"
                + "HandItems:["
                + (as.getItem(EquipmentSlot.HAND).getType() == Material.AIR ? "{}," : ("{id:" + as.getItem(EquipmentSlot.HAND).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.HAND).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.HAND).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.HAND)) + "}},"))
                + (as.getItem(EquipmentSlot.OFF_HAND).getType() == Material.AIR ? "{}" : ("{id:" + as.getItem(EquipmentSlot.OFF_HAND).getType().getKey().getKey() + ",Count:" + as.getItem(EquipmentSlot.OFF_HAND).getAmount() + ",tag:{Damage:" + as.getItem(EquipmentSlot.OFF_HAND).getDurability() + getItemStackTags(as.getItem(EquipmentSlot.OFF_HAND)) + "}}"))
                + "],"
                + "Pose:{"
                + "Body:[" + Utils.angle(as.getBodyPose().getX()) + "f," + Utils.angle(as.getBodyPose().getY()) + "f," + Utils.angle(as.getBodyPose().getZ()) + "f],"
                + "Head:[" + Utils.angle(as.getHeadPose().getX()) + "f," + Utils.angle(as.getHeadPose().getY()) + "f," + Utils.angle(as.getHeadPose().getZ()) + "f],"
                + "LeftLeg:[" + Utils.angle(as.getLeftLegPose().getX()) + "f," + Utils.angle(as.getLeftLegPose().getY()) + "f," + Utils.angle(as.getLeftLegPose().getZ()) + "f],"
                + "RightLeg:[" + Utils.angle(as.getRightLegPose().getX()) + "f," + Utils.angle(as.getRightLegPose().getY()) + "f," + Utils.angle(as.getRightLegPose().getZ()) + "f],"
                + "LeftArm:[" + Utils.angle(as.getLeftArmPose().getX()) + "f," + Utils.angle(as.getLeftArmPose().getY()) + "f," + Utils.angle(as.getLeftArmPose().getZ()) + "f],"
                + "RightArm:[" + Utils.angle(as.getRightArmPose().getX()) + "f," + Utils.angle(as.getRightArmPose().getY()) + "f," + Utils.angle(as.getRightArmPose().getZ()) + "f]"
                + "}"
                + "}"
        );
        cb.update();
    }

    public ArmorStand clone(ArmorStand as) {
        final ArmorStand clone = (ArmorStand) as.getWorld().spawnEntity(as.getLocation().add(1, 0, 0), EntityType.ARMOR_STAND);
        clone.setGravity(as.hasGravity());
        clone.setHelmet(as.getItem(EquipmentSlot.HEAD));
        clone.setChestplate(as.getItem(EquipmentSlot.CHEST));
        clone.setLeggings(as.getItem(EquipmentSlot.LEGS));
        clone.setBoots(as.getItem(EquipmentSlot.FEET));
        clone.getEquipment().setItemInMainHand(as.getItem(EquipmentSlot.HAND));
        clone.getEquipment().setItemInOffHand(as.getItem(EquipmentSlot.OFF_HAND));
        clone.setHeadPose(as.getHeadPose());
        clone.setBodyPose(as.getBodyPose());
        clone.setLeftArmPose(as.getLeftArmPose());
        clone.setRightArmPose(as.getRightArmPose());
        clone.setLeftLegPose(as.getLeftLegPose());
        clone.setRightLegPose(as.getRightLegPose());
        clone.setVisible(as.isVisible());
        clone.setBasePlate(as.hasBasePlate());
        clone.setArms(as.hasArms());
        clone.setCustomName(as.getCustomName());
        clone.setCustomNameVisible(as.isCustomNameVisible());
        clone.setSmall(as.isSmall());
        clone.setInvulnerable(as.isInvulnerable());
        clone.setGlowing(as.isGlowing());
        setSlotsDisabled(clone, getDisabledSlots(as) == 0xFFFFFF);
        final ArmorStandCmd asCmd = new ArmorStandCmd(as);
        if (asCmd.getCommand() != null) {
            asCmd.cloneTo(clone);
        }
        return clone;
    }

}
