package at.junction.tier2.database;

import com.avaje.ebean.Query;
import at.junction.tier2.Tier2;
import at.junction.tier2.database.Ticket.TicketStatus;
import java.util.ArrayList;
import java.util.List;

public class TicketTable {

    Tier2 plugin;

    public TicketTable(Tier2 plugin) {
        this.plugin = plugin;
    }

    public List<Ticket> getUserTickets(String username) {
        List<Ticket> retVal = new ArrayList<>();

        Query<Ticket> query = plugin.getDatabase().find(Ticket.class).where().ieq("playerName", username).eq("status", TicketStatus.OPEN).query();

        if (query != null) {
            retVal.addAll(query.findList());
        }

        return retVal;
    }

    public List<Ticket> getMissedClosedTickets(String username) {
        List<Ticket> retVal = new ArrayList<>();

        Query<Ticket> query = plugin.getDatabase().find(Ticket.class).where().ieq("playerName", username).eq("status", TicketStatus.CLOSED).eq("closeSeenByUser", false).query();

        if (query != null) {
            retVal.addAll(query.findList());
        }

        return retVal;
    }

    public int getNumTicketFromUser(String username) {
        int retVal = 0;
        Query<Ticket> query = plugin.getDatabase().find(Ticket.class).where().ieq("playerName", username).in("status", TicketStatus.OPEN, TicketStatus.CLAIMED, TicketStatus.ELEVATED).query();

        if (query != null) {
            retVal = query.findRowCount();
        }

        return retVal;
    }

    public List<Ticket> getAllTickets() {
        List<Ticket> retVal = new ArrayList<>();
        Query<Ticket> query = plugin.getDatabase().find(Ticket.class).where().in("status", TicketStatus.OPEN, TicketStatus.CLAIMED, TicketStatus.ELEVATED).query();

        if(query != null) {
            retVal.addAll(query.findList());
        }
        return retVal;
    }

    public Ticket getTicket(int id) {
        Ticket retVal = null;

        Query<Ticket> query = plugin.getDatabase().find(Ticket.class).where().eq("id", id).query();

        if (query != null) {
            retVal = query.findUnique();
        }

        return retVal;
    }

    public void save(Ticket Ticket) {
        plugin.getDatabase().save(Ticket);
    }

}