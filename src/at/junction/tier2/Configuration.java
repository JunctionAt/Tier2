package at.junction.tier2;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;

public class Configuration {
    private final Tier2 plugin;

    public boolean COLORNAMES;
    public ChatColor NAMECOLOR;
    public String GROUPPREFIX;
    public List<String> GROUPS;
    public final HashMap<String, Integer> ITEMS = new HashMap<>();
    public boolean DEBUG = false;
    public GameMode GAMEMODE;
    public String SUPERMODE_GROUP;
    public String MODE_MOTD;

    public Configuration(Tier2 instance) {
        plugin = instance;
    }

    public void load() {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        COLORNAMES = config.getBoolean("color-names");
        NAMECOLOR = ChatColor.valueOf(config.getString("name-color"));
        GROUPPREFIX = config.getString("group-prefix");
        GROUPS = config.getStringList("groups");
        for(String item : config.getStringList("items")) {
            ITEMS.put(item.split("x")[1], Integer.parseInt(item.split("x")[0]));
        }
        DEBUG = config.getBoolean("debug");
        GAMEMODE = GameMode.valueOf(config.getString("gameMode", "SURVIVAL"));
        SUPERMODE_GROUP = config.getString("supermodeGroup", "superpowers");
        MODE_MOTD = NAMECOLOR + config.getString("modeMOTD", "Welcome to Assistance Mode!");
    }
    public void save(){

    }
}
