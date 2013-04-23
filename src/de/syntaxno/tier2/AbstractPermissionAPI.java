package de.syntaxno.tier2;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
    
    public abstract void addTier2Groups(Player player, String prefix);
    public abstract void removeTier2Groups(Player player, String prefix);
    
    public abstract boolean isInGroup(Player player, String group);
}
