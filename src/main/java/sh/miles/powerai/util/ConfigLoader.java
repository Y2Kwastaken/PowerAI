package sh.miles.powerai.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static void saveConfig(Plugin plugin, String config) {
        plugin.saveResource(config, false);
    }

    public static FileConfiguration loadConfig(Plugin plugin, String config) {
        Path path = plugin.getDataPath().resolve(config);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
