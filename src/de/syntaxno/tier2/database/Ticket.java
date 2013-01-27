package de.syntaxno.tier2.database;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity()
@Table(name = "tickets")
public class Ticket {

    public enum TicketStatus {
        CLOSED, CLAIMED, ELEVATED, OPEN
    }

    @Id
    private int id;

    @NotNull
    private String playerName;
    private String assignedMod;

    @NotEmpty
    private String ticket;

    @NotNull
    private long ticketTime;

    @NotNull
    private TicketStatus status;

    @NotNull
    private String ticketLocation;
    private String closeMessage;
    private long closeTime;
    private boolean closeSeenByUser;

    private String elevationGroup;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public void setAssignedMod(String assignedMod) {
        this.assignedMod = assignedMod;
    }

    public String getAssignedMod() {
        return this.assignedMod;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return this.ticket;
    }

    public void setTicketTime(long ticketTime) {
        this.ticketTime = ticketTime;
    }

    public long getTicketTime() {
        return this.ticketTime;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketStatus getStatus() {
        return this.status;
    }

    public void setTicketLocation(String ticketLocation) {
        this.ticketLocation = ticketLocation;
    }

    public String getTicketLocation() {
        return this.ticketLocation;
    }

    public void setCloseMessage(String closeMessage) {
        this.closeMessage = closeMessage;
    }

    public String getCloseMessage() {
        return this.closeMessage;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getCloseTime() {
        return this.closeTime;
    }

    public void setCloseSeenByUser(boolean closeSeenByUser) {
        this.closeSeenByUser = closeSeenByUser;
    }

    public boolean isCloseSeenByUser() {
        return this.closeSeenByUser;
    }

    public void setElevationGroup(String elevationGroup) {
        this.elevationGroup = elevationGroup;
    }

    public String getElevationGroup() {
        return this.elevationGroup;
    }
}
