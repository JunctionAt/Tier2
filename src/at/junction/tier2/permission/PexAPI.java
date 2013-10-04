package at.junction.tier2.permission;

import at.junction.tier2.AbstractPermissionAPI;

import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PexAPI extends AbstractPermissionAPI {
    PermissionManager pex;
    
    public PexAPI() {
        pex = PermissionsEx.getPermissionManager();
    }
    
    public void addTier2Groups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== START addTier2Groups() FOR '" + player.getName() + "' ===");
        }

        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getGroups()) {
            if(group.getName().startsWith(prefix)) {
                continue;
            }
            
            user.addGroup(pex.getGroup(prefix + group.getName()));
            if (plugin.config.DEBUG) {
                plugin.logger.info("Adding '" + group + "' to '" + player.getName() + "'.");
            }
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END addTier2Groups() ===");
        }
    }

    @Override
    public void removeTier2Groups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== START removeTier2Groups() FOR '" + player.getName() + "' ===");
        }

        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getGroups()) {
            if(group.getName().startsWith(prefix)) {
                user.removeGroup(group);
                if (plugin.config.DEBUG) {
                    plugin.logger.info("Removing '" + group + "' from '" + player.getName() + "'.");
                }
            }
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END removeTier2Groups() ===");
        }
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        PermissionUser user = pex.getUser(player);
        
        return user.inGroup(group);
    }
}
