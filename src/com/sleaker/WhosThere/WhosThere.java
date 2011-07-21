package com.sleaker.WhosThere;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import net.milkbowl.administrate.AdminHandler;
import net.milkbowl.administrate.Administrate;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class WhosThere extends JavaPlugin{

	public Configuration config;

	private static Logger log = Logger.getLogger("Minecraft");
	private static String plugName; 
	private Permission perms;

	public AdminHandler admins = null;
	private boolean usePrefix = true;
	private boolean showStealthed = false;
	private boolean useColorOption = false;
	private String colorOption = "namecolor";

	public void onDisable() {
		log.info(plugName + " Disabled");
	}

	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		plugName = "[" + pdfFile.getName() + "]";
		//If we can't load dependencies, disable
		if (!setupDependencies()) {
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

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
				} else if (!has(player, "whosthere.showall") && admins != null && !showStealthed) {
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

	private boolean setupDependencies() {
        Collection<RegisteredServiceProvider<Permission>> perms = this.getServer().getServicesManager().getRegistrations(net.milkbowl.vault.permission.Permission.class);
        for(RegisteredServiceProvider<Permission> perm : perms) {
            Permission p = perm.getProvider();
            log.info(String.format("[%s] Found Service (Permission) %s", getDescription().getName(), p.getName()));
        }
        
        this.perms = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class).getProvider();
        log.info(String.format("[%s] Using Permission Provider %s", getDescription().getName(), this.perms.getName()));
        
		if (this.perms == null)
			return false;
		else
			return true;
	}

	public boolean has(Player player, String permission) {
		return this.perms.has(player, permission);
	}

	public String option(Player player, String permission) {
		return this.perms.getPlayerInfoString(player, permission, null);
	}

	/*
	 * Gets a Permissions Prefix
	 */
	public String prefix(Player player) {
		return this.perms.getPlayerPrefix(player);
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
			sender.sendMessage(replaceColors("&a----  " + colorize(p) + "&a----"));
			sender.sendMessage(replaceColors("&aLoc: &d" + pLoc.getBlockX() + "&a, &d" + pLoc.getBlockY() + "&a, &d" + pLoc.getBlockZ() + "&a on: &d" + pLoc.getWorld().getName()));
			sender.sendMessage(replaceColors("&aIP: &d" + p.getAddress().getAddress().getHostAddress().toString()));
		} else {
			sender.sendMessage("No player with name " + args[0] + " was found on the server");
		}
	}
	/*
	 * Sends a limited who list to the command sender
	 * 
	 */
	private void whoLimited(CommandSender sender, String[] args) {
		
		World world = null;
		if (args.length > 0) {
			world = getServer().getWorld(args[0]);
		}
		String playerList = "";
		int i = 0;
		int j = 0;
		for (Player player : getServer().getOnlinePlayers()) {
			if (isStealthed(player.getName()))
				continue;

			if ((world == null && args.length == 0) || (world != null && player.getWorld().equals(world)) || (world == null && player.getName().contains(args[0]))) {
				playerList += colorize(player);
				i++;
			}
			j++;
		}
		if (i == 0 && world == null && args.length > 0) {
			sender.sendMessage("No players found with that name.");
		} else if (i == 0 && world != null) {
			sender.sendMessage("No players were found on " + world.getName());
		}  else if (args.length == 0) {
			String message = ChatColor.WHITE + "There are " + ChatColor.BLUE + i + "/" + j + ChatColor.WHITE + " players online:  " + playerList;
			sender.sendMessage(message);
		} else if (world != null) {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players on " + world.getName() + ":  " + playerList;
			sender.sendMessage(message);
		} else {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria:  " + playerList;
			sender.sendMessage(message);
		}
	}

	/*
	 * sends the full who list to the player
	 * 
	 */
	private void whoUnlimited(CommandSender sender, String[] args) {
		World world = null;
		if (args.length > 0) {
			world = getServer().getWorld(args[0]);
		}
		String playerList = "";
		int i = 0;
		for (Player player : getServer().getOnlinePlayers()) {
			if ((world == null && args.length == 0) || (world != null && player.getWorld().equals(world)) || (world == null && player.getName().contains(args[0]))) {
				playerList += colorize(player);
				i++;
			}
			
		}
		if (i == 0 && world == null && args.length > 0) {
			sender.sendMessage("No players found with that name.");
		} else if (i == 0 && world != null) {
			sender.sendMessage("No players were found on " + world.getName());
		} else if (args.length == 0) {
			String message = ChatColor.WHITE + "There are " + ChatColor.BLUE + i + "/" + getServer().getMaxPlayers() + ChatColor.WHITE + " players online:  " + playerList;
			sender.sendMessage(message);
		} else if (world != null) {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players " + world.getName() + ":  " + playerList;
			sender.sendMessage(message);
		} else {
			String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria:  " + playerList;
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
		if (useColorOption && colorOption != "" && colorOption != null) {
			message += option(p, colorOption);
		}
		message += p.getName() + ChatColor.WHITE + "  ";
		return replaceColors(message);
	}

	/**
	 * Takes a string and replaces &# color codes with ChatColors
	 * 
	 * @param message
	 * @return
	 */
	private String replaceColors (String message) {
		message = message.replace("&0", ChatColor.BLACK + "");
		message = message.replace("&1", ChatColor.DARK_BLUE + "");
		message = message.replace("&2", ChatColor.DARK_GREEN + "");
		message = message.replace("&3", ChatColor.DARK_AQUA + "");
		message = message.replace("&4", ChatColor.DARK_RED + "");
		message = message.replace("&5", ChatColor.DARK_PURPLE + "");
		message = message.replace("&6", ChatColor.GOLD + "");
		message = message.replace("&7", ChatColor.GRAY + "");
		message = message.replace("&8", ChatColor.DARK_GRAY + "");
		message = message.replace("&9", ChatColor.BLUE + "");
		message = message.replace("&a", ChatColor.GREEN + "");
		message = message.replace("&b", ChatColor.AQUA + "");
		message = message.replace("&c", ChatColor.RED + "");
		message = message.replace("&d", ChatColor.LIGHT_PURPLE + "");
		message = message.replace("&e", ChatColor.YELLOW + "");
		message = message.replace("&f", ChatColor.WHITE + "");
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
