package de.syntaxno.tier2.permission;

import de.syntaxno.tier2.AbstractPermissionAPI;

import org.bukkit.entity.Player;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;

public class BPermsAPI extends AbstractPermissionAPI {
    private String[] getGroups(Player p) {
        return ApiLayer.getGroups(
                p.getWorld().getName(), CalculableType.USER, p.getName());
    }
    
    private void addGroup(Player p, String g) {
        ApiLayer.addGroup(
                p.getWorld().getName(), CalculableType.USER, p.getName(), g);
    }
    
    private void removeGroup(Player p, String g) {
        ApiLayer.removeGroup(
                p.getWorld().getName(), CalculableType.USER, p.getName(), g);
    }
    
    public void addTier2Groups(Player player, String prefix) {
        for (String group : getGroups(player)) {
            if(group.startsWith(prefix)) continue;
            
            addGroup(player, prefix + group);
        }
    }

    @Override
    public void removeTier2Groups(Player player, String prefix) {
        for (String group : getGroups(player)) {
            if(group.startsWith(prefix)) {
                removeGroup(player, group);
            }
        }
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        return ApiLayer.hasGroup(
                player.getWorld().getName(), CalculableType.USER,
                player.getName(), group);
    }
}
