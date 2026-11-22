package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;

public class Read_Block implements ZPlayerFunction {

    static final String BLOCK_PERMISSION = "powerai.lua.function.block";
    static final String BLOCK_LOAD_PERMISSION = "powerai.lua.function.block.load";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final int x = args.arg(1).checkInteger();
        final int y = args.arg(2).checkInteger();
        final int z = args.arg(3).checkInteger();

        final World world = player.getWorld();
        final Block block = world.getBlockAt(x, y, z);
        if (!block.getChunk().isLoaded() && !player.hasPermission(BLOCK_LOAD_PERMISSION)) {
            return Constants.NIL;
        }

        return valueOf(block.getType().key().asMinimalString());
    }

    @Override
    public String permission() {
        return BLOCK_PERMISSION;
    }
}
