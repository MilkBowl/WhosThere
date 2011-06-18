package com.sleaker.WhosThere;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class WhosThere extends JavaPlugin{

	public static Logger log = Logger.getLogger("Minecraft");
	public static final String plugName = "[WhosThere]"; 

	public void onDisable() {
		log.info(plugName + " Disabled");
	}
	
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(plugName + " - " + pdfFile.getVersion() + " by Sleaker is enabled!");

	}



}
