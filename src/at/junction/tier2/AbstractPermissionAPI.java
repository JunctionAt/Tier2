package at.junction.tier2;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class AbstractPermissionAPI {

    protected Tier2 plugin;

    public static AbstractPermissionAPI getAPI(Tier2 plugin, String name) {
        try {
            Class<AbstractPermissionAPI> cl =
                    (Class<AbstractPermissionAPI>)Class.forName(name);
            if (cl != null) {
                AbstractPermissionAPI permissionAPI = cl.newInstance();
                permissionAPI.plugin = plugin;
                return permissionAPI;
            }
            else {
                return null;
            }
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | NoClassDefFoundError e) {
            return null;
        }
    }
    
    public abstract void addGroups(Player player, String prefix);
    public abstract void removeGroups(Player player, String prefix);

    public abstract boolean isInGroup(CommandSender player, String group);
}
