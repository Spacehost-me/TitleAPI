package com.connorlinfoot.titleapi;

import java.util.logging.Level;

enum InternalAPIMapping {
    // Highest minimum version first; the first implementation the running server satisfies wins,
    // so anything newer than the versions listed here keeps using the Bukkit API.
    BUKKIT(BukkitTitleAPI.class, MinecraftVersion.v1_13),
    REFLECTION(ReflectionTitleAPI.class, MinecraftVersion.v1_8),
    ;
    private final Class<? extends InternalTitleAPI> apiClass;
    private final MinecraftVersion minVersion;

    InternalAPIMapping(Class<? extends InternalTitleAPI> apiClass, MinecraftVersion minVersion) {
        this.apiClass = apiClass;
        this.minVersion = minVersion;
    }

    static InternalTitleAPI create() {
        MinecraftVersion version = MinecraftVersion.get();
        for (InternalAPIMapping thisOne : values()) {
            if (version.isGreaterThanOrEqualTo(thisOne.minVersion)) {
                try {
                    return thisOne.apiClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    TitleAPI.instance.getLogger().log(Level.WARNING, "Failed to create InternalTitleAPI", e);
                }
            }
        }
        return null;
    }
}
