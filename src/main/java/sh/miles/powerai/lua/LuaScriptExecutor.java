package sh.miles.powerai.lua;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.interrupt.InterruptHandler;
import sh.miles.powerai.lua.global.LuaGlobals;
import sh.miles.powerai.lua.global.system.ZLuaSystemFunctions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LuaScriptExecutor implements Runnable {
    private static final Duration STOP_GRACE = Duration.ofMillis(100);

    private final Queue<LuaScript> scripts = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("powerai-lua-watchdog").factory());
    private final Logger logger;
    private final long runtimePerTickMillis;
    private final long scriptAllowancePerTickMillis;
    private final long totalScriptAllowanceNanos;

    private final Consumer<LuaState> globalWriter;
    private final LuaState.Builder globalBuilder;

    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ReentrantLock runLock = new ReentrantLock();

    private volatile LuaScript current;
    private long tick = 0;

    public LuaScriptExecutor(final Logger logger, final long runtimePerTickMillis, final long scriptAllowancePerTickMillis, final long totalScriptAllowanceMillis, final Consumer<LuaState> globalWriter) {
        this.logger = logger;
        this.runtimePerTickMillis = runtimePerTickMillis;
        this.scriptAllowancePerTickMillis = scriptAllowancePerTickMillis;
        this.totalScriptAllowanceNanos = TimeUnit.MILLISECONDS.toNanos(totalScriptAllowanceMillis);

        this.globalWriter = globalWriter;
        this.globalBuilder = LuaState.builder().errorReporter(new Slf4jErrorReporter()).interruptHandler(new ExecutorScriptInterrupt());
    }

    public boolean addScriptFromStream(final String name, final UUID owner, final InputStream stream, Consumer<LuaState> localeWriter) {
        try {
            return addScriptFromBytes(name, owner, stream.readAllBytes(), localeWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addScriptFromString(final String name, final UUID owner, final String script, Consumer<LuaState> localeWriter) {
        byte[] bytes = script.getBytes(StandardCharsets.UTF_8);
        return addScriptFromBytes(name, owner, bytes, localeWriter);
    }

    public boolean addScriptFromBytes(String name, UUID owner, byte[] bytes, Consumer<LuaState> localeWriter) {
        if (stopped.get()) return false;
        final var scriptState = this.globalBuilder.build();
        this.globalWriter.accept(scriptState);
        localeWriter.accept(scriptState);
        final LuaScript script;
        try {
            script = new LuaScript(scriptState, name, owner, bytes);
        } catch (CompileException | LuaError e) {
            logger.warn("script {} failed to compile: {}", name, e.getMessage());
            tellOwner(owner, name, "failed to compile: " + firstLine(e.getMessage()));
            return false;
        }
        // init system globals
        LuaGlobals.system(scriptState, script, () -> this.tick);
        return this.scripts.add(script);
    }

    /**
     * @return true while a script is executing, i.e. when called from inside a Lua function binding
     */
    public boolean isRunningScript() {
        return current != null;
    }

    public int cancelOwnedBy(final UUID owner) {
        int count = 0;
        for (LuaScript script : scripts) {
            if (!owner.equals(script.owner)) continue;
            script.cancelled = true;
            count++;
        }

        scripts.removeIf(s -> s.cancelled);
        LuaScript running = current;
        if (running != null && owner.equals(running.owner)) {
            running.cancelled = true;
            running.interrupt();
            count++;
        }
        return count;
    }

    @Override
    public void run() {
        if (stopped.get() || !runLock.tryLock()) return;
        tick++;

        List<LuaScript> sleeping = new ArrayList<>();
        long tickStart = System.nanoTime();
        long tickBudget = TimeUnit.MILLISECONDS.toNanos(runtimePerTickMillis);
        try {
            while (!stopped.get() && System.nanoTime() - tickStart < tickBudget) {
                LuaScript script = scripts.poll();
                if (script == null) break;
                if (script.wakeTick > tick) {
                    sleeping.add(script);
                    continue;
                }

                script.startTime = System.nanoTime();
                script.timeAllowed = TimeUnit.MILLISECONDS.toNanos(scriptAllowancePerTickMillis);
                script.suspendRequestedAt = 0;
                current = script;
                ScheduledFuture<?> timeout = null;
                try {
                    timeout = watchdog.schedule(script::interrupt, scriptAllowancePerTickMillis, TimeUnit.MILLISECONDS);
                    boolean finished = script.step();
                    script.totalRuntime += System.nanoTime() - script.startTime;
                    if (finished || stopped.get() || script.cancelled) {
                    } else if (script.totalRuntime >= totalScriptAllowanceNanos) {
                        long used = TimeUnit.NANOSECONDS.toMillis(script.totalRuntime);
                        long allowed = TimeUnit.NANOSECONDS.toMillis(totalScriptAllowanceNanos);
                        logger.warn("killing script {}: used {}ms of {}ms total allowance", script.name, used, allowed);
                        tellOwner(script.owner, script.name, "stopped: used its whole time budget (" + used + "ms of " + allowed + "ms)");
                    } else {
                        this.scripts.add(script);
                    }
                } catch (LuaError e) {
                    if (script.cancelled || stopped.get()) {
                    } else if (script.aborted) {
                        logger.warn("script {} aborted: ran too long without yielding", script.name);
                        tellOwner(script.owner, script.name, "stopped: ran too long without yielding (add a sleep() to long loops)");
                    } else {
                        // the message already carries file:line and, once filled, the Lua traceback
                        logger.warn("script {} errored: {}", script.name, e.getMessage());
                        tellOwner(script.owner, script.name, "error: " + firstLine(e.getMessage()));
                    }
                } catch (RejectedExecutionException ignored) {
                } finally {
                    if (timeout != null) timeout.cancel(false);
                    current = null;
                }
            }
        } finally {
            scripts.addAll(sleeping);
            runLock.unlock();
        }
    }

    public void stop() throws InterruptedException {
        if (!stopped.compareAndSet(false, true)) return;
        LuaScript running = current;
        if (running != null) running.interrupt();
        boolean idle = runLock.tryLock(STOP_GRACE.toNanos(), TimeUnit.NANOSECONDS);
        try {
            if (!idle) {
                logger.warn("script {} didn't yield within {}; it will be dropped when it returns", running == null ? "?" : running.name, STOP_GRACE);
            }
            watchdog.shutdownNow();
            logger.info("stopping LuaTaskExecutor, cancelling {} scripts", scripts.size());
            scripts.clear();
        } finally {
            if (idle) runLock.unlock();
        }
    }

    private static void tellOwner(final UUID owner, final String scriptName, final String message) {
        Player player = Bukkit.getPlayer(owner);
        if (player == null) return;
        player.sendMessage(Component.text("[" + scriptName + "] " + message, NamedTextColor.RED));
    }

    private static String firstLine(final String message) {
        if (message == null) return "unknown error";
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    private class Slf4jErrorReporter implements LuaState.ErrorReporter {
        @Override
        public void report(final Throwable error, final Supplier<String> message) {
            logger.error(message.get(), error);
        }
    }

    private class ExecutorScriptInterrupt implements InterruptHandler {

        @Override
        public InterruptAction interrupted() throws LuaError {
            if (stopped.get()) return InterruptAction.SUSPEND;
            LuaScript script = current;
            if (script == null) return InterruptAction.SUSPEND;
            if (script.wakeTick > tick) return InterruptAction.SUSPEND;
            long now = System.nanoTime();
            if (script.aborted || (script.suspendRequestedAt != 0 && now - script.suspendRequestedAt >= script.timeAllowed * 2)) {
                script.aborted = true;
                script.interrupt();
                throw new LuaError("script ran too long without yielding");
            }
            if (!script.cancelled && now - script.startTime < script.timeAllowed) return InterruptAction.CONTINUE;
            if (script.suspendRequestedAt == 0) script.suspendRequestedAt = now;
            return InterruptAction.SUSPEND;
        }
    }
}
