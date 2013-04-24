package de.syntaxno.tier2.permission;

import de.syntaxno.tier2.AbstractPermissionAPI;

import org.bukkit.entity.Player;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;

public class BPermsAPI extends AbstractPermissionAPI {
    private String[] getGroups(Player player) {
        return ApiLayer.getGroups(
                player.getWorld().getName(), CalculableType.USER, player.getName());
    }
    
    private void addGroup(Player player, String group) {
        ApiLayer.addGroup(
                "*", CalculableType.USER, player.getName(), group);
        if (plugin.config.DEBUG) {
            plugin.logger.info("Adding '" + group + "' to '" + player.getName() + "'.");
        }
    }
    
    private void removeGroup(Player player, String group) {
        ApiLayer.removeGroup(
                "*", CalculableType.USER, player.getName(), group);
        if (plugin.config.DEBUG) {
            plugin.logger.info("Removing '" + group + "' from '" + player.getName() + "'.");
        }
    }
    
    public void addTier2Groups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== START addTier2Groups() FOR '" + player.getName() +"' ===");
        }

        for (String group : getGroups(player)) {
            if(group.startsWith(prefix)) {
                continue;
            }
            addGroup(player, prefix + group);
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END removeTier2Groups() ===");
        }
    }

    @Override
    public void removeTier2Groups(Player player, String prefix) {

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== START removeTier2Groups() FOR '" + player.getName() +"' ===");
        }

        for (String group : getGroups(player)) {
            if(group.startsWith(prefix)) {
                removeGroup(player, group);
                if (plugin.config.DEBUG) {
                    plugin.logger.info(" - Removing " + group);
                }
            }
        }

        if (plugin.config.DEBUG) {
            plugin.logger.info("=== END removeTier2Groups() ===");
        }
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        return ApiLayer.hasGroup(
                "*", CalculableType.USER,
                player.getName(), group);
    }
}