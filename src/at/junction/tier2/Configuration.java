package at.junction.tier2;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;

public class Configuration {
    private Tier2 plugin;

    public boolean COLORNAMES;
    public String NAMECOLOR;
    public String GROUPPREFIX;
    public List<String> GROUPS;
    public HashMap<Integer, Integer> ITEMS = new HashMap<>();
    public boolean DEBUG = false;

    public Configuration(Tier2 instance) {
        plugin = instance;
    }

    public void load() {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        COLORNAMES = config.getBoolean("color-names");
        NAMECOLOR = config.getString("name-color");
        GROUPPREFIX = config.getString("group-prefix");
        GROUPS = config.getStringList("groups");
        for(String item : config.getStringList("items")) {
            ITEMS.put(Integer.parseInt(item.split("x")[1]), Integer.parseInt(item.split("x")[0]));
        }
        DEBUG = config.getBoolean("debug");
    }
}
