package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

final class Read_Position implements ZPlayerFunction{

    static final String POSITION_PERMISSION = "powerai.lua.function.position";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final Location location = player.getLocation();
        return varargsOf(
                valueOf(location.getX()),
                valueOf(location.getY()),
                valueOf(location.getZ()),
                valueOf(location.getYaw()),
                valueOf(location.getPitch()),
                valueOf(location.getWorld().key().asMinimalString())
        );
    }

    @Override
    public String permission() {
        return POSITION_PERMISSION;
    }
}
