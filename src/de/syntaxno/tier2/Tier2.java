package de.syntaxno.tier2;

import de.syntaxno.tier2.database.Ticket;
import de.syntaxno.tier2.database.Ticket.TicketStatus;
import de.syntaxno.tier2.database.TicketTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.persistence.PersistenceException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class Tier2 extends JavaPlugin {
    Tier2Listener listener;
    Configuration config;
    TicketTable ticketTable;

    AbstractPermissionAPI perms = null;
    
    public static final String[] apiList = {
        "de.syntaxno.tier2.permission.PexAPI",
        "de.syntaxno.tier2.permission.BPermsAPI"
    };

    @Override
    public void onEnable() {
        setupDatabase();

        ticketTable = new TicketTable(this);
        listener = new Tier2Listener(this);
        config = new Configuration(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        for (String name : apiList) {
            AbstractPermissionAPI api = AbstractPermissionAPI.getAPI(name);
            if (api != null) {
                perms = api;
                break;
            }
        }
        
        if (perms == null) {
            /* we need to do something clever here */
        }

        File cfile = new File(getDataFolder(), "config.yml");
		if(!cfile.exists()) {
			getConfig().options().copyDefaults(true);
			saveConfig();
		}
        config.load();
    }

    @Override
    public void onDisable() {
        for(Player online : getServer().getOnlinePlayers()) {
            if(online.hasMetadata("assistance")) {
                toggleMode(online);
            }
        }
    }

    public boolean setupDatabase() {
        try {
            getDatabase().find(Ticket.class).findRowCount();
        } catch(PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<>();
        list.add(Ticket.class);
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        Player player = (Player)sender;

        if(command.getName().equalsIgnoreCase("req")) { // Input a ticket.
            if(args.length == 0) {
                return false;
            }

            if(sender instanceof Player) {
                if(ticketTable.getNumTicketFromUser(player.getName()) < 5) {
                    String message = args[0];
                    for(int i = 1; i < args.length; i++) {
                        message += " " + args[i];
                    }

                    Ticket ticket = new Ticket();
                    ticket.setPlayerName(player.getName());
                    ticket.setTicket(message);
                    ticket.setTicketTime(System.currentTimeMillis());
                    String ticketLocation = String.format("%s,%f,%f,%f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
                    ticket.setTicketLocation(ticketLocation);
                    ticket.setStatus(TicketStatus.OPEN);
                    ticketTable.save(ticket);

                    msgStaff(player.getName() + " opened a new ticket.");
                    player.sendMessage(ChatColor.GOLD + "Ticket has been filed. Please be patient for staff to complete your request.");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You already have five open tickets. Please wait for these to be closed, or close some yourself.");
                    return true;
                }
            }
        }

        if(command.getName().equalsIgnoreCase("check")) { // Check for list, or check the details of a particular ticket.
            if(args.length > 0) { // If there's an ID to check for.
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                } catch(NumberFormatException | NullPointerException ex) { // If arg[0] wasn't an integer.
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                if(player.hasPermission("tier2.ticket") || player.getName().equals(ticket.getPlayerName())) { // Check for permission to view ticket.
                    msgTicket(player, ticket);
                    return true;
                } else { // If they don't have permission to view a specific ticket.
                    player.sendMessage(ChatColor.RED + "You do not have permission to view this ticket.");
                    return true;
                }
            } else { // List all tickets.
                List<Ticket> tickets = new ArrayList<>();

                if(player.hasPermission("tier2.ticket")) {
                    tickets.addAll(ticketTable.getAllTickets());
                } else {
                    tickets.addAll(ticketTable.getUserTickets(player.getName()));
                }
                msgTickets(player, tickets);
                return true;
            }
        }

        if(command.getName().equalsIgnoreCase("claim")) { // Notify other staff of the claim.
            if(args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.CLAIMED);
                    ticket.setAssignedMod(player.getName());
                    ticketTable.save(ticket);
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " claimed #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        }

        if(command.getName().equalsIgnoreCase("tpclaim")) {
            if(args.length > 0) {
                Ticket ticket;
                if(!player.hasMetadata("assistance")) {
                    toggleMode(player);
                }
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.CLAIMED);
                    ticket.setAssignedMod(player.getName());
                    ticketTable.save(ticket);

                    String world;
                    double x, y, z;
                    String[] split = ticket.getTicketLocation().split(",");
                    world = split[0];
                    x = Double.parseDouble(split[1]);
                    y = Double.parseDouble(split[2]);
                    z = Double.parseDouble(split[3]);

                    Location loc = new Location(getServer().getWorld(world), x, y, z);

                    player.teleport(loc);
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " claimed #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        }

        if(command.getName().equalsIgnoreCase("unclaim")) { // Notify other staff of the unclaim.
            if(args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setStatus(TicketStatus.OPEN);
                    ticket.setAssignedMod("");
                    ticketTable.save(ticket);
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                msgStaff(player.getName() + " is no longer handling #" + args[0] + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        }

        if(command.getName().equalsIgnoreCase("done")) { // Close a ticket with an optional message.
            if(args.length > 0) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                    ticket.setCloseTime(System.currentTimeMillis());
                    if(args.length > 1) { // If we have a message to attach.
                        String message = args[1]; // arg[0] is the ticket ID.
                        for(int i = 2; i < args.length; i++) {
                            message += " " + args[i];
                        }
                        ticket.setCloseMessage(message);
                    }
                    ticket.setAssignedMod(player.getName()); // Just in case they didn't claim it.
                    ticket.setStatus(TicketStatus.CLOSED);
                    ticketTable.save(ticket);
                    msgStaff(player.getName() + " closed #" + args[0] + ".");
                    if(getServer().getPlayer(ticket.getPlayerName()) != null) {
                    String message;
                    if("".equals(ticket.getCloseMessage())) {
                        message = "No close message.";
                	} else {
                		message = ticket.getCloseMessage();
                	}
                	getServer().getPlayer(ticket.getPlayerName()).sendMessage(ChatColor.GOLD + "Ticket " + ticket.getId() + "closed: " + message);
                }
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You did not specify a ticket ID!");
                return false;
            }
        }

        if(command.getName().equalsIgnoreCase("elevate")) { // Elevate above first-level moderators to a particular group (predefined enums in Ticket.java).
            if(args.length == 2) {
                Ticket ticket;
                try {
                    ticket = ticketTable.getTicket(Integer.parseInt(args[0]));
                } catch(NumberFormatException | NullPointerException ex) {
                    player.sendMessage(ChatColor.RED + "Invalid ticket ID!");
                    return false;
                }
                ticket.setStatus(TicketStatus.ELEVATED);
                if(config.GROUPS.contains(args[1].toLowerCase())) {
                    ticket.setElevationGroup(args[1].toLowerCase());
                    ticketTable.save(ticket);
                } else {
                    player.sendMessage(ChatColor.RED + "That is an invalid elevation group.");
                    String groups = "";
                    for(String group : config.GROUPS) {
                        groups += group + ", ";
                    }
                    player.sendMessage(ChatColor.RED + "Available groups: " + groups.substring(0, groups.length() - 2));
                    return false;
                }
                player.sendMessage(ChatColor.GOLD + "Elevating #" + args[0] + " to " + args[1].toUpperCase() + ".");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Invalid parameters!");
                return false;
            }
        }

        if(command.getName().equalsIgnoreCase("staff")) { // Get a list of staff.
            String stafflist = "";
            for (Player online : getServer().getOnlinePlayers()) {
                if(online.hasPermission("tier2.ticket") && online.hasMetadata("assistance") && !online.hasMetadata("vanished")) {
                    stafflist += online.getName() + ", ";
                }
            }

            if(stafflist.equals("")) {
                player.sendMessage(ChatColor.GOLD + "No staff are currently online. :(");
                player.sendMessage(ChatColor.GOLD + "You can still make a request with \"/request <your request here>\", though!");
                player.sendMessage(ChatColor.GOLD + "One of the server staff will be with you as soon as possible.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Online Staff:");
                sender.sendMessage(ChatColor.GOLD + stafflist.substring(0, stafflist.length() - 2));
            }
            return true;
        }

        if(command.getName().equalsIgnoreCase("mode")) {
            toggleMode(player);
            return true;
        }

        if(command.getName().equalsIgnoreCase("vanish")) {
            toggleVanish(player, true);
            return true;
        }

        if(command.getName().equalsIgnoreCase("unvanish")) {
            toggleVanish(player, false);
            return true;
        }

        return true;
    }

    public void toggleVanish(Player player, boolean vanish) {
        if(vanish) {
            if(!player.hasMetadata("vanished")) {
                for(Player online : getServer().getOnlinePlayers()) {
                    if(!online.hasPermission("tier2.vanish.see")) {
                        online.hidePlayer(player);
                    }
                }
                player.setMetadata("vanished", new FixedMetadataValue(this, true));
                player.sendMessage(ChatColor.GOLD + "You are now vanished.");
            } else {
                player.sendMessage(ChatColor.GOLD + "You are already vanished!");
            }
        } else {
            if(player.hasMetadata("vanished")) {
                for(Player online : getServer().getOnlinePlayers()) {
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
        if(player.hasMetadata("assistance")) { // Remove metadata and restore to old "player".
            player.removeMetadata("assistance", this);
            ItemStack[] oldinv = (ItemStack[])player.getMetadata("inventory").get(0).value();
            Location oldloc = (Location)player.getMetadata("location").get(0).value();
            player.setExp((float)player.getMetadata("exp").get(0).value());
            player.setFoodLevel((int)player.getMetadata("food").get(0).value());
            player.getInventory().clear();
            player.setNoDamageTicks(60);
            player.teleport(oldloc);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setCanPickupItems(true);
            toggleVanish(player, false);
            for(ItemStack item : oldinv) {
                if(item != null) {
                    player.getInventory().addItem(item);
                }
            }
            perms.removeTier2Groups(player, config.GROUPPREFIX);
            if(config.COLORNAMES) {
                player.setDisplayName(player.getDisplayName().substring(2, player.getDisplayName().length() - 2));
            }
            player.sendMessage(ChatColor.GOLD + "You are no longer in assistance mode.");
        } else { // Add metadata and enter assistance mode at the current location.
            player.saveData();
            player.setMetadata("assistance", new FixedMetadataValue(this, true));
            Location playerloc = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + 0.5, player.getLocation().getZ()); // An attempted block-stuck fix.
            ItemStack[] playerinv = player.getInventory().getContents();
            player.setMetadata("location", new FixedMetadataValue(this, playerloc));
            player.setMetadata("inventory", new FixedMetadataValue(this, playerinv));
            player.setMetadata("exp", new FixedMetadataValue(this, player.getExp()));
            player.setMetadata("food", new FixedMetadataValue(this, player.getFoodLevel()));
            player.setAllowFlight(true);
            player.setCanPickupItems(false);
            player.getInventory().clear();
            perms.addTier2Groups(player, config.GROUPPREFIX);
            if(config.COLORNAMES) {
                player.setDisplayName(ChatColor.valueOf(config.NAMECOLOR) + player.getName() + ChatColor.RESET);
            }
            for(int i : config.ITEMS.keySet()) { // Add items as per config.yml.
                ItemStack itemstack = new ItemStack(i, config.ITEMS.get(i));
                player.getInventory().addItem(itemstack);
            }
            player.sendMessage(ChatColor.GOLD + "You are now in assistance mode.");
        }
    }

    public void msgStaff(String message) {
        for(Player online : getServer().getOnlinePlayers()) {
            if(online.hasPermission("tier2.ticket")) {
                online.sendMessage(ChatColor.GOLD + message);
            }
        }
    }

    public void msgTicket(Player player, Ticket ticket) {
        player.sendMessage(ChatColor.GOLD + "== Ticket #" + ticket.getId() + " ==");
        if(ticket.getStatus() == TicketStatus.ELEVATED) {
            player.sendMessage(ChatColor.GOLD + "Elevated To: " + ticket.getElevationGroup().toString());
        }
        player.sendMessage(ChatColor.GOLD + "Opened By: " + ticket.getPlayerName());
        player.sendMessage(ChatColor.GOLD + "Description: " + ticket.getTicket());
        player.sendMessage(ChatColor.GOLD + "Status: " + ticket.getStatus().toString());
        if(ticket.getStatus() == TicketStatus.CLOSED) {
            player.sendMessage(ChatColor.GOLD + "Closed By: " + ticket.getAssignedMod());
            player.sendMessage(ChatColor.GOLD + "Close Message: " + ticket.getCloseMessage());
        }
    }

    public void msgTickets(Player player, List<Ticket> tickets) {
        player.sendMessage(ChatColor.GOLD + "== Active Tickets (" + tickets.size() + ") ==");
        for(Ticket ticket : tickets) {
            // Check that it's either unelevated or they have the appropriate permissions.
            if(ticket.getStatus() != TicketStatus.ELEVATED
                    || perms.isInGroup(player, ticket.getElevationGroup())
                    || perms.isInGroup(player, config.GROUPPREFIX + ticket.getElevationGroup()))
            {
                player.sendMessage(ChatColor.DARK_AQUA + "#" + ticket.getId() + " by " + ticket.getPlayerName() + ":");
                String messageBody = ticket.getTicket();
                if(ticket.getTicket().length() > 25) {
                    messageBody = ticket.getTicket().substring(0, 26) + "...";
                }
                player.sendMessage(ChatColor.GOLD + ((ticket.getStatus() == TicketStatus.ELEVATED)?(ChatColor.AQUA + "[" + ticket.getElevationGroup().toUpperCase() + "] " + ChatColor.GOLD) : "") + messageBody);
            }
        }
    }
}
