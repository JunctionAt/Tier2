package at.junction.tier2;

import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class LogblockListener implements Listener {
    private final Tier2 plugin;

    public LogblockListener(Tier2 plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogBlockPreLogEvent(BlockChangePreLogEvent event) {
        Player player = plugin.getServer().getPlayer(event.getOwner());
        if (player != null && player.hasMetadata("assistance")){
            event.setOwner("assistance_" + player.getDisplayName());
        }
    }
}
