package com.applenick.bungeeportals;

import org.bukkit.Material;

/************************************************
			 Created By AppleNick
Copyright © 2015 , AppleNick, All rights reserved.
			http://applenick.com
 *************************************************/
public class PortalManager {


	public PortalManager(){
		//TODO: Will be used for Portal stuff later on
	}



	public static Material getPortalMaterial(String type){
		if(type.equalsIgnoreCase("END")){
			return Material.ENDER_PORTAL;
		}else if(type.equalsIgnoreCase("LAVA")){
			return Material.LAVA;
		}else if(type.equalsIgnoreCase("PORTAL")){
			return Material.PORTAL;
		}else if(type.equalsIgnoreCase("WATER")){
			return Material.WATER;
		}else if(type.equalsIgnoreCase("WEB")){
			return Material.WEB;
		}else{
			return Material.PORTAL;
		}

	}
}
