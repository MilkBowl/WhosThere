package com.sleaker.MemManager;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class MemManager extends JavaPlugin{

	private final MemCheck memchecker = new MemCheck();
	public static Logger log = Logger.getLogger("Minecraft");
	public static final String plugName = "[MemManager]"; 

	public void onDisable() {
		log.info(plugName + " Disabled");
	}



	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(plugName + " - " + pdfFile.getVersion() + " by Sleaker is enabled!");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, memchecker, 200, 6000);

	}



}
