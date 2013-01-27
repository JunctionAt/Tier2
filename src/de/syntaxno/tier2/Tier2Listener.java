package de.syntaxno.tier2;

import de.syntaxno.tier2.database.Ticket;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Tier2Listener implements Listener {
    private Tier2 plugin;

    public Tier2Listener (Tier2 instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(!event.getPlayer().hasPermission("tier2.vanish.see")) { // Only check if they don't have the ability to see vanished players.
            for(Player online : plugin.getServer().getOnlinePlayers()) {
                if(online.hasMetadata("vanished")) {
                    event.getPlayer().hidePlayer(online);
                }
            }
        }

        if(event.getPlayer().hasPermission("tier2.ticket")) { // Check upon login.
            List<Ticket> tickets = plugin.ticketTable.getAllTickets();
            if(tickets.size() > 0) {
                event.getPlayer().sendMessage(ChatColor.GOLD + "There are open tickets. Type /check to see.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(player.hasMetadata("assistance")) {
            plugin.toggleMode(player);
        }
    }

    @EventHandler // Prevent mobs targetting players in assistance mode.
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTarget();
        if (player.hasMetadata("assistance")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) { // Prevents players in assistance mode being damaged.
        if(event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if(player.hasMetadata("assistance")) {
                event.setCancelled(true);
                if(event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent)event;
                    if(ee.getDamager() instanceof Player) {
                        Player damager = (Player)ee.getDamager();
                        damager.sendMessage(ChatColor.RED + "This staff member is in assistance mode.");
                        damager.sendMessage(ChatColor.RED + "Assistance mode is only to be used for player assistance and aid.");
                        damager.sendMessage(ChatColor.RED + "Please notify a member of the arbitration committee if you feel a staff member is abusing assistance mode.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if(player.hasMetadata("assistance")) {
                if (player.getFoodLevel() != 20) {
                    player.setFoodLevel(20);
                }
                event.setCancelled(true);
            }
        }
    }
}