package com.applenick.bungeeportals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.applenick.bungeeportals.commands.PortalCommands;
import com.applenick.bungeeportals.listeners.EventListener;
import com.applenick.bungeeportals.tasks.SaveTask;
import com.google.common.collect.Maps;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class BungeePortals extends JavaPlugin {

    public static final String PROXY_NAME = "BungeeCord";

    // key -> location string, value -> server name
    private final ConcurrentMap<String, String> portalData = Maps.newConcurrentMap();

    private WorldEditPlugin worldEdit;
    private YamlConfiguration configFile;
    private YamlConfiguration portalsFile;
    
    private static BungeePortals plugin;
    public static BungeePortals get(){
    	return plugin;
    }
    
    private static PortalManager manager;
    public static PortalManager getManager(){
    	return manager;
    }

    @Override
    public void onEnable() {
    	plugin = this;
        long time = System.currentTimeMillis();

        Plugin worldEditPlugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEditPlugin == null) {
            getLogger().warning("WorldEdit not found, will not be able to create portals");
        } else this.worldEdit = (WorldEditPlugin) worldEditPlugin;

        //TODO: Enable when new portal features are added
        //this.manager = new PortalManager();
        
        this.setupCommands();
        getLogger().log(Level.INFO, "Commands registered!");

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getLogger().log(Level.INFO, "Events registered!");

        getServer().getMessenger().registerOutgoingPluginChannel(this, PROXY_NAME);
        getLogger().log(Level.INFO, "Plugin channel registered!");

        this.reloadConfigurationFiles();
        this.reloadPortalsData();

        long interval = this.configFile.getInt("SaveTask.Interval") * 20L;
        new SaveTask(this).runTaskTimer(this, interval, interval);

        getLogger().log(Level.INFO, "Save task started!");
        getLogger().log(Level.INFO, "Version " + getDescription().getVersion() + " has been enabled. (" + (System.currentTimeMillis() - time) + "ms)");
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        this.savePortalConfiguration();
        getLogger().log(Level.INFO, "Version " + getDescription().getVersion() + " has been disabled. (" + (System.currentTimeMillis() - time) + "ms)");
    }

    public ConcurrentMap<String, String> getPortalData() {
        return portalData;
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEdit;
    }

    public YamlConfiguration getConfigFile() {
        return configFile;
    }

    private void createConfigurationFile(InputStream in, File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void reloadConfigurationFiles() {
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);

        this.configFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        getLogger().log(Level.INFO, "Configuration file " + configFile.getName() + " loaded!");

        File portalsFile = new File(getDataFolder(), "portals.yml");
        if (!portalsFile.exists() && portalsFile.getParentFile().mkdirs()) {
            createConfigurationFile(getResource(portalsFile.getName()), portalsFile);
            getLogger().log(Level.INFO, "Configuration file " + portalsFile.getName() + " created!");
        }

        this.portalsFile = YamlConfiguration.loadConfiguration(portalsFile);
        getLogger().log(Level.INFO, "Configuration file " + portalsFile.getName() + " loaded!");
    }

    public void reloadPortalsData() {
        this.portalData.clear();

        try {
            long time = System.currentTimeMillis();
            for (String key : this.portalsFile.getKeys(false)) {
                this.portalData.put(key, this.portalsFile.getString(key));
            }
            getLogger().log(Level.INFO, "Portal data loaded! (" + (System.currentTimeMillis() - time) + "ms)");
        } catch (NullPointerException ignored) {
        }
    }

    public void savePortalConfiguration() {
        //TODO: write lock for concurrent saving when creating and deleting.

        long time = System.currentTimeMillis();
        for (Entry<String, String> entry : this.portalData.entrySet()) {
            this.portalsFile.set(entry.getKey(), entry.getValue());
        }

        try {
            this.portalsFile.save(new File(getDataFolder(), "portals.yml"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        getLogger().log(Level.INFO, "Portal data saved! (" + (System.currentTimeMillis() - time) + "ms)");
    }
    
    //Command Framework
	private CommandsManager<CommandSender> commands;
	private void setupCommands(){
		this.commands = new CommandsManager<CommandSender>() {
			@Override public boolean hasPermission(CommandSender sender, String perm) {
				return sender instanceof ConsoleCommandSender || sender.hasPermission(perm);
			}
		};

		CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, this.commands);
		cmdRegister.register(PortalCommands.PortalCommandsParent.class);
	}
    
    @Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		try {
			this.commands.execute(cmd.getName(), args, sender, sender);
		} catch (CommandPermissionsException e) {
			sender.sendMessage(ChatColor.RED + "You don't have permission.");
		} catch (MissingNestedCommandException e) {
			sender.sendMessage(ChatColor.RED + e.getUsage());
		} catch (CommandUsageException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
			sender.sendMessage(ChatColor.RED + e.getUsage());
		} catch (WrappedCommandException e) {
			if (e.getCause() instanceof NumberFormatException) {
				sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
				e.printStackTrace();
			}
		} catch (CommandException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
		}

		return true;
	}
    
}
