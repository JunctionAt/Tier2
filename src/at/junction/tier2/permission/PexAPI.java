package at.junction.tier2.permission;

import at.junction.tier2.AbstractPermissionAPI;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PexAPI extends AbstractPermissionAPI {
    private final PermissionManager pex;

    public PexAPI() {
        pex = PermissionsEx.getPermissionManager();
    }
    @Override
    public void addGroups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info(String.format("=== START addTier2Groups(%s) FOR '%s' ===", prefix, player.getName()));
        }

        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getOwnParents()) {
            if(group.getName().contains("_")) {
                continue;
            }

            user.addGroup(pex.getGroup(String.format("%s%s", prefix, group.getName())));
            if (plugin.config.DEBUG) {
                plugin.logger.info(String.format("Adding '%s%s' to '%s'.", prefix, group.getName(), player.getName()));
            }
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END addTier2Groups() ===");
        }
    }

    @Override
    public void removeGroups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info(String.format("=== START removeAssistanceGroups(%s) FOR '%s' ===", prefix, player.getName()));
        }

        PermissionUser user = pex.getUser(player);

        for (PermissionGroup group : user.getOwnParents()) {
            if(group.getName().startsWith(prefix)) {
                user.removeGroup(group);
                if (plugin.config.DEBUG) {
                    plugin.logger.info("Removing '" + group + "' from '" + player.getName() + "'.");
                }
            }
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END removeAssistanceGroups() ===");
        }
    }

    @Override
    public boolean isInGroup(CommandSender sender, String group) {
        if (sender.getName().equals("CONSOLE"))
            return true;
        if (sender instanceof Player){
            PermissionUser user = pex.getUser((Player)sender);

            return user.inGroup(group);
        }
        return false;
    }
}
