package at.junction.tier2;

import at.junction.tier2.database.Ticket;
import at.junction.tier2.database.Ticket.TicketStatus;
import at.junction.tier2.database.TicketTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
        if (getServer().getPluginManager().getPlugin("LogBlock") != null) {
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

        if (this.isEnabled()) {
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
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        switch (command.getName().toLowerCase()) {
            case "modreq":
                ///modreq <details of request here>
                if (args.length == 0) {
                    return false;
                } else if ((ticketTable.getNumTicketFromUser(sender.getName()) < 5) || sender.hasPermission("tier2.staff")) {
                    //Only let players have 5 tickets open - let staff have inf open
                    String message = join(' ', args);

                    Ticket ticket = new Ticket();
                    ticket.setPlayerName(sender.getName());
                    ticket.setTicket(message);
                    ticket.setTicketTime(System.currentTimeMillis());

                    String ticketLocation;
                    if (player != null) {
                        ticketLocation = String.format("%s,%f,%f,%f,%f,%f", player.getWorld().getName(),
                                player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                                player.getLocation().getPitch(), player.getLocation().getYaw());
                    } else {
                        ticketLocation = "world,0,0,0,0,0";
                    }

                    ticket.setTicketLocation(ticketLocation);
                    ticket.setStatus(TicketStatus.OPEN);
                    ticketTable.save(ticket);

                    msgStaff(sender.getName(),  " opened a new ticket.");
                    sender.sendMessage(String.format("%sTicket has been filed. Please be patient for staff to complete your request.", ChatColor.GOLD));
                } else {
                    sender.sendMessage(String.format("%sYou already have five open tickets. Please wait for these to be closed, or close some yourself.", ChatColor.RED));
                }
                break;
            case "check":
                ///check [<id>]
                if (args.length > 0) { // If there's an ID to check for.
                    Ticket ticket;
                    try {
                        ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                        if (sender.hasPermission("tier2.ticket") || sender.getName().equals(ticket.getPlayerName())) { // Check for permission to view ticket.
                            msgTicket(sender, ticket);
                        } else { // If they don't have permission to view a specific ticket.
                            sender.sendMessage(String.format("%sYou do not have permission to view this ticket.", ChatColor.RED));
                        }
                    } catch (NumberFormatException | NullPointerException ex) { // If arg[0] wasn't an integer.
                        sender.sendMessage(String.format("%sInvalid ticket ID!", ChatColor.RED));
                    }
                } else { // List all tickets.
                    List<Ticket> tickets = new ArrayList<>();

                    if (sender.hasPermission("tier2.ticket")) {
                        tickets.addAll(ticketTable.getAllTickets());
                    } else {
                        tickets.addAll(ticketTable.getUserTickets(sender.getName()));
                    }
                    msgTickets(sender, tickets);
                }
                break;
            case "claim":
                ///claim <id>
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else {
                    if (args.length > 0) {
                        Ticket ticket;
                        try {
                            ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                            ticket.setStatus(TicketStatus.CLAIMED);
                            ticket.setAssignedMod(player.getName());
                            ticketTable.save(ticket);
                            msgStaff(player.getName(), " claimed #", args[0], ".");
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(String.format("%sInvalid ticket ID!",  ChatColor.RED));
                        }

                    } else {
                        player.sendMessage(String.format("%sYou did not specify a ticket ID!", ChatColor.RED));
                    }
                }
                break;
            case "tpclaim":
                ///tpclaim <id>
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else if (args.length > 0) {
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
                        pitch = Float.parseFloat(split[4]);
                        yaw = Float.parseFloat(split[5]);
                        loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
                        player.teleport(loc);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(String.format("%sInvalid ticket ID!", ChatColor.RED));
                    }
                    msgStaff(player.getName(), " claimed #", args[0], ".");
                } else {
                    player.sendMessage(String.format("%sYou did not specify a ticket ID!", ChatColor.RED));
                }
                break;
            case "tp-id":
                ///tp-id <id>
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else if (args.length > 0) {
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
                        pitch = Float.parseFloat(split[4]);
                        yaw = Float.parseFloat(split[5]);
                        loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);

                        player.teleport(loc);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(String.format("%sInvalid ticket ID!", ChatColor.RED));
                    }
                } else {
                    player.sendMessage(String.format("%sYou did not specify a ticket ID!", ChatColor.RED));
                }

                break;

            case "unclaim":
                ///unclaim <id>
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else if (args.length > 0) {
                    Ticket ticket;
                    try {
                        ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                        ticket.setStatus(TicketStatus.OPEN);
                        ticket.setAssignedMod("");
                        ticketTable.save(ticket);
                        msgStaff(player.getName(), " is no longer handling #", args[0], ".");
                    } catch (NumberFormatException ex) {
                        player.sendMessage(String.format("%sInvalid ticket ID!", ChatColor.RED));
                    }
                } else {
                    player.sendMessage(String.format("%sYou did not specify a ticket ID!", ChatColor.RED));
                }
                break;
            case "done":
                ///done <id> [<message>]
                if (args.length > 0) {
                    Ticket ticket;
                    try {
                        ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                        ticket.setCloseTime(System.currentTimeMillis());
                        String message = args.length > 1 ? join(' ', 1, args) : "Ticket closed.";
                        ticket.setCloseMessage(message);
                        ticket.setAssignedMod(sender.getName()); // Just in case they didn't claim it.
                        ticket.setStatus(TicketStatus.CLOSED);
                        ticketTable.save(ticket);
                        msgStaff(sender.getName(), " closed #", args[0], ".");
                        if (getServer().getPlayer(ticket.getPlayerName()) != null) {
                            getServer().getPlayer(ticket.getPlayerName()).sendMessage(String.format("%sTicket %s closed: %s", ChatColor.GOLD, + ticket.getId(), ticket.getCloseMessage()));
                        }
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(String.format("%sInvalid ticket ID!", ChatColor.RED));
                    }
                } else {
                    sender.sendMessage(String.format("%sYou did not specify a ticket ID!", ChatColor.RED));
                }
                break;
            case "elevate":
                ///elevate <id> <group>
                if (args.length == 2) {
                    Ticket ticket;
                    try {
                        ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                        ticket.setStatus(TicketStatus.ELEVATED);
                        if (config.ELEVATION_GROUPS.contains(args[1].toLowerCase())) {
                            ticket.setElevationGroup(args[1].toLowerCase());
                            ticketTable.save(ticket);
                            sender.sendMessage(String.format("%sElevating #%s to %s.", ChatColor.GOLD, args[0], args[1].toUpperCase()));
                        } else {
                            sender.sendMessage(String.format("%sThat is an invalid elevation group.", ChatColor.RED));
                            sender.sendMessage(String.format("%sAvailable groups: %s", ChatColor.RED, join(' ', (String[]) config.ELEVATION_GROUPS.toArray())));
                        }

                    } catch (NumberFormatException | NullPointerException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid parameters!");
                }
                break;
            case "staff":
                ///staff
                StringBuilder stafflist = new StringBuilder();
                for (Player online : getServer().getOnlinePlayers()) {
                    if (online.hasPermission("tier2.ticket") && !online.hasMetadata("vanished") && !online.hasMetadata("hidden")) {
                        stafflist.append(online.getDisplayName()).append(", ");
                    }
                }

                if (stafflist.length() == 0) {
                    sender.sendMessage(String.format("%sNo staff are currently online. :(", ChatColor.GOLD));
                    sender.sendMessage(String.format("%sYou can still make a request with \"/modreq <your request here>\", though!", ChatColor.GOLD));
                    sender.sendMessage(String.format("%sOne of the server staff will be with you as soon as possible.", ChatColor.GOLD));
                } else {
                    sender.sendMessage(String.format("%sOnline Staff:", ChatColor.GOLD));
                    sender.sendMessage(String.format("%s%s", ChatColor.GOLD, stafflist.substring(0, stafflist.length() - 2)));
                }

                break;
            case "mode":
                ///mode
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else {
                    toggleMode(player);
                }
                break;
            case "vanish":
                ///vanish
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));

                } else {
                    toggleVanish(player, true);
                }
                break;
            case "unvanish":
                ///unvanish
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else {

                    toggleVanish(player, false);
                }

                break;
            case "hide":
                ///hide
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));

                } else if (player.hasMetadata("hidden")) {
                    player.sendMessage(String.format("%sYou are already hidden! Type /unhide to add yourself to the staff listing", ChatColor.GOLD));

                } else {
                    player.setMetadata("hidden", new FixedMetadataValue(this, true));
                }
                break;
            case "unhide":
                ///unhide
                if (player == null) {
                    sender.sendMessage(String.format("%sThis is only usable by players, sorry!", ChatColor.RED));
                } else if (!player.hasMetadata("hidden")) {
                    player.sendMessage(String.format("%sYou are not hidden! Type /hide to remove yourself from the staff listing", ChatColor.GOLD));

                } else {
                    player.removeMetadata("hidden", this);
                }
                break;
            case "tier2-reload":
                ///tier2-reload
                config.save();
                config.load();
                break;
            case "supermode":
                //supermode <user> <reason>
                //Require command to be sent from console
                if (!(sender.getName().equals("CONSOLE"))) {
                    sender.sendMessage(String.format("%sPlease execute command from console", ChatColor.RED));
                } else if (args.length < 2) {
                    sender.sendMessage(String.format("%sUsage: /supermode <player> <reason>", ChatColor.RED));
                } else {
                    //Get player (args[0])
                    player = getServer().getPlayer(args[0]);
                    if (player == null) {
                        sender.sendMessage(String.format("%sThis player is not online", ChatColor.RED));
                    } else if (!player.hasPermission("tier2.superpowers")) {
                        sender.sendMessage(String.format("%sThis player does not have superpowers", ChatColor.RED));
                    } else if (!player.hasMetadata("assistance")) {
                        sender.sendMessage(String.format("%sPlayer must be in assistance mode to gain superpowers", ChatColor.RED));
                    } else {
                        //You've passed the tests - continue

                        StringBuilder reason = new StringBuilder();
                        for (int i = 1; i < args.length; i++)
                            reason.append(args[i]).append(" ");
                        getServer().dispatchCommand(player, String.format("transmission:staffchat I have gained super powers. Reason: %s", reason.toString()));
                        //Add correct group here
                        perms.addGroups(player, config.SUPER_PREFIX);
                        //You are now in superpower mode. Give diamond block head
                        player.getInventory().getHelmet().setType(Material.IRON_BLOCK);
                        player.setMetadata("superpowers", new FixedMetadataValue(this, "batman"));
                    }
                }
                break;
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
                player.getInventory().setHelmet(new ItemStack(org.bukkit.Material.ICE));
                if (getServer().getOnlinePlayers().length >= 10) {
                    getServer().broadcastMessage(String.format("%s%s left the game.", ChatColor.YELLOW, player.getName()));
                }

                player.sendMessage(String.format("%sYou are now vanished.", ChatColor.GOLD));
            } else {
                player.sendMessage(String.format("%sYou are already vanished!", ChatColor.GOLD));
            }
        } else {
            if (player.hasMetadata("vanished")) {
                for (Player online : getServer().getOnlinePlayers()) {
                    online.showPlayer(player);
                }
                player.removeMetadata("vanished", this);
                player.getInventory().setHelmet(new ItemStack(player.hasMetadata("superpowers") ? Material.IRON_BLOCK : Material.GLASS));
                //getServer().broadcastMessage(String.format("%s%s joined the game.", ChatColor.YELLOW, player.getName()));
                player.sendMessage(String.format("%sYou are no longer vanished.", ChatColor.GOLD));
            } else {
                player.sendMessage(String.format("%sYou are already visible!", ChatColor.GOLD));
            }
        }
    }

    public void toggleMode(Player player) {

        if (player.hasMetadata("assistance")) { // Remove metadata and restore to old "player".
            logger.info(String.format("%s left MODe at %s", player.getName(), player.getLocation().toString()));
            if (player.isOp()) {
                player.setOp(false);
            }
            player.removeMetadata("assistance", this);
            if (player.hasMetadata("superpowers")) {
                player.removeMetadata("superpowers", this);
                getServer().dispatchCommand(player, "transmission:staffchat I have lost my superpowers");
                perms.removeGroups(player, config.SUPER_PREFIX);
            }

            //Move back to previous location
            Location oldloc = (Location) player.getMetadata("location").get(0).value();
            player.teleport(oldloc);

            //Unvanish
            if (player.hasMetadata("vanished"))
                toggleVanish(player, false);

            ItemStack[] oldinv = (ItemStack[]) player.getMetadata("inventory").get(0).value();
            ItemStack[] oldarm = (ItemStack[]) player.getMetadata("armor").get(0).value();
            //restore previous data
            player.setExp((float) player.getMetadata("exp").get(0).value());
            player.setFoodLevel((int) player.getMetadata("food").get(0).value());
            player.setFallDistance((float) player.getMetadata("fallDist").get(0).value()); //Reset fall distance
            player.getInventory().clear();
            player.setNoDamageTicks(60);
            player.setGameMode(config.GAMEMODE);
            player.setFlying(player.getGameMode() == org.bukkit.GameMode.CREATIVE);
            player.setAllowFlight(player.getGameMode() == org.bukkit.GameMode.CREATIVE);
            player.setCanPickupItems(true);
            player.getInventory().setContents(oldinv);
            player.getInventory().setArmorContents(oldarm);
            player.setExp((float) player.getMetadata("experience").get(0).value());

            //Change groups
            perms.removeGroups(player, config.ASSIST_PREFIX);
            if (config.COLORNAMES && player.hasMetadata("displayName")) {
                player.setDisplayName((String) player.getMetadata("displayName").get(0).value());
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
            player.sendMessage(String.format("%sYou are no longer in assistance mode.", ChatColor.GOLD));
        } else { // Add metadata and enter assistance mode at the current location.
            logger.info(String.format("%s entering MODE at %s", player.getName(), player.getLocation().toString()));

            //enable logblock tool, if logblock is enabled
            if (getServer().getPluginManager().getPlugin("LogBlock") != null) {
                getServer().dispatchCommand(player, "lb toolblock on");
                getServer().dispatchCommand(player, "lb tool on");
            }
            player.sendMessage(config.MODE_MOTD);
            if (player.hasPermission("tier2.superpowers")) {
                player.sendMessage(String.format("%sSome permissions now require supermode", config.NAMECOLOR));
                player.sendMessage(String.format("%sTo enable supermode, do `supermode <IGN> <reason>` on the console", config.NAMECOLOR));
                player.sendMessage(String.format("%sThis includes use of sudo and worldedit", config.NAMECOLOR));
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
            player.setMetadata("experience", new FixedMetadataValue(this, player.getExp()));
            player.setAllowFlight(true);
            player.setCanPickupItems(false);


            //Remove armor
            player.getInventory().clear();
            player.getInventory().setHelmet(new ItemStack(org.bukkit.Material.GLASS));
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);

            //Change groups
            perms.addGroups(player, config.ASSIST_PREFIX);
            if (config.COLORNAMES) {
                player.setMetadata("displayName", new FixedMetadataValue(this, player.getDisplayName()));
                player.setDisplayName(config.NAMECOLOR + player.getName() + ChatColor.RESET);
            }
            //Add items in config.yml
            for (ItemStack item : config.ITEMS) { // Add items as per config.yml.
                player.getInventory().addItem(item);
            }

            //Swap Team
            if (board.getPlayerTeam(player) != null) {
                Team playerteam = board.getPlayerTeam(player);
                player.setMetadata("team", new FixedMetadataValue(this, playerteam));
            }
            assistanceTeam.addPlayer(player);

            //Let the player know they have entered assistance mode
            player.playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, null);
            player.sendMessage(String.format("%sYou are now in assistance mode.", ChatColor.GOLD));
        }
    }

    void msgStaff(String... message) {
        for (Player online : getServer().getOnlinePlayers()) {
            if (online.hasPermission("tier2.ticket")) {
                online.sendMessage(String.format("%s%s", ChatColor.GOLD, join(' ', message)));
            }
        }
    }

    void msgTicket(CommandSender player, Ticket ticket) {
        player.sendMessage(String.format("%s==Ticket #%s ==", ChatColor.GOLD, ticket.getId() ));
        if (ticket.getStatus() == TicketStatus.ELEVATED) {
            player.sendMessage(String.format("%sElevated To: %s", ChatColor.GOLD, ticket.getElevationGroup()));
        }
        player.sendMessage(String.format("%sOpened By: %s", ChatColor.GOLD, ticket.getPlayerName()));
        player.sendMessage(String.format("%sDescription: %s", ChatColor.GOLD, ticket.getTicket()));
        player.sendMessage(String.format("%sLocation: %s", ChatColor.GOLD, ticket.getTicketLocation()));
        player.sendMessage(String.format("%sStatus: %s", ChatColor.GOLD, ticket.getStatus().toString()));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            player.sendMessage(String.format("%sClosed By: %s", ChatColor.GOLD, ticket.getAssignedMod()));
            player.sendMessage(String.format("%sClose Message: %s", ChatColor.GOLD, ticket.getCloseMessage()));
        }
    }

    void msgTickets(CommandSender player, List<Ticket> tickets) {
        player.sendMessage(String.format("%s== Active Tickets (%s) ==", ChatColor.GOLD, tickets.size()));
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
                    || player.hasPermission("tier2.ticket")) {
                player.sendMessage(String.format("%s#%s by %s", ChatColor.DARK_AQUA, ticket.getId(), ticket.getPlayerName()));
                String messageBody = ticket.getTicket();
                if (ticket.getTicket().length() > 50) {
                    messageBody = ticket.getTicket().substring(0, 50) + "...";
                }
                player.sendMessage(String.format("%s%s%s", ChatColor.GOLD, (ticket.getStatus() == TicketStatus.ELEVATED ? String.format("%s[%s]%s", ChatColor.AQUA, ticket.getElevationGroup().toUpperCase(), ChatColor.GOLD) : ""), messageBody));
            }
        }
        if (elevatedTickets.size() > 0) {
            player.sendMessage(String.format("%s== Elevated Tickets (%s) ==", ChatColor.GOLD, elevatedTickets.size()));
            for (String group : elevatedTickets.keySet()) {
                player.sendMessage(String.format("%s[%s]%s", ChatColor.AQUA, group.toUpperCase(), elevatedTickets.get(group)));
            }
        }
    }

    /*
     * Join methods
     * join(sep, string[]), returns array contents in a single string seperated by sep
     * join(sep, start, string[]) - same as above, just with subarrays
     * join(sep, start, end, string[]) - same as previous two, just with start/end
     */

    public String join(Character seperator, int start, int end, String... text) {
        return join(seperator, (String[]) Arrays.asList(text).subList(start, end).toArray());
    }

    public String join(Character seperator, int start, String... text) {
        return join(seperator, (String[]) Arrays.asList(text).subList(start, text.length).toArray());
    }

    public String join(Character seperator, String... text) {
        StringBuilder sb = new StringBuilder();
        for (String t : text) {
            sb.append(t).append(seperator);
        }
        return sb.substring(0, sb.length() - 1);
    }
}
