package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_Spawn implements ZPlayerFunction {
    static final String SPAWN_PERMISSION = "powerai.lua.function.spawn";
    static final String SPAWN_LOAD_PERMISSION = "powerai.lua.function.spawn.load";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final EntityType type = ZArgs.registry(RegistryKey.ENTITY_TYPE, args.arg(1));
        if (!type.isSpawnable()) {
            throw new LuaError("can't spawn " + type.key().asMinimalString());
        }

        final World world = player.getWorld();
        final Location location = ZArgs.location(world, args, 2);
        if (!ZArgs.isChunkLoaded(world, location.getX(), location.getZ()) && !player.hasPermission(SPAWN_LOAD_PERMISSION)) {
            return Constants.FALSE;
        }

        world.spawnEntity(location, type);
        return Constants.TRUE;
    }

    @Override
    public String permission() {
        return SPAWN_PERMISSION;
    }
}
