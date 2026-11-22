package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.World;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Act_Fill implements ZPlayerFunction {
    static final String FILL_PERMISSION = "powerai.lua.function.fill";
    static final String FILL_LOAD_PERMISSION = "powerai.lua.function.fill.load";

    static final int MAX_VOLUME = 32768;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final int x1 = args.arg(1).checkInteger(), y1 = args.arg(2).checkInteger(), z1 = args.arg(3).checkInteger();
        final int x2 = args.arg(4).checkInteger(), y2 = args.arg(5).checkInteger(), z2 = args.arg(6).checkInteger();
        final BlockType type = ZArgs.registry(RegistryKey.BLOCK, args.arg(7));

        final World world = player.getWorld();
        final int minX = Math.min(x1, x2);
        final int maxX = Math.max(x1, x2);
        final int minZ = Math.min(z1, z2);
        final int maxZ = Math.max(z1, z2);
        final int minY = Math.max(Math.min(y1, y2), world.getMinHeight());
        final int maxY = Math.min(Math.max(y1, y2), world.getMaxHeight() - 1);
        if (minY > maxY) {
            return valueOf(0);
        }

        final long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_VOLUME) {
            throw new LuaError("fill too big: " + volume + " blocks, max " + MAX_VOLUME);
        }

        if (!player.hasPermission(FILL_LOAD_PERMISSION)) {
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                    if (!world.isChunkLoaded(cx, cz)) return valueOf(0);
                }
            }
        }

        final BlockData data = type.createBlockData();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // we skip out on physics for fast fills
                    // if only this were SUPER DUPE ASYNCHRONOUS IT WOUJLD GO ZOOOOOOM so quickly
                    world.getBlockAt(x, y, z).setBlockData(data, false);
                }
            }
        }
        return valueOf((int) volume);
    }

    @Override
    public String permission() {
        return FILL_PERMISSION;
    }
}
