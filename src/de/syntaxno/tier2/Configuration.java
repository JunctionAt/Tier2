package de.syntaxno.tier2;

import java.util.HashMap;
import java.util.List;

public class Configuration {
    private Tier2 plugin;

    public boolean COLORNAMES;
    public String NAMECOLOR;
    public String GROUPPREFIX;
    public List<String> GROUPS;
    public HashMap<Integer, Integer> ITEMS = new HashMap<>();

    public Configuration(Tier2 instance) {
        plugin = instance;
    }

    public void load() {
        plugin.reloadConfig();

        COLORNAMES = plugin.getConfig().getBoolean("color-names");
        NAMECOLOR = plugin.getConfig().getString("name-color");
        GROUPPREFIX = plugin.getConfig().getString("group-prefix");
        GROUPS = plugin.getConfig().getStringList("groups");
        for(String item : plugin.getConfig().getStringList("items")) {
            ITEMS.put(Integer.parseInt(item.split("x")[1]), Integer.parseInt(item.split("x")[0]));
        }
    }
}