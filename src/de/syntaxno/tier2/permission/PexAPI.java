package de.syntaxno.tier2.permission;

import de.syntaxno.tier2.AbstractPermissionAPI;

import org.bukkit.entity.Player;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PexAPI extends AbstractPermissionAPI{
    PermissionManager pex;
    
    public PexAPI() {
        pex = PermissionsEx.getPermissionManager();
    }
    
    public void addTier2Groups(Player player, String prefix) {
        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getGroups()) {
            if(group.getName().startsWith(prefix)) continue;
            
            user.addGroup(pex.getGroup(prefix + group.getName()));
        }
    }

    @Override
    public void removeTier2Groups(Player player, String prefix) {
        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getGroups()) {
            if(group.getName().startsWith(prefix)) {
                user.removeGroup(group);
            }
        }
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        PermissionUser user = pex.getUser(player);
        
        return user.inGroup(group);
    }
}
