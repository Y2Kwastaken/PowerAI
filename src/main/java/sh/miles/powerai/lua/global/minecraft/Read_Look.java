package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

final class Read_Look implements ZPlayerFunction {
    static final String LOOK_PERMISSION = "powerai.lua.function.look";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        int distance = Math.clamp(args.arg(1).optInteger(7), 1, 128);
        Block block = player.getTargetBlockExact(distance);
        if (block == null) {
            return Constants.NIL;
        }

        String blockType = block.getType().key().asMinimalString();
        return varargsOf(valueOf(block.getX()),
                valueOf(block.getY()),
                valueOf(block.getZ()),
                valueOf(blockType)
        );
    }

    @Override
    public String permission() {
        return LOOK_PERMISSION;
    }
}
