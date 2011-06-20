package com.sleaker.WhosThere;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.milkbowl.administrate.AdminHandler;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class WhosThere extends JavaPlugin{

    public Configuration config;

    public static Logger log = Logger.getLogger("Minecraft");
    public static final String plugName = "[WhosThere]"; 
    public static PermissionHandler Permissions = null;
    public AdminHandler admins = null;
    private boolean usePrefix = true;
    private boolean showStealthed = false;

    public void onDisable() {
        log.info(plugName + " Disabled");
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
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
                if (!Permissions.has(player, "whosthere.who")) {
                    return false;
                } else if (!Permissions.has(player, "whosthere.showall") && admins != null && !showStealthed) {
                    whoLimited(sender);
                } else {
                    whoUnlimited(sender);
                }
            } else {
                whoUnlimited(sender);
            }
        }
        return false;
    }

    private void setupConfiguration() {
        config = getConfiguration();
        if (config.getKeys(null).isEmpty()) {
            config.setProperty("use-prefix", usePrefix);
            config.setProperty("show-stealthed", showStealthed);
        }
        usePrefix = config.getBoolean("use-prefix", usePrefix);
        showStealthed = config.getBoolean("show-stealthed", showStealthed);
        config.save();

    }
    public void setupOptionals() {
        if (admins == null) {
            Plugin admin = this.getServer().getPluginManager().getPlugin("Administrate");
            if (admin != null) {
                if (!this.getServer().getPluginManager().isPluginEnabled("Administrate")){
                    this.getServer().getPluginManager().enablePlugin(admin);
                }
                admins = new AdminHandler();
            }
        } 
    }

    public void setupPermissions() {
        if (Permissions == null) {
            Plugin perms = this.getServer().getPluginManager().getPlugin("Permissions");
            if (perms != null) {
                if (!this.getServer().getPluginManager().isPluginEnabled("Permissions"))
                    this.getServer().getPluginManager().enablePlugin(perms);

                Permissions = ((Permissions) perms).getHandler();
                log.info(plugName + " - Successfully hooked into Permissions v" + perms.getDescription().getVersion());
            } else {
                log.info("[" + getDescription().getName() + "] Permissions not detected - disabling plugin");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    /*
     * Gets a Permissions Prefix
     */
    public String prefix(Player player) {
        //Return Null if Permissions didn't load or if usePrefix is false
        if (Permissions == null || !usePrefix) 
            return null;        
        else
            return Permissions.getGroupPrefix(player.getWorld().getName(), Permissions.getGroup(player.getWorld().getName(), player.getName()));
    }

    /*
     * Sends a limited who list to the command sender
     * 
     */
    private void whoLimited(CommandSender sender) {
        String playerList = "";
        int i = 0;
        for (Player player : getServer().getOnlinePlayers()) {
            if (isStealthed(player.getName()))
                continue;
            if (usePrefix) {
                playerList += prefix(player);
            }
            playerList += player.getName() + ChatColor.WHITE + "  ";
            i++;
        }
        String message = ChatColor.WHITE + "There are " + ChatColor.BLUE + i + "/" + getServer().getMaxPlayers() + ChatColor.WHITE + " players online:  " + playerList;
        sender.sendMessage(message);
    }

    /*
     * sends the full who list to the player
     * 
     */
    private void whoUnlimited(CommandSender sender) {
        String playerList = ChatColor.WHITE + "There are " + ChatColor.BLUE + getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers() + ChatColor.WHITE + " players online:  ";
        for (Player player : getServer().getOnlinePlayers()) {
            if (usePrefix) {
                playerList += prefix(player);
            }
            playerList += player.getName() + ChatColor.WHITE + "  ";
        }
        sender.sendMessage(playerList);
    }

    /*
     * Returns whether a player is stealthed or not
     * 
     */
    public boolean isStealthed(String player) {
        if (admins == null)
            return false;
        else
            return admins.isStealthed(player);
    }

}
