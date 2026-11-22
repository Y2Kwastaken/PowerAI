package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import java.util.Locale;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Act_Weather implements ZPlayerFunction {
    static final String WEATHER_PERMISSION = "powerai.lua.function.weather";

    static final int DEFAULT_DURATION = 6000;
    static final int MAX_DURATION = 1_000_000;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final World world = player.getWorld();
        if (!args.arg(1).isNil()) {
            final String type = ZArgs.utf8(args.arg(1).checkLuaString()).toLowerCase(Locale.ROOT);
            final int duration = Math.clamp(args.arg(2).optInteger(DEFAULT_DURATION), 1, MAX_DURATION);
            switch (type) {
                case "clear" -> world.setClearWeatherDuration(duration);
                case "rain" -> {
                    world.setStorm(true);
                    world.setThundering(false);
                    world.setWeatherDuration(duration);
                }
                case "thunder" -> {
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(duration);
                    world.setThunderDuration(duration);
                }
                default -> throw new LuaError("unknown weather '" + type + "', expected clear, rain or thunder");
            }
        }

        return valueOf(world.isThundering() ? "thunder" : world.hasStorm() ? "rain" : "clear");
    }

    @Override
    public String permission() {
        return WEATHER_PERMISSION;
    }
}
