package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Lightning implements ZPlayerFunction {
    static final String LIGHTNING_PERMISSION = "powerai.lua.function.lightning";
    static final String LIGHTNING_LOAD_PERMISSION = "powerai.lua.function.lightning.load";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final World world = player.getWorld();
        final Location location = ZArgs.location(world, args, 1);
        final boolean effectOnly = args.arg(4).optBoolean(false);

        if (!ZArgs.isChunkLoaded(world, location.getX(), location.getZ()) && !player.hasPermission(LIGHTNING_LOAD_PERMISSION)) {
            return Constants.FALSE;
        }

        if (effectOnly) {
            world.strikeLightningEffect(location);
        } else {
            world.strikeLightning(location);
        }
        return Constants.TRUE;
    }

    @Override
    public String permission() {
        return LIGHTNING_PERMISSION;
    }
}
