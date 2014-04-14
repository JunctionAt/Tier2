package at.junction.tier2;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;

public class Configuration {
    private final Tier2 plugin;

    public boolean COLORNAMES;
    public ChatColor NAMECOLOR;
    public String ASSIST_PREFIX;
    public String SUPER_PREFIX;
    public List<String> ELEVATION_GROUPS;
    public final HashSet<ItemStack> ITEMS = new HashSet<>();
    public boolean DEBUG = false;
    public GameMode GAMEMODE;
    public String MODE_MOTD;

    public Configuration(Tier2 instance) {
        plugin = instance;
    }

    public void load() {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        COLORNAMES = config.getBoolean("color-names");
        NAMECOLOR = ChatColor.valueOf(config.getString("name-color"));

        ASSIST_PREFIX = config.getString("assist-prefix", "assist_");
        SUPER_PREFIX = config.getString("super-prefix", "super_");

        ELEVATION_GROUPS = config.getStringList("elevation-groups");

        for(String item : config.getStringList("items")) {
            String[] temp = item.split("x");
            ITEMS.add(new ItemStack(Material.valueOf(temp[1]), Integer.parseInt(temp[0])));
        }

        GAMEMODE = GameMode.valueOf(config.getString("gameMode", "SURVIVAL"));
        MODE_MOTD = NAMECOLOR + config.getString("modeMOTD", "Welcome to Assistance Mode!");

        DEBUG = config.getBoolean("debug");

    }
    public void save(){

    }
}
