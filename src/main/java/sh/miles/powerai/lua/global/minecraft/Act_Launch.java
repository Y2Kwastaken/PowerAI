package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Launch implements ZPlayerFunction {
    static final String LAUNCH_PERMISSION = "powerai.lua.function.launch";

    static final double MAX_VELOCITY = 10.0;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        Vector velocity = new Vector(component(args, 1), component(args, 2), component(args, 3));
        if (args.arg(4).optBoolean(false)) {
            velocity.add(player.getVelocity());
        }

        player.setVelocity(new Vector(
                Math.clamp(velocity.getX(), -MAX_VELOCITY, MAX_VELOCITY),
                Math.clamp(velocity.getY(), -MAX_VELOCITY, MAX_VELOCITY),
                Math.clamp(velocity.getZ(), -MAX_VELOCITY, MAX_VELOCITY)
        ));
        return Constants.NONE;
    }

    private static double component(final Varargs args, final int index) throws LuaError {
        double value = args.arg(index).checkDouble();
        if (!Double.isFinite(value)) {
            throw new LuaError("velocity must be finite numbers");
        }
        return value;
    }

    @Override
    public String permission() {
        return LAUNCH_PERMISSION;
    }
}
