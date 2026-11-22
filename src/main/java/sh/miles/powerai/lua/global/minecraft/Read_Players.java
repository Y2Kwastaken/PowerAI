package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.Varargs;

import java.nio.charset.StandardCharsets;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Read_Players implements ZPlayerFunction {
    static final String PLAYERS_PERMISSION = "powerai.lua.function.players";
    static final double MAX_RADIUS = 256;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final double radius = Math.clamp(args.arg(1).optDouble(64), 1, MAX_RADIUS);
        final Location origin = player.getLocation();

        final LuaTable list = new LuaTable();
        int index = 1;
        for (Player other : origin.getNearbyPlayers(radius).stream()
                .sorted((a, b) -> Double.compare(a.getLocation().distanceSquared(origin), b.getLocation().distanceSquared(origin)))
                .toList()) {
            final Location at = other.getLocation();
            final LuaTable entry = new LuaTable();
            entry.rawset("name", valueOf(other.getName().getBytes(StandardCharsets.UTF_8)));
            entry.rawset("x", valueOf(at.getX()));
            entry.rawset("y", valueOf(at.getY()));
            entry.rawset("z", valueOf(at.getZ()));
            entry.rawset("self", valueOf(other.equals(player)));
            list.rawset(index++, entry);
        }
        return list;
    }

    @Override
    public String permission() {
        return PLAYERS_PERMISSION;
    }
}
