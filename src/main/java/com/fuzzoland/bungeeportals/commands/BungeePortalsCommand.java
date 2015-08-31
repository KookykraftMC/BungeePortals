package com.fuzzoland.bungeeportals.commands;

import com.fuzzoland.bungeeportals.BungeePortals;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BungeePortalsCommand implements CommandExecutor {

    private final Multimap<UUID, String> selections = HashMultimap.create();
    private final BungeePortals plugin;

    public BungeePortalsCommand(BungeePortals plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            PluginDescriptionFile descriptionFile = plugin.getDescription();
            sender.sendMessage(ChatColor.BLUE + descriptionFile.getFullName() + " by " + StringUtils.join(descriptionFile.getAuthors(), ", "));
            sender.sendMessage(ChatColor.GREEN + "/" + label + " reload " + ChatColor.RED + "Reload all files and data.");
            sender.sendMessage(ChatColor.GREEN + "/" + label + " forcesave " + ChatColor.RED + "Force-save portals.");
            sender.sendMessage(ChatColor.GREEN + "/" + label + " select <filter,list> " + ChatColor.RED + "Get selection.");
            sender.sendMessage(ChatColor.GREEN + "/" + label + " clear " + ChatColor.RED + "Clear selection.");
            sender.sendMessage(ChatColor.GREEN + "/" + label + " create <destination> " + ChatColor.RED + "Create portals.");
            sender.sendMessage(ChatColor.GREEN + "/" + label + " remove <destination> " + ChatColor.RED + "Remove portals.");
            if (descriptionFile.getWebsite() != null) {
                sender.sendMessage(ChatColor.BLUE + "Visit " + descriptionFile.getWebsite() + " for help.");
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigurationFiles();
            plugin.reloadPortalsData();
            sender.sendMessage(ChatColor.GREEN + "All configuration files and data have been reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("forcesave")) {
            plugin.savePortalsData();
            sender.sendMessage(ChatColor.GREEN + "Portal data saved!");
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                return true;
            }

            Collection<String> selections = this.selections.removeAll(((Player) sender).getUniqueId());
            if (selections != null && !selections.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Selection cleared.");
            } else {
                sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("select")) {
            if (plugin.getWorldEdit() == null) {
                sender.sendMessage(ChatColor.RED + "Cannot do this as WorldEdit is not installed.");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + ' ' + args[0].toLowerCase() + " <filter|list>");
                return true;
            }

            Player player = (Player) sender;
            Selection selection = plugin.getWorldEdit().getSelection(player);
            if (selection == null) {
                sender.sendMessage(ChatColor.RED + "You have to first create a WorldEdit selection!");
                return true;
            }

            int count = 0;
            int filtered = 0;
            String[] ids = null;
            boolean filter = false;
            if (!args[1].equals("0")) {
                ids = args[1].split(",");
                filter = true;
            }

            World world = player.getWorld();
            UUID uuid = player.getUniqueId();
            final List<Location> locations = getLocationsFromSelection(selection);
            for (Location location : locations) {
                Block block = world.getBlockAt(location);
                if (filter) {
                    boolean found = false;
                    for (String id : ids) {
                        String[] parts = id.split(":");
                        if (parts.length == 2) {
                            if (parts[0].equals(String.valueOf(block.getTypeId())) && parts[1].equals(String.valueOf(block.getData()))) {
                                found = true;
                                break;
                            }
                        } else if (parts[0].equals(String.valueOf(block.getTypeId()))) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        selections.put(uuid, block.getWorld().getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ());
                        count++;
                    } else {
                        filtered++;
                    }
                } else {
                    selections.put(uuid, block.getWorld().getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ());
                    count++;
                }
            }

            sender.sendMessage(ChatColor.GREEN.toString() + count + " blocks have been selected, " + filtered + " filtered.");
            sender.sendMessage(ChatColor.GREEN + "Use the selection in the create and remove commands.");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + ' ' + args[0].toLowerCase() + " <serverName>");
                return true;
            }

            Player player = (Player) sender;
            Collection<String> selections = this.selections.get(player.getUniqueId());
            if (selections == null || selections.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
                return true;
            }

            Map<String, String> portalData = plugin.getPortalData();
            for (String selection : selections) {
                portalData.put(selection, args[1]);
            }

            sender.sendMessage(ChatColor.GREEN.toString() + selections.size() + " portals have been created.");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + ' ' + args[0].toLowerCase() + " <serverName>");
                return true;
            }

            Collection<String> selections = this.selections.get(((Player) sender).getUniqueId());
            if (selections == null || selections.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
                return true;
            }

            int count = 0;
            Set<String> portalData = plugin.getPortalData().keySet();
            for (String selection : selections) {
                if (portalData.remove(selection)) {
                    count++;
                }
            }

            sender.sendMessage(ChatColor.GREEN.toString() + count + " portals have been removed.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Type /" + label + " for help!");
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
}
