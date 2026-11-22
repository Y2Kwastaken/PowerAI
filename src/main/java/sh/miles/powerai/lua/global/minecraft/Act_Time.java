package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Act_Time implements ZPlayerFunction {
    static final String TIME_PERMISSION = "powerai.lua.function.time";

    static final int DAY_LENGTH = 24000;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final World world = player.getWorld();
        if (!args.arg(1).isNil()) {
            world.setTime(Math.floorMod(args.arg(1).checkLong(), DAY_LENGTH));
        }
        return valueOf((int) world.getTime());
    }

    @Override
    public String permission() {
        return TIME_PERMISSION;
    }
}
