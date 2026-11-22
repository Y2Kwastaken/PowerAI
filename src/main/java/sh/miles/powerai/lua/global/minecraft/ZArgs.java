package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ZArgs {

    static StringBuilder stringJoiner(Varargs args) throws LuaError {
        int argCount = args.count();
        StringBuilder joiner = new StringBuilder();
        for (int i = 1; i <= argCount; i++) {
            joiner.append(args.arg(i).checkString());
            if (i != argCount) {
                joiner.append(" ");
            }
        }

        return joiner;
    }

    static String utf8(LuaString s) {
        return StandardCharsets.UTF_8.decode(s.toBuffer()).toString();
    }

    static <T extends Keyed> T registry(RegistryKey<T> r, String key) throws LuaError {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        T value = namespacedKey == null ? null : RegistryAccess.registryAccess().getRegistry(r).get(namespacedKey);
        if (value == null) {
            throw new LuaError("unknown " + r.key().value() + " '" + key + "'");
        }
        return value;
    }

    static <T extends Keyed> T registry(RegistryKey<T> r, LuaValue key) throws LuaError {
        return registry(r, utf8(key.checkLuaString()));
    }

    static Location location(World world, Varargs args, int first) throws LuaError {
        double x = args.arg(first).checkDouble();
        double y = args.arg(first + 1).checkDouble();
        double z = args.arg(first + 2).checkDouble();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new LuaError("coordinates must be finite numbers");
        }
        return new Location(world, x, y, z);
    }

    static boolean isChunkLoaded(World world, double x, double z) {
        return world.isChunkLoaded(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
    }

}
