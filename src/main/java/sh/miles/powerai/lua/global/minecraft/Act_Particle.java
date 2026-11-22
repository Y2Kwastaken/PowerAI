package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Particle implements ZPlayerFunction {
    static final String PARTICLE_PERMISSION = "powerai.lua.function.particle";
    static final int MAX_COUNT = 1000;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final Particle particle = ZArgs.registry(RegistryKey.PARTICLE_TYPE, args.arg(1));
        final Location location = ZArgs.location(player.getWorld(), args, 2);
        final int count = Math.clamp(args.arg(5).optInteger(1), 1, MAX_COUNT);
        final double spread = Math.max(0, args.arg(6).optDouble(0));
        final double speed = Math.max(0, args.arg(7).optDouble(0));
        final Object data = ZParticleData.read(particle, args.arg(8), location);

        particle.builder()
                .location(location)
                .count(count)
                .offset(spread, spread, spread)
                .speed(speed)
                .data(data)
                .force(true)
                .spawn();
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return PARTICLE_PERMISSION;
    }
}
