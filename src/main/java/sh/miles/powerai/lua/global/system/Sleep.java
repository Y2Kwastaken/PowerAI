package sh.miles.powerai.lua.global.system;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import sh.miles.powerai.lua.LuaScript;

import java.util.function.LongSupplier;

final class Sleep implements ZLuaSystemFunctions.SystemFunction {

    @Override
    public Varargs invoke(final LuaState state, final LuaScript script, final LongSupplier globalTicker, final Varargs args) throws LuaError {
        script.wakeTick = globalTicker.getAsLong() + Math.max(1, args.arg(1).optInteger(1));
        state.interrupt();
        return Constants.NONE;
    }
}
