package com.gmail.St3venAU.plugins.ArmorStandTools;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ItemStackReflections {

    private static final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();

    private final static Method GET_SERVER;
    private final static Method GET_REGISTRY_ACCESS;
    private final static Method AS_NMS_COPY;
    private final static Method GET_COMPONENTS;
    private final static Method HAS_COMPONENT;
    private final static Method SAVE;
    private final static Method MODIFY_ITEM_STACK;

    private final static Object ENTITY_DATA_COMPONENT_TYPE;
    private final static Object CRAFT_MAGIC_NUMBERS;

    static {
        try {
            Class<?> mcMinecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
            Class<?> mcItemStack = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> mcDataComponentMap = Class.forName("net.minecraft.core.component.DataComponentMap");
            Class<?> mcDataComponentType = Class.forName("net.minecraft.core.component.DataComponentType");
            Class<?> mcDataComponents = Class.forName("net.minecraft.core.component.DataComponents");
            Class<?> mcHolderLookupProvider = Class.forName("net.minecraft.core.HolderLookup$Provider");
            Class<?> bukkitItemStack = Class.forName(CRAFTBUKKIT_PACKAGE + ".inventory.CraftItemStack");
            Class<?> craftMagicNumbers = Class.forName(CRAFTBUKKIT_PACKAGE + ".util.CraftMagicNumbers");

            GET_SERVER = mcMinecraftServer.getMethod("getServer");
            GET_REGISTRY_ACCESS = mcMinecraftServer.getMethod("registryAccess");
            AS_NMS_COPY = bukkitItemStack.getMethod("asNMSCopy", ItemStack.class);
            GET_COMPONENTS = mcItemStack.getDeclaredMethod("a");
            GET_COMPONENTS.setAccessible(true);
            HAS_COMPONENT = mcDataComponentMap.getDeclaredMethod("get", mcDataComponentType);
            SAVE = mcItemStack.getDeclaredMethod("save", mcHolderLookupProvider);
            MODIFY_ITEM_STACK = craftMagicNumbers.getDeclaredMethod("modifyItemStack", ItemStack.class, String.class);

            ENTITY_DATA_COMPONENT_TYPE = mcDataComponents.getField("ENTITY_DATA").get(null);
            CRAFT_MAGIC_NUMBERS = craftMagicNumbers.getField("INSTANCE").get(null);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static String itemNBTToString(ItemStack item) {
        try {
            final Object server = GET_SERVER.invoke(null);
            final Object registryAccess = GET_REGISTRY_ACCESS.invoke(server);
            final Object nmsItem = AS_NMS_COPY.invoke(null, item);
            if (nmsItem == null) {
                throw new NullPointerException("Unable to find a nms item clone for " + item);
            }

            final Object tag = SAVE.invoke(nmsItem, registryAccess);
            return tag.toString();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getItemComponents(ItemStack item) {
        try {
            final Object nmsItem = AS_NMS_COPY.invoke(null, item);
            if (nmsItem == null) {
                throw new NullPointerException("Unable to find a nms item clone for " + item);
            }
            return GET_COMPONENTS.invoke(nmsItem);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsEntityData(ItemStack itemStack) {
        final Object components = getItemComponents(itemStack);
        if (components == null)
            return false;

        try {
            return (Boolean) HAS_COMPONENT.invoke(components, ENTITY_DATA_COMPONENT_TYPE);
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
