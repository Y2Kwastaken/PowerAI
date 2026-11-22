package sh.miles.powerai.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import sh.miles.powerai.util.ConfigLoader;

public record LuaConfig(long runtimePerTickMillis, long scriptAllowancePerTickMillis, long totalScriptAllowanceMillis) {

    private static LuaConfig load0(Plugin plugin) {
        ConfigLoader.saveConfig(plugin, "config.yml");
        FileConfiguration config = ConfigLoader.loadConfig(plugin, "config.yml");

        long runtimePerTick = config.getLong("runtime_per_tick");
        if (runtimePerTick < 0 || runtimePerTick > 50) {
            throw new IllegalStateException("can not set per tick runtime to longer than a tick or less than 0");
        }

        long scriptAllowancePerTick = config.getLong("script_allowance_per_tick");
        if (scriptAllowancePerTick < 0 || scriptAllowancePerTick > runtimePerTick) {
            throw new IllegalStateException("can not set per tick script runtime to longer than runtime per tick (%d)".formatted(runtimePerTick));
        }

        long totalScriptAllowance = config.getLong("total_script_allowance");
        if (totalScriptAllowance <= 0) {
            throw new IllegalStateException("total script allowance must be atleast 1ms");
        }

        return new LuaConfig(
                runtimePerTick,
                scriptAllowancePerTick,
                totalScriptAllowance
        );
    }

    public static LuaConfig load(Plugin plugin) {
        try {
            return load0(plugin);
        } catch (IllegalStateException e) {
            plugin.getSLF4JLogger().error("failed to load LuaConfig", e);
            plugin.getServer().shutdown();
            return null;
        }
    }
}
