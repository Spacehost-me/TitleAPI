package com.connorlinfoot.titleapi;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Minecraft version the server is running, as a comparable major.minor.patch value.
 * <p>
 * This is deliberately not a list of known releases, so versions newer than the ones this library
 * was built against are still detected and compared correctly without needing a code change.
 */
final class MinecraftVersion implements Comparable<MinecraftVersion> {
    static final MinecraftVersion v1_8 = of(1, 8, 0);
    static final MinecraftVersion v1_13 = of(1, 13, 0);

    /**
     * Used when the running version cannot be read at all, keeping the old behaviour of assuming
     * something modern rather than refusing to load.
     */
    private static final MinecraftVersion LATEST_KNOWN = of(1, 26, 3);

    // "1.26.3-R0.1-SNAPSHOT", as returned by Bukkit.getBukkitVersion()
    private static final Pattern BUKKIT_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    // "git-Spigot-18fbb24-ac8c7ca (MC: 1.26.3)", as returned by Bukkit.getVersion()
    private static final Pattern SERVER_VERSION = Pattern.compile("MC:\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    // "v1_12_R1", the legacy relocated CraftBukkit package name
    private static final Pattern PACKAGE_VERSION = Pattern.compile("v(\\d+)_(\\d+)");

    private static final MinecraftVersion MINECRAFT_VERSION = build();

    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    static MinecraftVersion of(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch);
    }

    public boolean isGreaterThanOrEqualTo(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isLessThanOrEqualTo(MinecraftVersion other) {
        return compareTo(other) <= 0;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MinecraftVersion)) {
            return false;
        }
        return compareTo((MinecraftVersion) other) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 1000 + minor) * 1000 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    private static MinecraftVersion build() {
        // Every source is optional: Bukkit.getBukkitVersion() is the reliable one on all supported
        // versions, the others are there in case a fork reports something unusual. The package name
        // is last because servers from 1.20.5 onwards no longer relocate CraftBukkit at all.
        try {
            MinecraftVersion version = parse(BUKKIT_VERSION, Bukkit.getBukkitVersion());
            if (version == null) {
                version = parse(SERVER_VERSION, Bukkit.getVersion());
            }
            if (version == null) {
                version = parse(PACKAGE_VERSION, Bukkit.getServer().getClass().getPackage().getName());
            }
            if (version != null) {
                return version;
            }
        } catch (Throwable ignored) {
            // Never let version detection stop the plugin from loading
        }
        return LATEST_KNOWN;
    }

    private static MinecraftVersion parse(Pattern pattern, String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        try {
            String patch = matcher.groupCount() >= 3 ? matcher.group(3) : null;
            return of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch == null ? 0 : Integer.parseInt(patch));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static MinecraftVersion get() {
        return MINECRAFT_VERSION;
    }
}
