package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Teleport implements ZPlayerFunction {
    static final String TELEPORT_PERMISSION = "powerai.lua.function.teleport";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final World world = player.getWorld();
        final Location target = ZArgs.location(world, args, 1);
        final Location current = player.getLocation();
        target.setYaw((float) args.arg(4).optDouble(current.getYaw()));
        target.setPitch((float) Math.clamp(args.arg(5).optDouble(current.getPitch()), -90.0, 90.0));

        if (!world.getWorldBorder().isInside(target)) {
            return Constants.FALSE;
        }

        player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        return Constants.TRUE;
    }

    @Override
    public String permission() {
        return TELEPORT_PERMISSION;
    }
}
