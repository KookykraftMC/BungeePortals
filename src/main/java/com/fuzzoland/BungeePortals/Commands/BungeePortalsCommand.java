package com.fuzzoland.BungeePortals.Commands;

import com.fuzzoland.BungeePortals.BungeePortals;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BungeePortalsCommand implements CommandExecutor {

    private final BungeePortals plugin;
    private final Multimap<UUID, String> selections = HashMultimap.create();

    public BungeePortalsCommand(BungeePortals plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.BLUE + "BungeePortals v" + plugin.getDescription().getVersion() + " by YoFuzzy3");
                sender.sendMessage(ChatColor.GREEN + "/BPortals reload " + ChatColor.RED + "Reload all files and data.");
                sender.sendMessage(ChatColor.GREEN + "/BPortals forcesave " + ChatColor.RED + "Force-save portals.");
                sender.sendMessage(ChatColor.GREEN + "/BPortals select <filter,list> " + ChatColor.RED + "Get selection.");
                sender.sendMessage(ChatColor.GREEN + "/BPortals clear " + ChatColor.RED + "Clear selection.");
                sender.sendMessage(ChatColor.GREEN + "/BPortals create <destination> " + ChatColor.RED + "Create portals.");
                sender.sendMessage(ChatColor.GREEN + "/BPortals remove <destination> " + ChatColor.RED + "Remove portals.");
                sender.sendMessage(ChatColor.BLUE + "Visit www.spigotmc.org/resources/bungeeportals.19 for help.");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.loadConfigurationFiles();
                    plugin.loadPortalsData();
                    sender.sendMessage(ChatColor.GREEN + "All configuration files and data have been reloaded.");
                } else if (args[0].equalsIgnoreCase("forcesave")) {
                    plugin.savePortalsData();
                    sender.sendMessage(ChatColor.GREEN + "Portal data saved!");
                } else if (args[0].equalsIgnoreCase("clear")) {
                    if (sender instanceof Player) {
                        UUID playerUUID = ((Player) sender).getUniqueId();
                        if (this.selections.removeAll(playerUUID) != null) {
                            sender.sendMessage(ChatColor.GREEN + "Selection cleared.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Type /BPortals for help!");
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("select")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        UUID playerUUID = player.getUniqueId();
                        Selection selection = plugin.worldEdit.getSelection(player);
                        if (selection != null) {
                            if (selection instanceof CuboidSelection) {
                                List<Location> locations = getLocationsFromCuboid((CuboidSelection) selection);
                                Integer count = 0;
                                Integer filtered = 0;
                                String[] ids = null;
                                Boolean filter = false;
                                if (!args[1].equals("0")) {
                                    ids = args[1].split(",");
                                    filter = true;
                                }
                                for (Location location : locations) {
                                    Block block = player.getWorld().getBlockAt(location);
                                    if (filter) {
                                        Boolean found = false;
                                        for (String id : ids) {
                                            String[] parts = id.split(":");
                                            if (parts.length == 2) {
                                                if (parts[0].equals(String.valueOf(block.getTypeId())) && parts[1].equals(String.valueOf(block.getData()))) {
                                                    found = true;
                                                    break;
                                                }
                                            } else {
                                                if (parts[0].equals(String.valueOf(block.getTypeId()))) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (found) {
                                            selections.put(playerUUID, block.getWorld().getName() + "#" + String.valueOf(block.getX()) + "#" + String.valueOf(block.getY()) + "#" + String.valueOf(block.getZ()));
                                            count++;
                                        } else {
                                            filtered++;
                                        }
                                    } else {
                                        selections.put(playerUUID, block.getWorld().getName() + "#" + String.valueOf(block.getX()) + "#" + String.valueOf(block.getY()) + "#" + String.valueOf(block.getZ()));
                                        count++;
                                    }
                                }
                                sender.sendMessage(ChatColor.GREEN + String.valueOf(count) + " blocks have been selected, " + String.valueOf(filtered) + " filtered.");
                                sender.sendMessage(ChatColor.GREEN + "Use the selection in the create and remove commands.");
                            } else {
                                sender.sendMessage(ChatColor.RED + "Must be a cuboid selection!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You have to first create a WorldEdit selection!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (sender instanceof Player) {
                        UUID playerUUID = ((Player) sender).getUniqueId();
                        Collection<String> selections = this.selections.get(playerUUID);
                        if (selections != null && !selections.isEmpty()) {
                            for (String block : selections) {
                                plugin.portalData.put(block, args[1]);
                            }
                            sender.sendMessage(ChatColor.GREEN + String.valueOf(selections.size()) + " portals have been created.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (sender instanceof Player) {
                        UUID playerUUID = ((Player) sender).getUniqueId();
                        Collection<String> selections = this.selections.get(playerUUID);
                        if (selections != null && !selections.isEmpty()) {
                            int count = 0;
                            for (String block : selections) {
                                if (plugin.portalData.remove(block) != null) {
                                    count++;
                                }
                            }
                            sender.sendMessage(ChatColor.GREEN + String.valueOf(count) + " portals have been removed.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "You haven't selected anything.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can use that command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Type /BPortals for help!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Type /BPortals for help!");
            }
        return true;
    }

    private List<Location> getLocationsFromCuboid(CuboidSelection cuboid) {
        List<Location> locations = new ArrayList<Location>();
        Location minLocation = cuboid.getMinimumPoint();
        Location maxLocation = cuboid.getMaximumPoint();
        for (int i1 = minLocation.getBlockX(); i1 <= maxLocation.getBlockX(); i1++) {
            for (int i2 = minLocation.getBlockY(); i2 <= maxLocation.getBlockY(); i2++) {
                for (int i3 = minLocation.getBlockZ(); i3 <= maxLocation.getBlockZ(); i3++) {
                    locations.add(new Location(cuboid.getWorld(), i1, i2, i3));
                }
            }
        }
        return locations;
    }
}
