package com.connorlinfoot.titleapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.logging.Level;

/**
 * Packet based implementation for 1.8 - 1.12, before Bukkit had a title API of its own.
 */
class ReflectionTitleAPI implements InternalTitleAPI {
    private static final String NMS_VERSION = nmsVersion();

    ReflectionTitleAPI() {

    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            TitleAPI.instance.getLogger().log(Level.WARNING, "Failed to send packet", e);
        }
    }

    private static String nmsVersion() {
        // org.bukkit.craftbukkit.v1_12_R1.CraftServer on the versions this implementation covers.
        // Servers from 1.20.5 onwards no longer relocate CraftBukkit, hence the length check.
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        return parts.length > 3 ? parts[3] : null;
    }

    private Class<?> getNMSClass(String name) {
        if (NMS_VERSION == null) {
            return null;
        }

        try {
            return Class.forName("net.minecraft.server." + NMS_VERSION + "." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) {
            return;
        }

        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> componentClass = getNMSClass("IChatBaseComponent");
        if (packetClass == null || componentClass == null) {
            TitleAPI.instance.getLogger().warning("Could not find the title packet classes for Minecraft " + MinecraftVersion.get());
            return;
        }

        try {
            Class<?> actionClass = packetClass.getDeclaredClasses()[0]; // PacketPlayOutTitle.EnumTitleAction
            Constructor<?> timesConstructor = packetClass.getConstructor(actionClass, componentClass, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            Constructor<?> textConstructor = packetClass.getConstructor(actionClass, componentClass);

            Object times = timesConstructor.newInstance(actionClass.getField("TIMES").get(null), toComponent(componentClass, ""), fadeIn, stay, fadeOut);
            sendPacket(player, times);

            if (title != null) {
                sendPacket(player, textConstructor.newInstance(actionClass.getField("TITLE").get(null), toComponent(componentClass, title)));
            }

            if (subtitle != null) {
                sendPacket(player, textConstructor.newInstance(actionClass.getField("SUBTITLE").get(null), toComponent(componentClass, subtitle)));
            }
        } catch (Exception e) {
            TitleAPI.instance.getLogger().log(Level.WARNING, "Failed to handle reflection", e);
        }
    }

    @Override
    public void sendTabTitle(Player player, String header, String footer) {
        Class<?> packetClass = getNMSClass("PacketPlayOutPlayerListHeaderFooter");
        Class<?> componentClass = getNMSClass("IChatBaseComponent");
        if (packetClass == null || componentClass == null) {
            TitleAPI.instance.getLogger().warning("Could not find the tab list packet classes for Minecraft " + MinecraftVersion.get());
            return;
        }

        try {
            Object packet = packetClass.getConstructor().newInstance();
            setField(packet, "a", "header", toComponent(componentClass, header == null ? "" : header));
            setField(packet, "b", "footer", toComponent(componentClass, footer == null ? "" : footer));
            sendPacket(player, packet);
        } catch (Exception e) {
            TitleAPI.instance.getLogger().log(Level.WARNING, "Failed to handle reflection", e);
        }
    }

    private Object toComponent(Class<?> componentClass, String message) throws ReflectiveOperationException {
        // IChatBaseComponent.ChatSerializer.a(String)
        return componentClass.getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, toChatJson(message));
    }

    private String toChatJson(String message) {
        String escaped = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "{\"text\":\"" + escaped + "\"}";
    }

    private void setField(Object packet, String obfuscatedName, String name, Object value) throws ReflectiveOperationException {
        Field field;
        try {
            field = packet.getClass().getDeclaredField(obfuscatedName);
        } catch (NoSuchFieldException e) {
            field = packet.getClass().getDeclaredField(name);
        }
        field.setAccessible(true);
        field.set(packet, value);
    }
}
