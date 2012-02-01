/* WhosThere is copyright Nicholas Minkler
 * WhosThere is licensed under the CreativeCommons - Non-Commercial - No-Derivatives license 
 * It can be viewed at the following link
 * 
 * http://creativecommons.org/licenses/by-nc-nd/3.0/
 */
package com.sleaker.WhosThere;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class WhosThere extends JavaPlugin{

    private Logger log = Logger.getLogger("Minecraft");
    private String plugName; 
    private Permission perms;
    private Chat chat;
    private boolean usePrefix = true;
    private boolean useColorOption = false;
    private boolean displayOnLogin = false;
    private boolean prefixTabName = true;
    private boolean colorOptionTabName = false;
    private String colorOption = "namecolor";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm - MMM, dd");
    private static final int charsPerLine = 52;
    private static final String lineBreak = "%LB%";

    public void onDisable() {
        log.info(plugName + " Disabled");
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        plugName = "[" + pdfFile.getName() + "]";
        //If we can't load dependencies, disable
        if (!setupPermissions()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //Setup chat API connection
        if (!setupChat()) {
            log.warning(plugName + " - No Info/Chat plugin found! Colorization and Prefix options will not work!");
        }
        setupConfiguration();

        this.getServer().getPluginManager().registerEvents(new WhoPlayerListener(this), this);
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
                }
            } 
            //If this is Console, or a Player with Administrate priveledges they will see this message
            whoCommand(sender, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("whois")) {
            if (sender instanceof Player) {
                if (!has((Player) sender, "whosthere.whois")) {
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
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        usePrefix = config.getBoolean("use-prefix", usePrefix);
        useColorOption = config.getBoolean("use-color-option", useColorOption);
        colorOption = config.getString("color-option-name", colorOption);
        displayOnLogin = config.getBoolean("display-on-login", displayOnLogin);
        prefixTabName = config.getBoolean("prefix-tab-name", prefixTabName);
        colorOptionTabName = config.getBoolean("color-option-tab-name", colorOptionTabName);
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            perms = permissionProvider.getProvider();
        }
        return (perms != null);
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null) {
            this.chat = chatProvider.getProvider();
            log.info(String.format("[%s] Using Chat Provider %s", getDescription().getName(), this.chat.getName()));
        }
        return chat != null;
    }

    public boolean has(Player player, String permission) {
        return this.perms.has(player, permission);
    }

    public String option(Player player, String permission) {
        if (chat == null)
            return "";
        return this.chat.getPlayerInfoString(player, permission, "");
    }

    /*
     * Gets a Permissions Prefix
     */
    public String prefix(Player player) {
        if (chat == null)
            return "";
        String prefix = this.chat.getPlayerPrefix(player);
        return prefix != null ? prefix : "";
    }

    private void whois(CommandSender sender, String[] args) {
        Player p = null;
        if (sender instanceof Player && args.length == 0) {
            p = (Player) sender;
        }

        for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
            if (pl.getName().contains(args[0])) {
                p = pl;
                break;
            }
        }
        if (p != null) {
            Location pLoc = p.getLocation();
            sender.sendMessage(replaceColors("&a----  " + colorize(p) + "&a----"));
            if (sender instanceof Player && !has((Player) sender, "whosthere.admin"))  {
                return;
            }
            sender.sendMessage(replaceColors("&aLoc: &d" + pLoc.getBlockX() + "&a, &d" + pLoc.getBlockY() + "&a, &d" + pLoc.getBlockZ() + "&a on: &d" + pLoc.getWorld().getName()));
            sender.sendMessage(replaceColors("&aIP: &d" + p.getAddress().getAddress().getHostAddress().toString()));
        } else if (!checkOfflinePlayer(args[0], sender)) {
            sender.sendMessage("No player with name " + args[0] + " was found on the server");
        }
    }

    private boolean checkOfflinePlayer(String playerName, CommandSender sender) {
        OfflinePlayer op = Bukkit.getServer().getOfflinePlayer(playerName);
        if (op == null)
            return false;
        else {
            sender.sendMessage(replaceColors("&aLast Online: &d" + dateFormat.format(new Date(op.getLastPlayed()))));
            return true;
        }
    }

    /*
     * Sends a limited who list to the command sender
     */
    private void whoCommand(CommandSender sender, String[] args) {
        World world = null;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("staff")) {
                whoStaff(sender);
                return;
            }
            world = getServer().getWorld(args[0]);
        }
        String playerList = "";
        int i = 0;
        int remainingChars = charsPerLine;
        for(Player player : Bukkit.getOnlinePlayers()) {
            if ((world == null && args.length == 0) || (world != null && player.getWorld().equals(world)) || (world == null && player.getName().contains(args[0]))) {
                if (remainingChars - player.getName().length() < 0) {
                    playerList += lineBreak;
                    remainingChars = charsPerLine;
                }
                //Normalize our color in case we have bleed-through
                playerList += ChatColor.WHITE;
                //If this isn't a newline lets put in our spacer
                if (remainingChars != charsPerLine)
                    playerList += "  ";
                //Add the colorized playername to the list
                playerList += colorize(player);
                remainingChars -= (player.getName().length() + 2);
                i++;
            }
        }
        if (i == 0 && world == null && args.length > 0) {
            sender.sendMessage("No players found with that name.");
        } else if (i == 0 && world != null) {
            sender.sendMessage("No players were found on " + world.getName());
        }  else if (args.length == 0) {
            String message = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + "/" + this.getServer().getMaxPlayers() + ChatColor.WHITE + " players online:" + lineBreak + playerList;
            sendWrappedText(sender, message);
        } else if (world != null) {
            String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players on " + world.getName() + ":" + lineBreak + playerList;
            sendWrappedText(sender, message);
        } else {
            String message = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria:" + lineBreak + playerList;
            sendWrappedText(sender, message);
        }
    }

    /**
     * Performs a list of who online is staff
     * @param sender
     * @param args
     */
    private void whoStaff(CommandSender sender) {
        String playerList = "";
        int i = 0;
        int remainingChars = charsPerLine;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("whosthere.staff")) {
                if (remainingChars - player.getName().length() < 0) {
                    playerList += lineBreak;
                    remainingChars = charsPerLine;
                }
                //If this isn't a newline lets put in our spacer
                if (remainingChars != charsPerLine)
                    playerList += "  ";
                //Add the colorized playername to the list
                playerList += colorize(player);
                remainingChars -= (player.getName().length() + 2);
                //Normalize our color in case we have bleed-through
                playerList += ChatColor.WHITE;
                i++;
            }
        } if (i == 0) {
            sender.sendMessage("No staff ar currently online!");
        } else {
            String message = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + ChatColor.WHITE + " staff online:" + lineBreak + playerList;
            sendWrappedText(sender, message);
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
        message += p.getName();
        return replaceColors(message);
    }

    /**
     * Add colorization based on options selected to the tabname
     * 
     * @param p
     * @return
     */
    private String colorizeTabName (Player p) {
        String message = "";
        if (prefixTabName) {
            message += prefix(p);
        }
        if (colorOptionTabName && colorOption != "" && colorOption != null) {
            message += option(p, colorOption);
        }
        message += p.getName();
        return replaceColors(message);
    }

    /**
     * Takes a string and replaces &# color codes with ChatColors
     * 
     * @param message
     * @return
     */
    private String replaceColors (String message) {
        return message.replaceAll("(?i)&([a-f0-9])", "\u00A7$1");
    }


    private void sendWrappedText(CommandSender sender, String message) {
        for(String messageLine : message.split(lineBreak)) {
            sender.sendMessage(messageLine);
        }
    }

    public class PlayerComparator implements Comparator<Player> {

        @Override
        public int compare(Player p1, Player p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }
    
    public class WhoPlayerListener implements Listener {

        WhosThere plugin;

        public WhoPlayerListener(WhosThere plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            final Player player = event.getPlayer();
            if (displayOnLogin) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getServer().getPluginCommand("who").execute(player, "who", new String[0]);
                    }
                }, 1L);

            }
            if (prefixTabName || colorOptionTabName) {
                String listName = colorizeTabName(player);
                if (listName.length() > 16)
                    listName = listName.substring(0, 15);
                player.setPlayerListName(listName);
            }
        }
    }
}
