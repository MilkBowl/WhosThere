/* WhosThere is copyright Nicholas Minkler
 * WhosThere is licensed under the CreativeCommons - Non-Commercial - No-Derivatives license 
 * It can be viewed at the following link
 * 
 * http://creativecommons.org/licenses/by-nc-nd/3.0/
 */
package com.sleaker.WhosThere;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM, dd - HH:mm");
    private static final int CHARS_PER_LINE = 52;
    private static final int LINES_PER_PAGE = 7;
    private static final String LINE_BREAK = "%LB%";

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
        } else if (command.getName().equalsIgnoreCase("findwho")) {
            if (sender instanceof Player) {
                if (!has((Player) sender, "whosthere.find")) {
                    sender.sendMessage("You don't have permission to do that.");
                    return true;
                }
            }
            if (args.length < 1) {
                sender.sendMessage("You must supply a search string.");
                return true;
            } else {
                findCommand(sender, args);
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
        if (p == null) {
            for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                if (pl.getDisplayName().toLowerCase().contains(args[0])) {
                    p = pl;
                    break;
                }
            }
        }
        if (p != null) {
            if (sender instanceof Player && !((Player) sender).canSee(p)) {
                if (!checkOfflinePlayer(args[0], sender)) {
                    sender.sendMessage("No player with name " + args[0] + " was found on the server");
                }
                return;
            }
            sender.sendMessage(replaceColors("&a----  " + colorize(p) + "&a----"));
            if (sender instanceof Player && !has((Player) sender, "whosthere.admin"))  {
                return;
            }
            Location pLoc = p.getLocation();
            sender.sendMessage(replaceColors("&aLoc: &d" + pLoc.getBlockX() + "&a, &d" + pLoc.getBlockY() + "&a, &d" + pLoc.getBlockZ() + "&a on: &d" + pLoc.getWorld().getName()));
            long temp = p.getFirstPlayed();
            sender.sendMessage(replaceColors("&aFirst Online: &d" + (temp != 0 ? dateFormat.format(new Date(temp)) : " unknown")));
            sender.sendMessage(replaceColors("&aIP: &d" + p.getAddress().getAddress().getHostAddress().toString()));
        } else if (!checkOfflinePlayer(args[0], sender)) {
            sender.sendMessage("No player with name " + args[0] + " was found on the server");
        }
    }

    private boolean checkOfflinePlayer(String playerName, CommandSender sender) {
        OfflinePlayer op = Bukkit.getServer().getOfflinePlayer(playerName);
        if (op == null || !op.hasPlayedBefore())
            return false;
        else {
            long temp = op.getFirstPlayed();
            sender.sendMessage(replaceColors("&aFirst Online: &d" + (temp != 0 ? dateFormat.format(new Date(temp)) : " unknown")));
            temp = op.getLastPlayed();
            sender.sendMessage(replaceColors("&aLast Online: &d" + (temp != 0 ? dateFormat.format(new Date(temp)) : " unknown")));
            return true;
        }
    }

    private void findCommand(CommandSender sender, String[] args) {
        int page = 0;
        if (args.length > 1) {
            try {
                Integer val = Integer.parseInt(args[1]);
                page = val - 1;
            } catch (NumberFormatException e) {
                // Ignore the extra argument if it doesn't parse
            }
        }
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player) sender).canSee(player)) {
                continue;
            }
            if (player.getName().toLowerCase().contains(args[0].toLowerCase())) {
                if (remainingChars - player.getName().length() < 0) {
                    playerList += LINE_BREAK;
                    remainingChars = CHARS_PER_LINE;
                }
                //Normalize our color in case we have bleed-through
                playerList += ChatColor.WHITE;
                //If this isn't a newline lets put in our spacer
                if (remainingChars != CHARS_PER_LINE) {
                    playerList += "  ";
                }
                //Add the colorized playername to the list
                playerList += colorize(player);
                remainingChars -= (player.getName().length() + 2);
                i++;
            }
        }
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        // Make sure we can display the page we selected
        if (page >= totalPages || page < 0) {
            page = 0;
        }
        if (i == 0 ) {
            sender.sendMessage("No players found with that name.");
        } else {
            String title = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            sendWrappedText(sender, title, lines, page);
        }
    }
    /*
     * Sends a limited who list to the command sender
     */
    private void whoCommand(CommandSender sender, String[] args) {
        World world = null;
        int page = 0;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("staff")) {
                whoStaff(sender, args);
                return;
            }
            world = getServer().getWorld(args[0]);
            if (world == null) {
                try {
                    Integer val = Integer.parseInt(args[0]);
                    page = val - 1;
                } catch (NumberFormatException e) {
                    // Ignore an exception here
                }
            }
        } 
        if (args.length > 1) {
            try {
                Integer val = Integer.parseInt(args[1]);
                page = val - 1;
            } catch (NumberFormatException e) {
                // Ignore the extra argument if it doesn't parse
            }
        }
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player) sender).canSee(player)) {
                continue;
            }
            if ((world == null && args.length == 0) || (world != null && player.getWorld().equals(world))) {
                if (remainingChars - player.getName().length() < 0) {
                    playerList += LINE_BREAK;
                    remainingChars = CHARS_PER_LINE;
                }
                //Normalize our color in case we have bleed-through
                playerList += ChatColor.WHITE;
                //If this isn't a newline lets put in our spacer
                if (remainingChars != CHARS_PER_LINE) {
                    playerList += "  ";
                }
                //Add the colorized playername to the list
                playerList += colorize(player);
                remainingChars -= (player.getName().length() + 2);
                i++;
            }
        }
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        // Make sure we can display the page we selected
        if (page >= totalPages || page < 0) {
            page = 0;
        } else if (i == 0) {
            page = 0;
            totalPages = 1;
        }
        if (i == 0 && world != null) {
            sender.sendMessage("No players were found on " + world.getName());
        } else if (world != null) {
            String title = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players on " + world.getName() + ". Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            sendWrappedText(sender, title, lines, page);
        } else {
            String title = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + "/" + this.getServer().getMaxPlayers() + ChatColor.WHITE + " players online. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            sendWrappedText(sender, title, lines, page);
        } 
    }

    /**
     * Performs a list of who online is staff
     * @param sender
     * @param args
     */
    private void whoStaff(CommandSender sender, String[] args) {
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        int page = 0;
        if (args.length > 1) {
            try {
                Integer val = Integer.parseInt(args[1]);
                page = val - 1;
            } catch (NumberFormatException e) {
                
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player) sender).canSee(player)) {
                continue;
            }
            if (player.hasPermission("whosthere.staff")) {
                if (remainingChars - player.getName().length() < 0) {
                    playerList += LINE_BREAK;
                    remainingChars = CHARS_PER_LINE;
                }
                //If this isn't a newline lets put in our spacer
                if (remainingChars != CHARS_PER_LINE) {
                    playerList += "  ";
                }
                //Add the colorized playername to the list
                playerList += colorize(player);
                remainingChars -= (player.getName().length() + 2);
                //Normalize our color in case we have bleed-through
                playerList += ChatColor.WHITE;
                i++;
            }
        } 
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        // Make sure we can display the page we selected
        if (page >= totalPages || page < 0) {
            page = 0;
        }        
        if (i == 0) {
            sender.sendMessage("No staff are currently online!");
        } else {
            String title = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + ChatColor.WHITE + " staff online. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            sendWrappedText(sender, title, lines, page);
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
        return message.replaceAll("(?i)&([a-fk-o0-9])", "\u00A7$1");
    }
    
    private void sendWrappedText(CommandSender sender, String header, List<String> lines, int pageNumber) {
        sender.sendMessage(header);
        int end = (pageNumber + 1) * LINES_PER_PAGE;
        if (end > lines.size()) {
            end = lines.size();
        }
        for(int i = pageNumber * LINES_PER_PAGE; i < end; i++) {
            sender.sendMessage(lines.get(i));
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
