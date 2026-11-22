package sh.miles.powerai.lua;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaClosure;

import java.io.ByteArrayInputStream;
import java.util.UUID;

public final class LuaScript {

    public final LuaState state;
    public final String name;
    public final UUID owner;
    private final LuaClosure script;

    public long startTime;
    public long timeAllowed;
    public long totalRuntime;
    public boolean aborted;
    public long suspendRequestedAt; // 0 no suspend requested yet this slice
    public long wakeTick;
    public volatile boolean cancelled;
    private boolean started;

    /**
     * @throws CompileException if the script has a syntax error
     */
    public LuaScript(final LuaState globalState, final String name, final UUID owner, final byte[] script) throws CompileException, LuaError {
        this.state = globalState;
        this.name = name;
        this.owner = owner;
        this.script = LoadState.load(this.state, new ByteArrayInputStream(script), name, state.globals());
    }

    public boolean step() throws LuaError {
        var main = state.getMainThread();
        if (!started) {
            started = true;
            return LuaThread.runMain(state, script) != null;
        }
        if (!main.isAlive()) return true;
        return LuaThread.run(state.getCurrentThread(), Constants.NONE) != null;
    }


    public void interrupt() {
        this.state.interrupt();
    }

}
