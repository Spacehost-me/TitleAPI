package com.connorlinfoot.titleapi;

import org.bukkit.entity.Player;

/**
 * Implementation backed by the Bukkit API, used on 1.13 and anything newer.
 */
class BukkitTitleAPI implements InternalTitleAPI {

    BukkitTitleAPI() {
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void clearTitle(Player player) {
        player.resetTitle();
    }

    @Override
    public void sendTabTitle(Player player, String header, String footer) {
        player.setPlayerListHeaderFooter(header, footer);
    }
}
