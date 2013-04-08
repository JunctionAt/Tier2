package de.syntaxno.tier2;

import org.bukkit.entity.Player;

public abstract class AbstractPermissionAPI {
    public static AbstractPermissionAPI getAPI(String name) {
        try {
            Class<AbstractPermissionAPI> cl =
                    (Class<AbstractPermissionAPI>)Class.forName(name);
            if (cl != null)
                return cl.newInstance();
            else
                return null;
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | NoClassDefFoundError e) {
            return null;
        }
    }
    
    public abstract void addTier2Groups(Player player, String prefix);
    public abstract void removeTier2Groups(Player player, String prefix);
    
    public abstract boolean isInGroup(Player player, String group);
}
