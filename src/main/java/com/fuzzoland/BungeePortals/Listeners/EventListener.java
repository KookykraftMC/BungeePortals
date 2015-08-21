package com.fuzzoland.BungeePortals.Listeners;

import com.fuzzoland.BungeePortals.BungeePortals;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EventListener implements Listener {

    private BungeePortals plugin;
    private Map<String, Boolean> statusData = new HashMap<String, Boolean>();

    public EventListener(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (!this.statusData.containsKey(playerName)) {
            this.statusData.put(playerName, false);
        }
        Block block = player.getWorld().getBlockAt(player.getLocation());
        String data = block.getWorld().getName() + "#" + String.valueOf(block.getX()) + "#" + String.valueOf(block.getY()) + "#" + String.valueOf(block.getZ());
        if (plugin.portalData.containsKey(data)) {
            if (!this.statusData.get(playerName)) {
                this.statusData.put(playerName, true);
                String destination = plugin.portalData.get(data);
                if (player.hasPermission("BungeePortals.portal." + destination) || player.hasPermission("BungeePortals.portal.*")) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(destination);
                    player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                } else {
                    player.sendMessage(plugin.configFile.getString("NoPortalPermissionMessage").replace("{destination}", destination).replaceAll("(&([a-f0-9l-or]))", "\u00A7$2"));
                }
            }
        } else {
            if (this.statusData.get(playerName)) {
                this.statusData.put(playerName, false);
            }
        }
    }
}
