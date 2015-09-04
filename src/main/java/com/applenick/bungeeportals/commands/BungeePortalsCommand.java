package com.applenick.bungeeportals.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.applenick.bungeeportals.BungeePortals;
import com.google.common.primitives.Ints;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class BungeePortalsCommand implements CommandExecutor {

    private final BungeePortals plugin;

    public BungeePortalsCommand(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfigurationFiles();
                plugin.reloadPortalsData();
                sender.sendMessage(ChatColor.GREEN + "All configuration files and data have been reloaded.");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command is only executable by players.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + ' ' + args[0].toLowerCase() + " <serverName> ['overwrite'] [ignoredBlockIds...]");
                    return true;
                }

                if (plugin.getWorldEdit() == null) {
                    sender.sendMessage(ChatColor.RED + "Unable to create portals: WorldEdit not installed.");
                    return true;
                }

                Player player = (Player) sender;
                Selection selection = plugin.getWorldEdit().getSelection(player);

                if (selection == null) {
                    sender.sendMessage(ChatColor.RED + "You need to have a WorldEdit selection to do this.");
                    return true;
                }

                Set<Integer> ignoredBlockIds = getIgnoredBlockIds(sender, args, 2);
                if (ignoredBlockIds == null) {
                    return true;
                }

                int count = 0;
                final String serverName = args[1].toLowerCase();
                World world = player.getWorld();
                for (Location location : getLocationsFromSelection(selection)) {
                    Block block = world.getBlockAt(location);
                    if (!ignoredBlockIds.contains(block.getTypeId())) {
                        String locString = world.getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ();
                        if (plugin.getPortalData().putIfAbsent(locString, serverName) == null) { //TODO: add overwrite flag in arg
                            count++;
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN.toString() + count + " portals have been created for server " + serverName + " in the selection.");
                if (count == 0) return true; // prevent saving

                // Finally save the configuration async.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.savePortalConfiguration();
                    }
                }.runTaskAsynchronously(plugin);
                return true;
            }

            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command is only executable by players.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + ' ' + args[0].toLowerCase() + " <serverName|all> [ignoredBlockIds...]");
                    return true;
                }

                if (plugin.getWorldEdit() == null) {
                    sender.sendMessage(ChatColor.RED + "Unable to create portals: WorldEdit not installed.");
                    return true;
                }

                Player player = (Player) sender;
                Selection selection = plugin.getWorldEdit().getSelection(player);

                if (selection == null) {
                    sender.sendMessage(ChatColor.RED + "You need to have a WorldEdit selection to do this.");
                    return true;
                }

                Set<Integer> ignoredBlockIds = getIgnoredBlockIds(sender, args, 2);
                if (ignoredBlockIds == null) {
                    return true;
                }

                int removed = 0;
                final String serverName = args[1].equalsIgnoreCase("all") ? null : args[1].toLowerCase();
                World world = player.getWorld();
                for (Location location : getLocationsFromSelection(selection)) {
                    Block block = world.getBlockAt(location);
                    if (!ignoredBlockIds.contains(block.getTypeId())) {
                        final boolean result;
                        final String data = world.getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ();
                        if (serverName == null) {
                            result = plugin.getPortalData().remove(data) != null;
                        } else {
                            result = plugin.getPortalData().remove(data, serverName);
                        }

                        if (result) {
                            ++removed;
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN.toString() + removed + " portals in the selection have been removed for" +
                        (serverName != null ? " server " + serverName : "all servers") + ".");

                if (removed == 0) return true; // prevent saving

                // Finally save the configuration async.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.savePortalConfiguration();
                    }
                }.runTaskAsynchronously(plugin);
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <create|remove|reload>"); // More readable usage message.

        /*
         sender.sendMessage(ChatColor.BLUE + descriptionFile.getFullName() + " by " + StringUtils.join(descriptionFile.getAuthors(), ", "));
        sender.sendMessage(ChatColor.GREEN + "/" + label + " reload " + ChatColor.RED + "Reload all files and data.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " forcesave " + ChatColor.RED + "Force-save portals.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " select <filter,list> " + ChatColor.RED + "Get selection.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " clear " + ChatColor.RED + "Clear selection.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " create <destination> " + ChatColor.RED + "Create portals.");
        sender.sendMessage(ChatColor.GREEN + "/" + label + " remove <destination> " + ChatColor.RED + "Remove portals.");

        PluginDescriptionFile descriptionFile = plugin.getDescription();
        String website = descriptionFile.getWebsite();
        if (website != null) {
            sender.sendMessage(ChatColor.BLUE + "Visit " + website + " for help.");
        }*/

        return true;
    }

    private List<Location> getLocationsFromSelection(Selection selection) {
        Validate.notNull(selection, "Selection cannot be null");

        World world = selection.getWorld();
        List<Location> locations = new ArrayList<Location>();
        Location minLocation = selection.getMinimumPoint();
        Location maxLocation = selection.getMaximumPoint();
        for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                    locations.add(new Location(world, x, y, z));
                }
            }
        }

        return locations;
    }

    private Set<Integer> getIgnoredBlockIds(CommandSender sender, String[] args, int startIndex) {
        if (args.length < startIndex) return null;

        final Set<Integer> ignored = new HashSet<Integer>(args.length - startIndex);
        for (int i = startIndex; i < args.length; i++) {
            Integer val = Ints.tryParse(args[i]);
            if (val == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[i] + "' is not a number.");
                return null;
            } else ignored.add(val);
        }

        return ignored;
    }
}
