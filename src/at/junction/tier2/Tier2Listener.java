package at.junction.tier2;

import at.junction.tier2.database.Ticket;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;


//import de.diddiz.LogBlock.events.BlockChangePreLogEvent;


public class Tier2Listener implements Listener {
    private final Tier2 plugin;

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

    @EventHandler // Removes player from assistance mode on exit
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
                        damager.sendMessage(ChatColor.RED + "This moderator is in assistance mode.");
                        damager.sendMessage(ChatColor.RED + "Assistance mode is only to be used for dealing with official server business.");
                        damager.sendMessage(ChatColor.RED + "If you feel that this moderator is abusing assistance mode, feel free to contact another member of our moderation team.");
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
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){ // Removes colors from chat events. 
        if (plugin.getServer().getPluginManager().getPlugin("Transmission") != null) return;
        if (!plugin.config.COLORNAMES) return;
        if (event.getPlayer().hasMetadata("assistance")){
            //Cancel event
            event.setCancelled(true);
            
            //Reformat message and resend
            plugin.getServer().broadcastMessage("<" + event.getPlayer().getName() + "> " + event.getMessage());
            
        }
    }
    @EventHandler
    public void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent e){

        if (e.getPlayer().hasMetadata("assistance")){
            if (e.getPlayer().hasMetadata("changing-mode")){
                e.getPlayer().removeMetadata("changing-mode", plugin);
            } else {
                e.getPlayer().setMetadata("changing-mode", new FixedMetadataValue(plugin, "modeChange"));
                e.setCancelled(true);
                e.getPlayer().setGameMode(e.getNewGameMode());
                e.getPlayer().setAllowFlight(true);
            }
        }
    }

    //Warn players with mode access if someone gets kicked for flying
    @EventHandler
    public void onPlayerKickEvent(PlayerKickEvent e){
        if (e.getReason().contains("Flying is not enabled on this server")){
            for (Player p : plugin.getServer().getOnlinePlayers()){
                if (p.hasPermission("tier2.mode")){
                    String message = ChatColor.GREEN + "[TIER2] " + e.getPlayer().getName() + " was kicked for flying";
                    p.sendMessage(message);
                    plugin.getLogger().info(message);
                }
            }
        }
    }

    //Prevent players in assistance mode from removing their helmet
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        HumanEntity player = event.getWhoClicked();
        if (event.getWhoClicked().hasMetadata("assistance")) {
                //5 is the 'head' slot, not sure of a better way to get this. 
                if (event.getSlot() == 5) {
                    event.setCancelled(true);
                }
            }
        }
    }
//	@EventHandler
//	public void onLogBlockPreLogEvent(BlockChangePreLogEvent event) {
//		Player player = plugin.getServer().getPlayerExact(event.getOwner());
//		if (player != null && player.hasMetadata("assistance")){
//			event.setOwner("M_" + player.getDisplayName());
//		} else if (player != null) {
//			event.setOwner("P_" + player.getDisplayName());
//		}
//	}
}
