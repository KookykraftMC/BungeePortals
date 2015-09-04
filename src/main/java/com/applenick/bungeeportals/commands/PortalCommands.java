package com.applenick.bungeeportals.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;

/************************************************
			 Created By AppleNick
Copyright © 2015 , AppleNick, All rights reserved.
			http://applenick.com
 *************************************************/
public class PortalCommands {
	
	public static class PortalCommandsParent{
		@Command(
				aliases = {"portals", "bungeeportals", "bp"},
				desc = "Control your Bungee Portals"
				)
		@NestedCommand(PortalCommands.class)
		public static void onPortalCommand(){
		}
	}
	
	
	@Command(
			aliases = {"reload"},
			desc = "Reloads BungeePortal Config"
			)
	@CommandPermissions("bportals.reload")
	public static void reloadCommand(){
		
	}
	
	
	@Command(
			aliases = {"create" , "c"},
			desc = "Creates a BungeePortal"
			)
	@CommandPermissions("bportals.create")
	public static void createCommand(){
		
	}

}
