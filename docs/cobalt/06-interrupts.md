# Timeouts and interrupts

Cobalt has no instruction-count limit. Instead it uses **interrupts**:

1. Give the state an `InterruptHandler` when you build it.
2. Another thread calls `state.interrupt()`, which only sets a volatile flag.
3. At its next safe point, the VM thread calls your handler, which decides what happens next.

| The handler... | Effect |
|---|---|
| returns `InterruptAction.CONTINUE` | Keeps running |
| throws `LuaError` | Raises a normal Lua error at the current line. **`pcall` can catch it.** |
| throws an `Error` subclass | Escapes all the way out of `runMain` / `run`. **`pcall` can't catch it.** |
| returns `InterruptAction.SUSPEND` | Pauses the VM. `runMain` / `run` returns `null`, and you can resume later. |

The handler runs **on the VM thread**, not the thread that called `interrupt()`.

Calling `interrupt()` on a state built without a handler throws `IllegalStateException`.

## Timeout (soft, catchable)

```java
final long[] deadline = {Long.MAX_VALUE};

LuaState state = LuaState.builder()
    .interruptHandler(() -> {
        if (System.nanoTime() > deadline[0]) throw new LuaError("timed out");
        return InterruptAction.CONTINUE;
    })
    .build();

// A watchdog that pokes the VM regularly
ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
watchdog.scheduleAtFixedRate(state::interrupt, 20, 20, TimeUnit.MILLISECONDS);

deadline[0] = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
try {
    LuaThread.runMain(state, compile("while true do end"));
} catch (LuaError e) {
    e.getValue();   // "loop:1: timed out"
} finally {
    deadline[0] = Long.MAX_VALUE;
}
```

The state is still usable afterwards.

**Problem:** a script can defeat this with `pcall`. For example, `while true do pcall(function() while true do end end) end` catches each timeout error and keeps going forever. This was tested, and it hangs.

## Hard abort (can't be caught)

Cobalt's VM loop only converts `Exception` and `VirtualMachineError` into Lua errors. Throw your own `Error` subclass and it goes straight past every `pcall`:

```java
static final class HardAbort extends Error {
    HardAbort() { super("script ran too long"); }
}

.interruptHandler(() -> {
    if (hardDeadlinePassed) throw new HardAbort();
    return InterruptAction.CONTINUE;
})

try {
    LuaThread.runMain(state, chunk);
} catch (HardAbort e) {
    // Even the pcall loop above is stopped
}
```

**After a hard abort, throw the `LuaState` away.** The stack was abandoned in the middle of execution, so the state's internals are no longer consistent.

### What CC: Tweaked does (recommended)

CC: Tweaked uses both levels:

1. **Soft abort:** throw a `LuaError("Too long without yielding")` **once**. Well-behaved scripts error out normally, and their `pcall`s get a chance to clean up.
2. **Hard abort:** if the script is still running a few seconds later, throw a private `Error` subclass, then discard the whole state.

```java
private boolean thrownSoft;

.interruptHandler(() -> {
    if (hardAborted) throw new HardAbort();
    if (softAborted && !thrownSoft) {
        thrownSoft = true;
        throw new LuaError("Too long without yielding");
    }
    return InterruptAction.CONTINUE;
})
```

## Suspend (time slicing)

`SUSPEND` pauses the script **in the middle of execution**, even inside a tight loop. `runMain` / `run` then returns `null`, and calling `run` again continues exactly where it stopped. You can use this to spread one long computation across many server ticks instead of freezing one.

```java
LuaState state = LuaState.builder()
    .interruptHandler(() -> InterruptAction.SUSPEND)
    .build();
CoreLibraries.standardGlobals(state);

// someone calls state.interrupt() periodically, e.g. once per time budget
Varargs out = LuaThread.runMain(state, compile("local n = 0 for i = 1, 2e7 do n = n + i end return n"));
while (out == null) {                       // null means suspended, not finished
    // ...do other work, wait for the next tick...
    out = LuaThread.run(state.getMainThread(), Constants.NONE);
}
out.first();   // 200000010000000. In testing this was suspended 84 times on the way.
```

To resume a script you started with `new LuaThread(state, fn)` (see page 4), call `LuaThread.run(thatThread, Constants.NONE)` instead. In both cases, **`null` means suspended**, which is different from a normal yield that returns values.

If a Java function in the call stack used `createS` without setting `frame.state` (see page 4), resuming after a suspend fails. A suspend unwinds Java frames exactly as a yield does.

## Budget per tick (Paper example)

This is a sketch, not tested in a server:

```java
private volatile long sliceEnd;
private final LuaState state = LuaState.builder()
    .interruptHandler(() -> System.nanoTime() > sliceEnd ? InterruptAction.SUSPEND : InterruptAction.CONTINUE)
    .build();
private LuaThread script;   // new LuaThread(state, chunk), created when the script is loaded

// A separate timer thread calls state.interrupt() about every 1 ms, so the VM checks the budget often.

// Each tick, on the main thread:
void tick() throws LuaError {
    if (!script.isAlive()) return;
    sliceEnd = System.nanoTime() + 2_000_000;           // 2 ms budget for this tick
    Varargs out = LuaThread.run(script, Constants.NONE);
    if (out == null) return;                            // budget used up, continue next tick
    // otherwise the script yielded or finished: handle `out`
}
```

Combine this with a soft and hard abort based on **total** CPU time used, so an infinite loop doesn't just run forever at 2 ms per tick.
