package at.junction.tier2;

import at.junction.tier2.database.Ticket;
import at.junction.tier2.database.Ticket.TicketStatus;
import at.junction.tier2.database.TicketTable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Tier2 extends JavaPlugin {
    public Configuration config;
    TicketTable ticketTable;
    Scoreboard board;
    Team assistanceTeam;
    public Logger logger;

    private AbstractPermissionAPI perms = null;

    private static final String[] apiList = {
            "at.junction.tier2.permission.PexAPI"
    };

    @Override
    public void onEnable() {
        File cfile = new File(getDataFolder(), "config.yml");
        if (!cfile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        config = new Configuration(this);
        config.load();

        logger = this.getLogger();

        setupDatabase();

        setupScoreboards();

        ticketTable = new TicketTable(this);
        Tier2Listener listener = new Tier2Listener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        //Load our logblock listener iif logblock is loaded
        if (getServer().getPluginManager().getPlugin("LogBlock") != null){
            LogblockListener lbl = new LogblockListener(this);
            getServer().getPluginManager().registerEvents(lbl, this);
        }


        for (String name : apiList) {
            AbstractPermissionAPI api = AbstractPermissionAPI.getAPI(this, name);
            if (api != null) {
                perms = api;
                break;
            }
        }

        if (perms == null) {
            /* We have no permissions API - Die (Probably something better can be done) */
            logger.severe("No permissions API - Please install either PEX or bPermissions");
            this.setEnabled(false);
        }

        if (this.isEnabled()){
            logger.info("Tier2 Enabled");
        } else {
            logger.severe("Tier2 was not Enabled");
        }

    }

    @Override
    public void onDisable() {
        for (Player online : getServer().getOnlinePlayers()) {
            if (online.hasMetadata("assistance")) {
                toggleMode(online);
            }
        }
        logger.info("Tier2 Disabled");
    }

    void setupScoreboards() {
        board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (board.getTeam("assistance") == null) {
            assistanceTeam = board.registerNewTeam("assistance");
        } else {
            assistanceTeam = board.getTeam("assistance");
        }
        if (config.COLORNAMES) {
            assistanceTeam.setPrefix(config.NAMECOLOR + "");
        }
        assistanceTeam.setCanSeeFriendlyInvisibles(true);
    }

    void setupDatabase() {
        try {
            getDatabase().find(Ticket.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
        }
    }

    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<>();
        list.add(Ticket.class);
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {


        if (command.getName().equalsIgnoreCase("modreq")) { // Input a ticket.
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /modreq <details of what you need>");
            }


            if ((ticketTable.getNumTicketFromUser(sender.getName()) < 5) || sender.hasPermission("tier2.staff")) {
                String message = args[0];
                for (int i = 1; i < args.length; i++) {
                    message += " " + args[i];
                }

                Ticket ticket = new Ticket();
                ticket.setPlayerName(sender.getName());
                ticket.setTicket(message);
                ticket.setTicketTime(System.currentTimeMillis());

                String ticketLocation;
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ticketLocation = String.format("%s,%f,%f,%f,%f,%f", player.getWorld().getName(),
                            player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                            player.getLocation().getPitch(), player.getLocation().getYaw());
                } else {
                    ticketLocation = "world,0,0,0,0,0";
                }

                ticket.setTicketLocation(ticketLocation);
                ticket.setStatus(TicketStatus.OPEN);
                ticketTable.save(ticket);

                msgStaff(sender.getName() + " opened a new ticket.");
                sender.sendMessage(ChatColor.GOLD + "Ticket has been filed. Please be patient for staff to complete your request.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You already have five open tickets. Please wait for these to be closed, or close some yourself.");
                return true;
            }

        } else if (command.getName().equalsIgnoreCase("check")) { // Check for list, or check the details of a particular ticket.
            if (args.length > 0) { // If there's an ID to check for.
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                } catch (NumberFormatException | NullPointerException ex) { // If arg[0] wasn't an integer.
                    sender.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                if (sender.hasPermission("tier2.ticket") || sender.getName().equals(ticket.getPlayerName())) { // Check for permission to view ticket.
                    msgTicket(sender, ticket);
                    return true;
                } else { // If they don't have permission to view a specific ticket.
                    sender.sendMessage(ChatColor.RED + "You do not have permission to view this ticket.");
                    return true;
                }
            } else { // List all tickets.
                List<Ticket> tickets = new ArrayList<>();

                if (sender.hasPermission("tier2.ticket")) {
                    tickets.addAll(ticketTable.getAllTickets());
                } else {
                    tickets.addAll(ticketTable.getUserTickets(sender.getName()));
                }
                msgTickets(sender, tickets);

                return true;
            }
        } else if (command.getName().equalsIgnoreCase("claim")) { // Notify other staff of the claim.
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;
            if (args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.CLAIMED);
                    ticket.setAssignedMod(player.getName());
                    ticketTable.save(ticket);
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " claimed #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("tpclaim")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;
            if (args.length > 0) {
                Ticket ticket;
                if (!player.hasMetadata("assistance")) {
                    toggleMode(player);
                }
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.CLAIMED);
                    ticket.setAssignedMod(player.getName());
                    ticketTable.save(ticket);

                    Location loc;

                    String world;
                    double x, y, z;
                    float pitch, yaw;
                    String[] split = ticket.getTicketLocation().split(",");
                    world = split[0];
                    x = Double.parseDouble(split[1]);
                    y = Double.parseDouble(split[2]);
                    z = Double.parseDouble(split[3]);
                    if (split.length == 6) {
                        pitch = Float.parseFloat(split[4]);
                        yaw = Float.parseFloat(split[5]);
                        loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
                    } else {
                        loc = new Location(getServer().getWorld(world), x, y, z);
                    }
                    player.teleport(loc);
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " claimed #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("tp-id")) { // Teleport to a specified ID without claiming
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            if (args.length > 0) {
                Ticket ticket;
                if (!player.hasMetadata("assistance")) {
                    toggleMode(player);
                }
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    Location loc;

                    String world;
                    double x, y, z;
                    float pitch, yaw;
                    String[] split = ticket.getTicketLocation().split(",");
                    world = split[0];
                    x = Double.parseDouble(split[1]);
                    y = Double.parseDouble(split[2]);
                    z = Double.parseDouble(split[3]);
                    if (split.length == 6) {
                        pitch = Float.parseFloat(split[4]);
                        yaw = Float.parseFloat(split[5]);
                        loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
                    } else {
                        loc = new Location(getServer().getWorld(world), x, y, z);
                    }
                    player.teleport(loc);
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("unclaim")) { // Notify other staff of the unclaim.
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            if (args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.OPEN);
                    ticket.setAssignedMod("");
                    ticketTable.save(ticket);
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " is no longer handling #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("done")) { // Close a ticket with an optional message.
            if (args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setCloseTime(System.currentTimeMillis());
                    if (args.length > 1) { // If we have a message to attach.
                        String message = args[1]; // arg[0] is the ticket ID.
                        for (int i = 2; i < args.length; i++) {
                            message += " " + args[i];
                        }
                        ticket.setCloseMessage(message);
                    }
                    ticket.setAssignedMod(sender.getName()); // Just in case they didn't claim it.
                    ticket.setStatus(TicketStatus.CLOSED);
                    ticketTable.save(ticket);
                    msgStaff(sender.getName() + " closed #" + args[0] + ".");
                    if (getServer().getPlayer(ticket.getPlayerName()) != null) {
                        String message;
                        if ("".equals(ticket.getCloseMessage())) {
                            message = "No close message.";
                        } else {
                            message = ticket.getCloseMessage();
                        }
                        getServer().getPlayer(ticket.getPlayerName()).sendMessage(ChatColor.GOLD + "Ticket " + ticket.getId() + " closed: " + message);
                    }
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("elevate")) { // Elevate above first-level moderators to a particular group (predefined enums in Ticket.java).
            if (args.length == 2) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                } catch (NumberFormatException | NullPointerException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                ticket.setStatus(TicketStatus.ELEVATED);
                if (config.GROUPS.contains(args[1].toLowerCase())) {
                    ticket.setElevationGroup(args[1].toLowerCase());
                    ticketTable.save(ticket);
                } else {
                    sender.sendMessage(ChatColor.RED + "That is an invalid elevation group.");
                    String groups = "";
                    for (String group : config.GROUPS) {
                        groups += group + ", ";
                    }
                    sender.sendMessage(ChatColor.RED + "Available groups: " + groups.substring(0, groups.length() - 2));
                    return false;
                }
                sender.sendMessage(ChatColor.GOLD + "Elevating #" + args[0] + " to " + args[1].toUpperCase() + ".");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid parameters!");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("staff")) { // Get a list of staff.
            String stafflist = "";
            for (Player online : getServer().getOnlinePlayers()) {
                if (online.hasPermission("tier2.ticket") && !online.hasMetadata("vanished") && !online.hasMetadata("hidden")) {
                    stafflist += online.getDisplayName() + ", ";
                }
            }

            if (stafflist.equals("")) {
                sender.sendMessage(ChatColor.GOLD + "No staff are currently online. :(");
                sender.sendMessage(ChatColor.GOLD + "You can still make a request with \"/modreq <your request here>\", though!");
                sender  .sendMessage(ChatColor.GOLD + "One of the server staff will be with you as soon as possible.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Online Staff:");
                sender.sendMessage(ChatColor.GOLD + stafflist.substring(0, stafflist.length() - 2));
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("mode")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            toggleMode(player);
            return true;
        } else if (command.getName().equalsIgnoreCase("vanish")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            toggleVanish(player, true);
            return true;
        } else if (command.getName().equalsIgnoreCase("unvanish")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            toggleVanish(player, false);
            return true;
        } else if (command.getName().equalsIgnoreCase("hide")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            if (player.hasMetadata("hidden")) {
                player.sendMessage(ChatColor.GOLD + "You are already hidden! Type /unhide to add yourself to the staff listing");

            } else {
                player.setMetadata("hidden", new FixedMetadataValue(this, true));
            }

        } else if (command.getName().equalsIgnoreCase("unhide")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This is only usable by players, sorry!");
                return true;
            }
            Player player = (Player) sender;

            if (!player.hasMetadata("hidden")) {
                player.sendMessage(ChatColor.GOLD + "You are not hidden! Type /hide to remove yourself from the staff listing");

            } else {
                player.removeMetadata("hidden", this);
            }
        } else if (command.getName().equalsIgnoreCase("tier2-reload")) {
            config.save();
            config.load();
        } else if (command.getName().equalsIgnoreCase("supermode")) {
            //Require command to be sent from console
            if (!(sender.getName().equals("CONSOLE"))){
                sender.sendMessage(ChatColor.RED + "Please execute command from console");
                return true;
            }
            if (args.length < 2){
                sender.sendMessage(ChatColor.RED + "Usage: /supermode <player> <reason>");
                return true;
            }
            //Get player (args[0])
            Player player = getServer().getPlayer(args[0]);
            if (player == null){
                sender.sendMessage(ChatColor.RED + "This player is not online");
            } else if (!player.hasPermission("tier2.superpowers")) {
                sender.sendMessage(ChatColor.RED + "This player does not have superpowers");
            } else if (!player.hasMetadata("assistance")){
                sender.sendMessage(ChatColor.RED + "Player must be in assistance mode to gain superpowers");
            } else {
                //You've passed the tests - continue

                StringBuilder reason = new StringBuilder();
                for (int i=1; i<args.length; i++)
                    reason.append(args[i]).append(" ");
                getServer().dispatchCommand(player, String.format("sc I have gained super powers. Reason: %s", reason.toString()));
                //Add correct group here
                perms.addSuperpowers(player);
                //You are now in superpower mode. Give diamond block head
                player.getInventory().getHelmet().setType(Material.IRON_BLOCK);
                player.setMetadata("superpowers", new FixedMetadataValue(this, "batman"));
            }
        }
        return true;
    }

    void toggleVanish(Player player, boolean vanish) {
        if (vanish) {
            if (!player.hasMetadata("vanished")) {
                for (Player online : getServer().getOnlinePlayers()) {
                    if (!online.hasPermission("tier2.vanish.see")) {
                        online.hidePlayer(player);
                    }
                }
                player.setMetadata("vanished", new FixedMetadataValue(this, true));
                player.sendMessage(ChatColor.GOLD + "You are now vanished.");
            } else {
                player.sendMessage(ChatColor.GOLD + "You are already vanished!");
            }
        } else {
            if (player.hasMetadata("vanished")) {
                for (Player online : getServer().getOnlinePlayers()) {
                    online.showPlayer(player);
                }
                player.removeMetadata("vanished", this);
                player.sendMessage(ChatColor.GOLD + "You are no longer vanished.");
            } else {
                player.sendMessage(ChatColor.GOLD + "You are already visible!");
            }
        }
    }

    public void toggleMode(Player player) {

        if (player.hasMetadata("assistance")) { // Remove metadata and restore to old "player".
            logger.info(player.getName() + " left MODE at " + player.getLocation().toString());
            if (player.isOp()){
                player.setOp(false);
            }
            player.removeMetadata("assistance", this);
            if (player.hasMetadata("superpowers")){
                player.removeMetadata("superpowers", this);
                getServer().dispatchCommand(player, "sc I have lost my superpowers");
                perms.removeSuperpowers(player);
            }
            ItemStack[] oldinv = (ItemStack[]) player.getMetadata("inventory").get(0).value();
            ItemStack[] oldarm = (ItemStack[]) player.getMetadata("armor").get(0).value();
            Location oldloc = (Location) player.getMetadata("location").get(0).value();
            //restore previous data
            player.setExp((float) player.getMetadata("exp").get(0).value());
            player.setFoodLevel((int) player.getMetadata("food").get(0).value());
            player.setFallDistance((float) player.getMetadata("fallDist").get(0).value()); //Reset fall distance
            player.getInventory().clear();
            player.setNoDamageTicks(60);
            player.teleport(oldloc);
            player.setGameMode(config.GAMEMODE);
            player.setFlying(player.getGameMode() == org.bukkit.GameMode.CREATIVE);
            player.setAllowFlight(player.getGameMode() == org.bukkit.GameMode.CREATIVE);
            player.setCanPickupItems(true);
            player.getInventory().setContents(oldinv);
            player.getInventory().setArmorContents(oldarm);

            //Unvanish
            if (player.hasMetadata("vanished"))
                toggleVanish(player, false);

            //Change groups
            perms.removeTier2Groups(player, config.GROUPPREFIX);
            if (config.COLORNAMES && player.hasMetadata("displayName")) {
                player.setDisplayName((String)player.getMetadata("displayName").get(0).value());
            } else if (config.COLORNAMES && !player.hasMetadata("displayName")) {
                player.setDisplayName(player.getName());
            }

            //Swap Team
            if (player.hasMetadata("team")) {
                Team oldteam = (Team) player.getMetadata("team").get(0).value();
                oldteam.addPlayer(player);
            } else {
                assistanceTeam.removePlayer(player);
            }

            //Let the player know they have left assistance mode
            player.playEffect(player.getLocation(), org.bukkit.Effect.EXTINGUISH, null);
            player.sendMessage(ChatColor.GOLD + "You are no longer in assistance mode.");
        } else { // Add metadata and enter assistance mode at the current location.
            logger.info(player.getName() + " entering MODE at " + player.getLocation().toString());

            player.sendMessage(config.MODE_MOTD);
            if (player.hasPermission("tier2.superpowers")){
                player.sendMessage(config.NAMECOLOR + "Some permissions now require supermode");
                player.sendMessage(config.NAMECOLOR + "To enable supermode, do `supermode <IGN> <reason>` on the console");
                player.sendMessage(config.NAMECOLOR + "This includes use of sudo and worldedit");
            }

            player.saveData();
            player.setMetadata("assistance", new FixedMetadataValue(this, true));
            Location playerloc = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + 0.5, player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()); // An attempted block-stuck fix.
            //Get inventory AND armor
            ItemStack[] playerinv = player.getInventory().getContents();
            ItemStack[] playerarm = player.getInventory().getArmorContents();

            //save old player data into metadata
            player.setMetadata("location", new FixedMetadataValue(this, playerloc));
            player.setMetadata("inventory", new FixedMetadataValue(this, playerinv));
            player.setMetadata("armor", new FixedMetadataValue(this, playerarm));
            player.setMetadata("exp", new FixedMetadataValue(this, player.getExp()));
            player.setMetadata("food", new FixedMetadataValue(this, player.getFoodLevel()));
            player.setMetadata("fallDist", new FixedMetadataValue(this, player.getFallDistance()));
            player.setAllowFlight(true);
            player.setCanPickupItems(false);


            //Remove armor
            player.getInventory().clear();
            player.getInventory().setHelmet(new ItemStack(org.bukkit.Material.GLASS));
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);

            //Change groups
            perms.addTier2Groups(player, config.GROUPPREFIX);
            if (config.COLORNAMES) {
                player.setMetadata("displayName", new FixedMetadataValue(this, player.getDisplayName()));
                player.setDisplayName(config.NAMECOLOR + player.getName() + ChatColor.RESET);
            }
            for (String item : config.ITEMS.keySet()) { // Add items as per config.yml.
                ItemStack itemstack = new ItemStack(Material.valueOf(item), config.ITEMS.get(item));
                player.getInventory().addItem(itemstack);
            }

            //Swap Team
            if (board.getPlayerTeam(player) != null) {
                Team playerteam = board.getPlayerTeam(player);
                player.setMetadata("team", new FixedMetadataValue(this, playerteam));
            }
            assistanceTeam.addPlayer(player);

            //Let the player know they have entered assistance mode
            player.playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, null);
            player.sendMessage(ChatColor.GOLD + "You are now in assistance mode.");
        }
    }

    void msgStaff(String message) {
        for (Player online : getServer().getOnlinePlayers()) {
            if (online.hasPermission("tier2.ticket")) {
                online.sendMessage(ChatColor.GOLD + message);
            }
        }
    }

    void msgTicket(CommandSender player, Ticket ticket) {
        player.sendMessage(ChatColor.GOLD + "== Ticket #" + ticket.getId() + " ==");
        if (ticket.getStatus() == TicketStatus.ELEVATED) {
            player.sendMessage(ChatColor.GOLD + "Elevated To: " + ticket.getElevationGroup());
        }
        player.sendMessage(ChatColor.GOLD + "Opened By: " + ticket.getPlayerName());
        player.sendMessage(ChatColor.GOLD + "Description: " + ticket.getTicket());
        player.sendMessage(ChatColor.GOLD + "Status: " + ticket.getStatus().toString());
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            player.sendMessage(ChatColor.GOLD + "Closed By: " + ticket.getAssignedMod());
            player.sendMessage(ChatColor.GOLD + "Close Message: " + ticket.getCloseMessage());
        }
    }

    void msgTickets(CommandSender player, List<Ticket> tickets) {
        player.sendMessage(ChatColor.GOLD + "== Active Tickets (" + tickets.size() + ") ==");
        HashMap<String, Integer> elevatedTickets = new HashMap<>();
        for (Ticket ticket : tickets) {

            // Count the number
            if (ticket.getStatus() == TicketStatus.ELEVATED) {
                Integer currentTickets = elevatedTickets.get(ticket.getElevationGroup());
                currentTickets = (currentTickets == null ? 1 : currentTickets + 1);
                elevatedTickets.put(ticket.getElevationGroup(), currentTickets);
            }
            // Check that it's either unelevated or they have the appropriate permissions.
            if (ticket.getStatus() != TicketStatus.ELEVATED
                    || perms.isInGroup(player, ticket.getElevationGroup())
                    || perms.isInGroup(player, config.GROUPPREFIX + ticket.getElevationGroup())) {
                player.sendMessage(ChatColor.DARK_AQUA + "#" + ticket.getId() + " by " + ticket.getPlayerName() + ":");
                String messageBody = ticket.getTicket();
                if (ticket.getTicket().length() > 25) {
                    messageBody = ticket.getTicket().substring(0, 26) + "...";
                }
                player.sendMessage(ChatColor.GOLD + ((ticket.getStatus() == TicketStatus.ELEVATED) ? (ChatColor.AQUA + "[" + ticket.getElevationGroup().toUpperCase() + "] " + ChatColor.GOLD) : "") + messageBody);
            }
        }
        if (elevatedTickets.size() > 0) {
            player.sendMessage(ChatColor.GOLD + "== Elevated Tickets (" + elevatedTickets.size() + ") ==");
            for (String group : elevatedTickets.keySet()) {
                player.sendMessage(ChatColor.AQUA + "[" + group.toUpperCase() + "] " + elevatedTickets.get(group));
            }
        }
    }

}
