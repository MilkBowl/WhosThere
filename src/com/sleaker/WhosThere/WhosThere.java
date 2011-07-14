package com.sleaker.WhosThere;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.milkbowl.administrate.AdminHandler;
import net.milkbowl.administrate.Administrate;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.nijikokun.bukkit.Permissions.Permissions;

public class WhosThere extends JavaPlugin{

	public Configuration config;

	private static Logger log = Logger.getLogger("Minecraft");
	private static String plugName; 
	private static Plugin perms = null;
	private PermissionsHandler handler;

	public AdminHandler admins = null;
	private boolean usePrefix = false;
	private boolean showStealthed = false;
	private boolean useColorOption = true;
	private String colorOption = "namecolor";

	private enum PermissionsHandler {
		PERMISSIONSEX, PERMISSIONS
	}

	public void onDisable() {
		log.info(plugName + " Disabled");
	}

	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		plugName = "[" + pdfFile.getName() + "]";
		setupPermissions();
		setupOptionals();

		//Check to see if there is a configuration file.
		File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			new File(getDataFolder().toString()).mkdir();
			try {
				yml.createNewFile();
			}
			catch (IOException ex) {
				log.info(plugName + " - Cannot create configuration file. And none to load, using defaults.");
			}
		}   
		setupConfiguration();

		log.info(plugName + " - " + pdfFile.getVersion() + " by Sleaker is enabled!");

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (command.getName().equalsIgnoreCase("who")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (!has(player, "whosthere.who")) {
					player.sendMessage("You don't have permission to do that.");
					return true;
				} else if (has(player, "whosthere.showall") && admins != null && !showStealthed) {
					whoLimited(sender, args);
					return true;
				} else {
					whoUnlimited(sender, args);
					return true;
				}
			} else {
				whoUnlimited(sender, args);
				return true;
			}
		} else if (command.getName().equalsIgnoreCase("whois")) {
			if (sender instanceof Player) {
				if (!has((Player) sender, "whosthere.admin")) {
					sender.sendMessage("You don't have permission to do that.");
					return true;
				}
			}
			if (args.length < 1) {
				sender.sendMessage("You must supply a username to get information about");
				return true;
			} else {
				whois(sender, args);
				return true;
			}
		}
		return false;
	}

	private void setupConfiguration() {
		config = getConfiguration();
		if (config.getKeys(null).isEmpty()) {
			config.setProperty("use-prefix", usePrefix);
			config.setProperty("show-stealthed", showStealthed);
			config.setProperty("use-color-option", useColorOption);
			config.setProperty("color-option-name", colorOption);
		}
		usePrefix = config.getBoolean("use-prefix", usePrefix);
		showStealthed = config.getBoolean("show-stealthed", showStealthed);
		useColorOption = config.getBoolean("use-color-option", useColorOption);
		colorOption = config.getString("color-option-name", colorOption);
		config.save();

	}

	public void setupOptionals() {
		if (admins == null) {
			Plugin admin = this.getServer().getPluginManager().getPlugin("Administrate");
			if (admin != null) {
				admins = ((Administrate) admin).getAdminHandler();
				log.info(plugName + " - Successfully hooked into Administrate v" + admin.getDescription().getVersion());
			}
		} 
	}

	public void setupPermissions() {
		Plugin permissionsEx = this.getServer().getPluginManager().getPlugin("PermissionsEx");
		Plugin permissions = this.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionsEx != null) {
			perms = permissionsEx;
			this.handler = PermissionsHandler.PERMISSIONSEX;
			log.info(plugName + " - Successfully hooked into PermissionsEX v" + perms.getDescription().getVersion());
		} else if (permissions != null) {
			this.handler = PermissionsHandler.PERMISSIONS;
			perms = permissions;
			log.info(plugName + " - Successfully hooked into Permissions v" + perms.getDescription().getVersion());
		} else {
			log.info("[" + getDescription().getName() + "] Permissions not detected - disabling plugin");
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}

	public boolean has (Player player, String permission) {
		switch (handler) {
		case PERMISSIONSEX:
			return PermissionsEx.getPermissionManager().has(player, permission);
		case PERMISSIONS:
			return ((Permissions) perms).getHandler().has(player, permission);
		default:
			return false;
		}
	}

	public String option(Player player, String permission) {
		switch (handler) {
		case PERMISSIONSEX:
			return PermissionsEx.getPermissionManager().getUser(player.getName()).getOption(permission);
		case PERMISSIONS:
			return ((Permissions) perms).getHandler().getPermissionString(player.getWorld().getName(), ((Permissions) perms).getHandler().getGroup(player.getWorld().getName(), player.getName()), permission);
		default: return null;
		}
	}

	/*
	 * Gets a Permissions Prefix
	 */
	public String prefix(Player player) {
		//Return Null if Permissions didn't load or if usePrefix is false
		switch (handler) {
		case PERMISSIONSEX:
			return PermissionsEx.getPermissionManager().getUser(player.getName()).getOwnPrefix();
		case PERMISSIONS:
			return ((Permissions) perms).getHandler().getGroupPrefix(player.getWorld().getName(), ((Permissions) perms).getHandler().getGroup(player.getWorld().getName(), player.getName()));
		default: return null;
		}
	}

	private void whois(CommandSender sender, String[] args) {
		Player p = null;
		if (sender instanceof Player)
			p = (Player) sender;

		for (Player pl : this.getServer().getOnlinePlayers()) {
			if (admins != null && !showStealthed && p != null) 
				if (AdminHandler.isStealthed(pl.getName(), p))
					continue;

			if (pl.getName().contains(args[0])) {
				p = pl;
				break;
			}
		}
		if (p != null) {
			Location pLoc = p.getLocation();
			p.sendMessage(replaceColors("&a----  " + colorize(p) + "&a----"));
			p.sendMessage(replaceColors("&aLoc: &d" + pLoc.getBlockX() + "&a, &d" + pLoc.getBlockY() + "&a, &d" + pLoc.getBlockZ() + "&a on: &d" + pLoc.getWorld().getName()));
			p.sendMessage(replaceColors("&aIP: &d" + p.getAddress().getAddress().getHostAddress().toString()));
		} else {
			sender.sendMessage("No player with name " + args[0] + " was found on the server");
		}
	}
	/*
	 * Sends a limited who list to the command sender
	 * 
	 */
	private void whoLimited(CommandSender sender, String[] args) {
		
		String worldName = null;
		if (args.length > 1) {
			if (getServer().getWorld(args[0]) != null)
				worldName = getServer().getWorld(args[0]).getName();
		}
		String playerList = "";
		int i = 0;
		int j = 0;
		for (Player player : getServer().getOnlinePlayers()) {
			if (isStealthed(player.getName()))
				continue;

			if ((worldName == null && args.length == 0) || (worldName != null && player.getWorld().getName().equals(worldName)) || (worldName == null && player.getName().contains(args[0]))) {
				playerList += colorize(player);
				i++;
			}
			j++;
		}
		if (i == 0 && worldName == null) {
			sender.sendMessage("No players found with that name.");
		} else if (i == 0 && worldName != null) {
			sender.sendMessage("No players were found on " + worldName);
		}  else if (args.length == 0) {
			String message = ChatColor.WHITE + "There are " + ChatColor.BLUE + i + "/" + j + ChatColor.WHITE + " players online:  " + playerList;
			sender.sendMessage(message);
		} else {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria: " + playerList;
			sender.sendMessage(message);
		}
	}

	/*
	 * sends the full who list to the player
	 * 
	 */
	private void whoUnlimited(CommandSender sender, String[] args) {
		String worldName = null;
		if (args.length > 1) {
			if (getServer().getWorld(args[0]) != null)
				worldName = getServer().getWorld(args[0]).getName();
		}
		String playerList = "";
		int i = 0;
		for (Player player : getServer().getOnlinePlayers()) {
			if ((worldName == null && args.length == 0) || (worldName != null && player.getWorld().getName().equals(worldName)) || (worldName == null && player.getName().contains(args[0]))) {
				playerList += colorize(player);
				i++;
			}
			
		}
		if (i == 0 && worldName == null) {
			sender.sendMessage("No players found with that name.");
		} else if (i == 0 && worldName != null) {
			sender.sendMessage("No players were found on " + worldName);
		} else if (args.length == 0) {
			String message = ChatColor.WHITE + "There are " + ChatColor.BLUE + i + "/" + getServer().getMaxPlayers() + ChatColor.WHITE + " players online:  " + playerList;
			sender.sendMessage(message);
		} else {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria: " + playerList;
			sender.sendMessage(message);
		}
	}

	/**
	 * Add colorization based on options selected
	 * 
	 * @param p
	 * @return
	 */
	private String colorize (Player p) {
		String message = "";
		if (usePrefix) {
			message += prefix(p);
		}
		if (useColorOption) {
			message += option(p, colorOption);
		}
		message += p.getName() + ChatColor.WHITE + "  ";
		return message;
	}

	/**
	 * Takes a string and replaces &# color codes with ChatColors
	 * 
	 * @param message
	 * @return
	 */
	private String replaceColors (String message) {
		message = message.replaceAll("&a", ChatColor.GREEN + "");
		message = message.replaceAll("&d", ChatColor.LIGHT_PURPLE + "");
		message = message.replaceAll("&f", ChatColor.WHITE + "");
		return message;
	}

	/*
	 * Returns whether a player is stealthed or not
	 * 
	 */
	public boolean isStealthed(String player) {
		if (admins == null)
			return false;
		else
			return AdminHandler.isStealthed(player);
	}

}
