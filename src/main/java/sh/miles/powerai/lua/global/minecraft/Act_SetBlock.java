package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.World;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Act_SetBlock implements ZPlayerFunction {
    static final String SET_BLOCK_PERMISSION = "powerai.lua.function.setblock";
    static final String SET_BLOCK_LOAD_PERMISSION = "powerai.lua.function.setblock.load";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final int x = args.arg(1).checkInteger();
        final int y = args.arg(2).checkInteger();
        final int z = args.arg(3).checkInteger();
        final BlockType type = ZArgs.registry(RegistryKey.BLOCK, args.arg(4));

        final World world = player.getWorld();
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return Constants.FALSE;
        }
        if (!ZArgs.isChunkLoaded(world, x, z) && !player.hasPermission(SET_BLOCK_LOAD_PERMISSION)) {
            return Constants.FALSE;
        }

        world.setBlockData(x, y, z, type.createBlockData());
        return Constants.TRUE;
    }

    @Override
    public String permission() {
        return SET_BLOCK_PERMISSION;
    }
}
