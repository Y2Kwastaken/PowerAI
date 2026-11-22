package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Explode implements ZPlayerFunction {
    static final String EXPLODE_PERMISSION = "powerai.lua.function.explode";
    static final String EXPLODE_LOAD_PERMISSION = "powerai.lua.function.explode.load";

    static final float MAX_POWER = 128f;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final World world = player.getWorld();
        final Location location = ZArgs.location(world, args, 1);
        final float power = (float) Math.clamp(args.arg(4).optDouble(4.0), 0.0, MAX_POWER);
        final boolean fire = args.arg(5).optBoolean(false);
        final boolean breakBlocks = args.arg(6).optBoolean(true);

        if (!ZArgs.isChunkLoaded(world, location.getX(), location.getZ()) && !player.hasPermission(EXPLODE_LOAD_PERMISSION)) {
            return Constants.FALSE;
        }

        world.createExplosion(location, power, fire, breakBlocks);
        return Constants.TRUE;
    }

    @Override
    public String permission() {
        return EXPLODE_PERMISSION;
    }
}
