# Errors

## `LuaError`

Every Lua error is a `LuaError`, which is a checked `Exception`. You'll meet it from:
- `LuaThread.runMain` and `LuaThread.run`, when the script errors
- every `check*` and `opt*` accessor
- your own Java functions, when you throw one

`LoadState.load` throws `CompileException` for syntax errors, which is a separate exception class.

### Message versus value

```java
try {
    LuaThread.runMain(state, chunk);
} catch (LuaError e) {
    e.getValue();     // the error value. Usually a string with the position: "test:1: deep"
    e.getMessage();   // the value as a string, plus a stack traceback
}
```

Observed output for `local function f() error('deep') end f()`:

```
getValue():   test:1: deep
getMessage(): test:1: deep
              stack traceback:
                  test:1: in local 'f'
                  test:2: in main chunk
                  ...
```

Show `getValue()` to players or users, and log `getMessage()` for yourself.

### Non-string error values

`error({ code = 42 })` keeps the table:

```java
catch (LuaError e) {
    if (e.getValue() instanceof LuaTable t) {
        int code = t.rawget("code").toInteger();   // 42
    }
}
```

### Common runtime messages

```
test:1: attempt to index local 't' (a nil value)
test:1: attempt to index global 'origin' (a Point value)   -- __name from the metatable
test:1: bad argument (number expected, got string)
test:1: cannot yield main thread
```

## Throwing from Java functions

| Throw | Lua sees |
|---|---|
| `new LuaError("custom failure")` | `test:1: custom failure`. The position of the Lua caller is added. |
| `new LuaError("no position", 0)` | `no position` |
| `new LuaError(someLuaValue)` | That value, like `error(value)` |
| `ErrorFactory.argError(1, "must be positive")` | `test:1: bad argument #1 (must be positive)` |

All of these can be caught with `pcall`:

```lua
print(pcall(fail))   -- false   test:1: custom failure
```

## Java exceptions inside Lua

A `RuntimeException` thrown by your Java function (an NPE, an `IllegalStateException` and so on) **doesn't crash the host**. Cobalt wraps it:

```
test:1: vm error: java.lang.IllegalStateException: oops
```

- `pcall` catches it like any Lua error.
- `e.getCause()` on the resulting `LuaError` returns the original exception.
- If you set `LuaState.builder().errorReporter(...)`, Cobalt reports it there as well, as "Uncaught Java exception". Use this to log real bugs in your bindings, separately from script errors.

Cobalt catches `Exception` and `VirtualMachineError` (which includes `StackOverflowError` and `OutOfMemoryError`) and turns them into Lua errors. Deep Lua recursion gives a normal `rec:1: stack overflow` error. **Any other `Error` subclass isn't wrapped.** It propagates straight out of `runMain`, past every `pcall`. You can use this deliberately to [hard-abort a script](06-interrupts.md#hard-abort-cant-be-caught).

## Recovering

After a normal `LuaError` the state stays usable, so you can run more code in it.
