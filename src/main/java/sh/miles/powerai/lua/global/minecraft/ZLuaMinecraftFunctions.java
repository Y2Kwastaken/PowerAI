package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class ZLuaMinecraftFunctions {

    public static void write(LuaState state, Player player) {
        final LuaTable globals = state.globals();
    }

    private static void set(LuaTable globals, Player player, String name, ZPlayerFunction function) {
        globals.rawset(name, function.compile(player, name));
    }
}
