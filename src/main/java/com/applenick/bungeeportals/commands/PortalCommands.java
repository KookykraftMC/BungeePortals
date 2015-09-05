package com.applenick.bungeeportals.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.applenick.bungeeportals.BungeePortals;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.bukkit.selections.Selection;

/************************************************
			 Created By AppleNick
Copyright © 2015 , AppleNick, All rights reserved.
			http://applenick.com
 *************************************************/
public class PortalCommands {

	public static class PortalCommandsParent{
		@Command(
				aliases = {"portals", "bungeeportals", "bportals", "bp"},
				desc = "Control your Bungee Portals"
				)
		@NestedCommand(PortalCommands.class)
		public static void onPortalCommand(final CommandContext args, final CommandSender sender) throws CommandException{
		}
	}


	@Command(
			aliases = {"reload"},
			desc = "Reloads BungeePortal Config"
			)
	@CommandPermissions("bportals.reload")
	public static void reloadCommand(final CommandContext args, final CommandSender sender) throws CommandException{
		BungeePortals.get().reloadConfigurationFiles();
		BungeePortals.get().reloadPortalsData();
		sender.sendMessage(ChatColor.GREEN + "All configuration files and data have been reloaded.");
		return;
	}



	@Command(
			aliases = {"remove" , "r", "delete" , "del"},
			desc = "Removes a BungeePortal",
			usage = "[name]"			
			)
	@CommandPermissions("bportals.remove")
	public static void removePortalCommand(final CommandContext args, final CommandSender sender) throws CommandException{
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "This command is only executable by players.");
			return;
		}


		if (BungeePortals.get().getWorldEdit() == null) {
			sender.sendMessage(ChatColor.RED + "Unable to create portals: WorldEdit not installed.");
			return;
		}
		
		if(args.argsLength() < 1){
			sender.sendMessage(ChatColor.RED + "Please provide a [server]");
			return;
		}
		
		

		Player player = (Player) sender;
		Selection selection = BungeePortals.get().getWorldEdit().getSelection(player);

		if (selection == null) {
			sender.sendMessage(ChatColor.RED + "You need to have a WorldEdit selection to do this.");
			return;
		}


		int removed = 0;
		final String serverName = args.getString(0).equalsIgnoreCase("all") ? null : args.getString(0).toLowerCase();
		World world = player.getWorld();
		for (Location location : getLocationsFromSelection(selection)) {
			Block block = world.getBlockAt(location);
			if (isPortal(block.getType())) {
				block.setType(Material.AIR);
			}
			final boolean result;
			final String data = world.getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ();
			if (serverName == null) {
				result = BungeePortals.get().getPortalData().remove(data) != null;
			} else {
				result = BungeePortals.get().getPortalData().remove(data, serverName);
			}

			if (result) {
				++removed;
			}
			
		}

		sender.sendMessage(ChatColor.AQUA.toString() + removed + ChatColor.GREEN + " portals in the selection have been removed for " + ChatColor.RED + (serverName != null ? " server " + serverName : "all servers") + ".");

		if (removed == 0) return; // prevent saving

		// Finally save the configuration async.
		new BukkitRunnable() {
			@Override
			public void run() {
				BungeePortals.get().savePortalConfiguration();
			}
		}.runTaskAsynchronously(BungeePortals.get());
		return;
	}


	//Block Types will be {WATER , PORTAL, END, LAVA, WEB}

	@Command(
			aliases = {"create" , "add", "c"},
			desc = "Creates a BungeePortal",
			usage = "[server]"
			)
	@CommandPermissions("bportals.create")
	public static void createPortalCommand(final CommandContext args, final CommandSender sender) throws CommandException{
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command is only executable by players.");
			return;
		}

		if (BungeePortals.get().getWorldEdit() == null) {
			sender.sendMessage(ChatColor.RED + "Unable to create portals: WorldEdit not installed.");
			return;
		}


		if(args.argsLength() == 0){
			sender.sendMessage(ChatColor.RED + "Please provide a [server] and [type]");
		}


		if(args.argsLength() == 1){
			String server = args.getString(0).toLowerCase();

			Player player = (Player) sender;
			Selection selection = BungeePortals.get().getWorldEdit().getSelection(player);

			if (selection == null) {
				sender.sendMessage(ChatColor.RED + "You need to have a WorldEdit selection to do this.");
				return;
			}

			int count = 0;
			World world = player.getWorld();
			for (Location location : getLocationsFromSelection(selection)) {
				Block block = world.getBlockAt(location);
					String locString = world.getName() + '#' + block.getX() + '#' + block.getY() + '#' + block.getZ();
					if (BungeePortals.get().getPortalData().putIfAbsent(locString, server) == null) { //TODO: add overwrite flag in arg
						count++;
				}
			}
			player.sendMessage(ChatColor.GREEN + "A portal to " + ChatColor.GOLD + server + ChatColor.GREEN + " has been created. " + ChatColor.GRAY + "[" + ChatColor.RED + count + ChatColor.GRAY + "]");
			player.playSound(player.getLocation(), Sound.LEVEL_UP, 10, 5);
			if (count == 0) return; // prevent saving

			// Finally save the configuration async.
			new BukkitRunnable() {
				@Override
				public void run() {
					BungeePortals.get().savePortalConfiguration();
				}
			}.runTaskAsynchronously(BungeePortals.get());
			return;
		}		 
	}


	private static boolean isPortal(Material block){
		if(block == Material.LAVA || block == Material.WATER || block == Material.STATIONARY_WATER || block == Material.STATIONARY_LAVA || block == Material.ENDER_PORTAL || block == Material.PORTAL || block == Material.WEB){
			return true;
		}else{
			return false;
		}
	}

	private static List<Location> getLocationsFromSelection(Selection selection) {
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
