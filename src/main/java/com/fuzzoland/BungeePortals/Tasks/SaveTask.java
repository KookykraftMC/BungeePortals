package com.fuzzoland.BungeePortals.Tasks;

import com.fuzzoland.BungeePortals.BungeePortals;
import org.bukkit.scheduler.BukkitRunnable;

public class SaveTask extends BukkitRunnable {

    private BungeePortals plugin;

    public SaveTask(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getConfigFile().getBoolean("SaveTask.Enabled")) {
            plugin.savePortalsData();
        }
    }
}
