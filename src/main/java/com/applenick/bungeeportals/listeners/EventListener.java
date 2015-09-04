package com.applenick.bungeeportals.listeners;

import com.applenick.bungeeportals.BungeePortals;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

public class EventListener implements Listener {

    private final BungeePortals plugin;
    private final Set<UUID> statusData = Sets.newHashSet();

    public EventListener(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.statusData.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block block = event.getTo().getBlock();
        String data = block.getWorld().getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ();

        String destination = plugin.getPortalData().get(data);
        if (destination != null) {
            if (this.statusData.add(player.getUniqueId())) {
                if (player.hasPermission("bungeeportals.portal." + destination) || player.hasPermission("bungeeportals.portal.*")) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(destination);
                    player.sendPluginMessage(plugin, BungeePortals.PROXY_NAME, out.toByteArray());
                } else {
                    player.sendMessage(plugin.getConfigFile().getString("NoPortalPermissionMessage").replace("{destination}", destination).replaceAll("(&([a-f0-9l-or]))", "\u00A7$2"));
                }
            }
        } else {
            this.statusData.remove(player.getUniqueId());
        }
    }
}
