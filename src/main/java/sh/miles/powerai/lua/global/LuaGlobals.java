package sh.miles.powerai.lua.global;

import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.lib.CoreLibraries;
import sh.miles.powerai.lua.LuaScript;
import sh.miles.powerai.lua.global.minecraft.ZLuaMinecraftFunctions;
import sh.miles.powerai.lua.global.system.ZLuaSystemFunctions;

import java.util.function.LongSupplier;

public final class LuaGlobals {

    private LuaGlobals() {
    }

    public static void std(LuaState state) {
        try {
            CoreLibraries.standardGlobals(state);
        } catch (LuaError e) {
            throw new RuntimeException(e);
        }
    }

    public static void system(LuaState state, LuaScript script, LongSupplier globalTicker) {
        ZLuaSystemFunctions.write(state, script, globalTicker);
    }

    public static void minecraft(LuaState state, Player player) {
        ZLuaMinecraftFunctions.write(state, player);
    }


}
