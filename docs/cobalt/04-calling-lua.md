# Calling Lua from Java

How you call Lua depends on where your Java code is running:

| Situation | Use |
|---|---|
| Plain Java code, with no Lua running yet | `LuaThread.runMain(state, fn, args)` |
| Plain Java code that drives a long-lived script which yields | `new LuaThread(state, fn)` + `LuaThread.run(thread, args)` |
| **Inside a Java function that Lua called** | `Dispatch.call` / `Dispatch.invoke` inside a `createS` function |

Don't call `runMain` from inside a function that Lua called. It restarts the main thread under the running script.

## From outside the VM

```lua
-- loaded earlier
function onEvent(name, n) return 'got ' .. name, n * 2 end
```

```java
LuaValue fn = state.globals().rawget("onEvent");
if (fn instanceof LuaFunction f) {
    Varargs r = LuaThread.runMain(state, f, varargsOf(valueOf("join"), valueOf(21)));
    r.arg(1);   // "got join"
    r.arg(2);   // 42
}
```

`rawget` returns `NIL` for undefined globals, so check the type before calling. `checkFunction()` throws a `LuaError` if the value isn't a function.

Calling the stored function again later is fine, and is a normal way to implement event handlers: scripts register callbacks, and Java calls them through `runMain`.

## Host-driven coroutines (scripts that wait)

`coroutine.yield` in the main thread fails with `cannot yield main thread`. To let a script pause and wait for something like "sleep 20 ticks" or "wait for an event", run it in your **own** `LuaThread` and resume it from Java:

```java
LuaThread co = new LuaThread(state, chunk);

Varargs y = LuaThread.run(co, valueOf("arg"));   // starts the thread. The chunk receives "arg" as `...`
// The script ran: local r = coroutine.yield('sleep', 20)
y.arg(1);        // "sleep"
y.arg(2);        // 20
co.getStatus();  // SUSPENDED
co.isAlive();    // true

y = LuaThread.run(co, valueOf("tick1"));        // coroutine.yield(...) returns "tick1" in Lua
// ...later, the script returns 'done', r2
y = LuaThread.run(co, valueOf("tick2"));
y.arg(1);        // "done"
co.getStatus();  // DEAD
co.isAlive();    // false
```

- `run` returns whatever the script **yielded**, or what it **returned** once it finishes. Check `isAlive()` to tell which happened.
- If the script errors, `run` throws `LuaError` and the thread is `DEAD`.
- Resuming a dead thread throws `LuaError: cannot resume dead coroutine`.
- Scripts can still use `coroutine.create`, `resume` and `yield` themselves inside this thread. Only a yield from the thread's top level comes back to Java.

The script side of a "sleep" API is then just `coroutine.yield('sleep', ticks)`. You can wrap it in a Lua helper or a Java function that returns the yield. CC: Tweaked's `os.pullEvent` works this way.

## Java functions that call Lua callbacks

This is the part of Cobalt that's genuinely different from LuaJ.

Cobalt can **yield anywhere, including from inside Java code**. When a Lua function yields, Cobalt doesn't keep a Java thread blocked. It unwinds the Java stack by throwing `UnwindThrowable`, and later **resumes** each Java frame. A Java function that calls into Lua must therefore be able to resume. That's what `createS` is for, and it's why `create` and `createV` lambdas can't call `Dispatch` at all, because they're not allowed to throw `UnwindThrowable`.

### The rule

Inside a `createS` lambda, **before** any call that might yield, store a `SuspendedTask` in `frame.state` that says how to continue. When the callback finishes after a yield, Cobalt calls `task.resume(callbackResults)`, and whatever `resume` returns becomes your function's return value.

### Case 1: the callback call is the last thing you do

This is the easy case: when the function resumes, it just returns what the callback returned.

```java
g.rawset("withValue", LibFunction.createS((s, frame, args) -> {
    LuaFunction cb = args.arg(1).checkFunction();
    frame.state = (SuspendedTask<Varargs>) ret -> ret;   // on resume, return the callback's results
    return Dispatch.invoke(s, cb, valueOf("only"));
}));
```

Verified to work with a callback that yields:

```lua
local co = coroutine.create(function()
  return withValue(function(x) return coroutine.yield('y:' .. x) end)
end)
print(coroutine.resume(co))        -- true  y:only
print(coroutine.resume(co, 'R'))   -- true  R
```

### Case 2: work after the callback returns

You need a small state machine that records how far you got:

```java
final class Twice implements SuspendedTask<Varargs> {
    private final LuaState state;
    private final LuaFunction callback;
    private int step = 0;

    Twice(LuaState state, LuaFunction callback) {
        this.state = state;
        this.callback = callback;
    }

    Varargs start() throws LuaError, UnwindThrowable {
        Dispatch.call(state, callback, valueOf(1));   // may yield. If so, resume() runs with step == 0
        return second();
    }

    private Varargs second() throws LuaError, UnwindThrowable {
        step = 1;
        return Dispatch.invoke(state, callback, valueOf(2));   // may yield. If so, resume() runs with step == 1
    }

    @Override
    public Varargs resume(Varargs ret) throws LuaError, UnwindThrowable {
        return step == 0 ? second() : ret;
    }
}

g.rawset("twice", LibFunction.createS((s, frame, args) -> {
    Twice task = new Twice(s, args.arg(1).checkFunction());
    frame.state = task;            // set before the first call that might yield
    return task.start();
}));
```

```lua
print(twice(function(n) return n * 10 end))   -- 20

local co = coroutine.create(function()
  return twice(function(n) return coroutine.yield('yielded ' .. n) end)
end)
print(coroutine.resume(co))        -- true  yielded 1
print(coroutine.resume(co, 'A'))   -- true  yielded 2
print(coroutine.resume(co, 'B'))   -- true  B
```

### If you get it wrong

If you call `Dispatch` from `createS` **without** setting `frame.state`, everything works until a callback yields. Then resuming fails with:

```
vm error: java.lang.NullPointerException: Cannot invoke "...SuspendedTask.resume(...)" because "object" is null
```

If your callbacks never yield (no coroutines in your scripts and no `SUSPEND` interrupts, see page 6), you'll never hit this. Setting `frame.state` costs one line, though, so always set it.

### Dispatch API

All of these respect `__call`, and all throw `LuaError` and `UnwindThrowable`:

```java
Dispatch.call(state, fn);                     // returns the first result as a LuaValue
Dispatch.call(state, fn, a);
Dispatch.call(state, fn, a, b);
Dispatch.call(state, fn, a, b, c);
Dispatch.invoke(state, fn, varargs);          // returns all results as Varargs
```

The same resume rule applies to `OperationHelper` methods that can run metamethods, such as `getTable`, `setTable`, `toString`, `eq`, `lt` and `length`.
