package sh.miles.powerai.lua.global.system;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import sh.miles.powerai.lua.LuaScript;

import java.util.function.LongSupplier;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Read_Ticks implements ZLuaSystemFunctions.SystemFunction {

    @Override
    public Varargs invoke(final LuaState state, final LuaScript script, final LongSupplier globalTicker, final Varargs args) throws LuaError {
        return valueOf(globalTicker.getAsLong());
    }
}
