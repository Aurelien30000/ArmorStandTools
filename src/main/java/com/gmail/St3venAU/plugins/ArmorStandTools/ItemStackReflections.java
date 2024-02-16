package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ItemStackReflections {
    private static final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
    private final static Method AS_NMS_COPY;
    private final static Method GET_TAG;
    private final static Method MODIFY_ITEM_STACK;
    private final static Method CONTAINS;
    private final static Object CRAFT_MAGIC_NUMBERS;

    static {
        try {
            Class<?> mcItemStack = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> bukkitItemStack = Class.forName(CRAFTBUKKIT_PACKAGE + ".inventory.CraftItemStack");
            Class<?> craftMagicNumbers = Class.forName(CRAFTBUKKIT_PACKAGE + ".util.CraftMagicNumbers");
            Class<?> nbtTagCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
            AS_NMS_COPY = bukkitItemStack.getMethod("asNMSCopy", ItemStack.class);
            GET_TAG = mcItemStack.getDeclaredMethod("getTagClone");
            GET_TAG.setAccessible(true);
            MODIFY_ITEM_STACK = craftMagicNumbers.getDeclaredMethod("modifyItemStack", ItemStack.class, String.class);
            CRAFT_MAGIC_NUMBERS = craftMagicNumbers.getField("INSTANCE").get(null);
            Class<?>[] parameters = new Class[]{String.class, int.class};
            CONTAINS = Arrays.stream(nbtTagCompound.getDeclaredMethods()).filter(m ->
                    m.getReturnType() == boolean.class && Arrays.equals(m.getParameterTypes(), parameters)).findAny().orElseThrow();
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static String itemNBTToString(ItemStack item) {
        final Object tag = getItemNBT(item);
        if (tag == null) {
            return null;
        }
        return tag.toString();
    }

    public static Object getItemNBT(ItemStack item) {
        try {
            final Object nmsItem = AS_NMS_COPY.invoke(null, item);
            if (nmsItem == null) {
                throw new NullPointerException("Unable to find a nms item clone for " + item);
            }
            return GET_TAG.invoke(nmsItem);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsEntityTag(ItemStack itemStack) {
        final Object tag = getItemNBT(itemStack);
        if (tag == null) return false;
        try {
            return (Boolean) CONTAINS.invoke(tag, "EntityTag", 10/*CompoundTag*/);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setItemNBTFromString(ItemStack itemStack, String nbt) {
        try {
            MODIFY_ITEM_STACK.invoke(CRAFT_MAGIC_NUMBERS, itemStack, nbt);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
