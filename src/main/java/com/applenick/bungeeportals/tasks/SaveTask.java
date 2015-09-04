package com.applenick.bungeeportals.tasks;

import com.applenick.bungeeportals.BungeePortals;

import org.bukkit.scheduler.BukkitRunnable;

public class SaveTask extends BukkitRunnable {

    private final BungeePortals plugin;

    public SaveTask(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getConfigFile().getBoolean("SaveTask.Enabled")) {
            plugin.savePortalConfiguration();
        }
    }
}
