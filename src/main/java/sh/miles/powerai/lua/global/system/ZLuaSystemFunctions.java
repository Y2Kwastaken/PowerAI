package sh.miles.powerai.lua.global.system;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LibFunction;
import sh.miles.powerai.lua.LuaScript;

import java.util.function.LongSupplier;

public final class ZLuaSystemFunctions {

    public static void write(LuaState state, LuaScript script, LongSupplier globalTicker) {
        LuaTable globals = state.globals();

        set(globals, script, globalTicker, "sleep", new Sleep());
        set(globals, script, globalTicker, "ticks", new Read_Ticks());
    }

    private static void set(LuaTable globals, LuaScript script, LongSupplier globalTicker, String name, SystemFunction function) {
        globals.rawset(name, function.compile(script, globalTicker));
    }

    interface SystemFunction {

        Varargs invoke(LuaState state, LuaScript script, LongSupplier globalTicker, Varargs args) throws LuaError;

        default LibFunction compile(LuaScript script, LongSupplier globalTicker) {
            return LibFunction.createV((state, args) -> this.invoke(state, script, globalTicker, args));
        }
    }

}
