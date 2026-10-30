package com.connorlinfoot.titleapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CLUpdate implements Listener {
    private CLUpdate.UpdateResult result = CLUpdate.UpdateResult.DISABLED;
    private String version;
    private Plugin plugin;
    private String message = null;
    private String pluginMessage = null;
    private String updateMessage = null;
    private boolean updateAvailable = false;

    public enum UpdateResult {
        NO_UPDATE,
        DISABLED,
        UPDATE_AVAILABLE
    }

    CLUpdate(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::doCheck);
    }

    private void doCheck() {
        String data;
        String url = "http://api.linfoot.dev/v1/resource/release/" + plugin.getDescription().getName().toLowerCase() + "/";
        try {
            data = doCurl(url);
        } catch (IOException e) {
            return;
        }

        String newestVersion = readString(data, "version");
        if (newestVersion != null) {
            String currentVersion = plugin.getDescription().getVersion().replaceAll("-SNAPSHOT-", "."); // Changes 4.0.0-SNAPSHOT-4 to 4.0.0.4
            if (isNewer(newestVersion, currentVersion)) {
                result = UpdateResult.UPDATE_AVAILABLE;
                version = newestVersion;
            } else {
                result = UpdateResult.NO_UPDATE;
            }

            String remoteMessage = readString(data, "message");
            if (remoteMessage != null) {
                this.message = ChatColor.translateAlternateColorCodes('&', remoteMessage);
                Bukkit.getConsoleSender().sendMessage(this.message);
            }
        }

        Bukkit.getScheduler().runTask(plugin, this::handleResult);
    }

    private static boolean isNewer(String newestVersion, String currentVersion) {
        try {
            return Integer.parseInt(newestVersion.replace(".", "")) > Integer.parseInt(currentVersion.replace(".", ""));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Reads a single string value out of the response. The response only ever holds a flat object
     * of strings, so this avoids depending on a JSON library being on the server's classpath -
     * json-simple ships with some server versions and not others.
     */
    private static String readString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : null;
    }

    private static String unescape(String value) {
        StringBuilder unescaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                unescaped.append(current);
                continue;
            }

            char escaped = value.charAt(++i);
            switch (escaped) {
                case 'n':
                    unescaped.append('\n');
                    break;
                case 'r':
                    unescaped.append('\r');
                    break;
                case 't':
                    unescaped.append('\t');
                    break;
                case 'b':
                    unescaped.append('\b');
                    break;
                case 'f':
                    unescaped.append('\f');
                    break;
                case 'u':
                    if (i + 4 < value.length()) {
                        try {
                            unescaped.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            unescaped.append(escaped);
                        }
                    }
                    break;
                default:
                    unescaped.append(escaped);
            }
        }
        return unescaped.toString();
    }

    public String getVersion() {
        return version;
    }

    public String doCurl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(true);
        con.setDoOutput(true);
        con.setDoInput(true);
        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.close();
        DataInputStream input = new DataInputStream(con.getInputStream());
        int c;
        StringBuilder resultBuf = new StringBuilder();
        while ((c = input.read()) != -1) {
            resultBuf.append((char) c);
        }
        input.close();
        return resultBuf.toString();
    }

    public String getMessage() {
        return message;
    }

    public void handleResult() {
        if (getMessage() != null) {
            pluginMessage = getMessage();
        }

        switch (result) {
            default:
            case NO_UPDATE:
                updateAvailable = false;
                updateMessage = "No update was found, you are running the latest version.";
                break;
            case DISABLED:
                updateAvailable = false;
                updateMessage = "You currently have update checks disabled";
                break;
            case UPDATE_AVAILABLE:
                updateAvailable = true;
                updateMessage = "An update for " + plugin.getDescription().getName() + " is available, new version is " + getVersion() + ". Your installed version is " + plugin.getDescription().getVersion() + ".\nPlease update to the latest version :)";
                break;
        }

        plugin.getLogger().info(updateMessage);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (updateAvailable && event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(updateMessage);
        }
        if (pluginMessage != null && event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(pluginMessage);
        }
    }

}
