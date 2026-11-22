# Sandboxing

Cobalt starts out safer than LuaJ: there's no `io`, `os`, `require` or Java reflection library unless you add one. What a script can reach is exactly what you put in its environment.

## What each library installs

Checked against the 0.9.10 sources.

| Installer | Globals | Safe for untrusted code? |
|---|---|---|
| `BaseLib.add` | `assert` `error` `getfenv` `setfenv` `getmetatable` `setmetatable` `ipairs` `pairs` `next` `load` `loadstring` `pcall` `xpcall` `rawequal` `rawget` `rawset` `rawlen` `select` `tonumber` `tostring` `type` `unpack`, plus `_G` and `_VERSION` | Mostly. `getfenv` and `setfenv` let a script read or replace the environment of functions it can reach. |
| `TableLib.add` | `table.*`: `concat` `insert` `remove` `sort` `pack` `unpack` `move`, plus the 5.1 functions `getn` `maxn` `foreach` `foreachi` | Yes |
| `StringLib.add` | `string.*`: `byte` `char` `find` `format` `gmatch` `gsub` `len` `lower` `match` `rep` `reverse` `sub` `upper` `pack` `unpack` `packsize` `dump` | Yes. `string.rep` can allocate large strings. |
| `CoroutineLib.add` | `coroutine.*` | Yes |
| `MathLib.add` | `math.*` | Yes |
| `Utf8Lib.add` | `utf8.*` | Yes |
| `CoreLibraries.standardGlobals` | All six above | Yes, with the notes above |
| `Bit32Lib.add` | `bit32.*` (not in `standardGlobals`) | Yes |
| `DebugLib.add` / `CoreLibraries.debugGlobals` | `debug.*` | **No.** It can read and change locals and upvalues, and reach the registry. |
| `new SystemBaseLib(loader, in, out).add` | `print` `dofile` `loadfile` `collectgarbage` | **Depends on the `ResourceLoader`.** With `ResourceLoader.FILES` it can read **any file** the JVM can. |

Verified in a state with `standardGlobals` + `SystemBaseLib`:

```lua
print(io, os, require, package, debug, bit32)   -- nil nil nil nil nil nil
```

## File access: `dofile` and `loadfile`

`ResourceLoader` is a single-method interface, `InputStream load(String filename)`, that returns `null` for "not found". Restrict it to one folder:

```java
Path root = scriptsDir.toAbsolutePath().normalize();

ResourceLoader scriptsOnly = name -> {
    Path p = root.resolve(name).normalize();
    if (!p.startsWith(root)) return null;          // blocks ../../server.properties
    try { return Files.newInputStream(p); } catch (IOException e) { return null; }
};

new SystemBaseLib(scriptsOnly, InputStream.nullInputStream(), System.out).add(state);
```

Alternatively, don't install `SystemBaseLib` at all, and define your own `print` (see [Java functions](03-java-functions.md#replacing-print)).

`collectgarbage("collect")` calls `System.gc()`. If scripts are untrusted, remove it after installing: `state.globals().rawset("collectgarbage", Constants.NIL)`.

## Bytecode

Precompiled bytecode is a classic way to escape a Lua sandbox. Cobalt refuses it by default:

```lua
load(string.char(27) .. 'Lua')   -- nil, "attempt to load a binary chunk"
string.dump(function() end)      -- error: unable to dump given function
```

This stays true as long as you **don't** call `LuaState.builder().bytecodeFormat(...)`.

## Per-script environments

The last argument to `LoadState.load` is the chunk's environment. Give each script its own table to keep scripts from interfering with each other:

```java
LuaTable env = new LuaTable();
env.rawset("print", state.globals().rawget("print"));   // copy in only what the script should see

LuaClosure c = LoadState.load(state, input, "=sandboxed", env);
LuaThread.runMain(state, c);
```

```lua
-- inside the script:
x = 5
print(x, string)   -- 5   nil   (string wasn't copied in)
```

Afterwards `x` is set in `env` and **not** in `state.globals()`.

### Sharing the standard library safely

To give each script the full standard library, you have two options:
- **Copy the globals table** into each environment, with a `next` loop (see [Values](02-values.md#iterating)). Library tables such as `string` and `table` are still **shared objects**, so one script could do `string.upper = nil` and break the others.
- **One `LuaState` per script.** This gives full isolation. A `LuaState` is reasonably cheap, and it also isolates timeouts, because a hard abort only discards that one state.

Strings share one metatable per state (`state.stringMetatable`), so `getmetatable('').__index` is the `string` table. With a shared state, that's another way for scripts to affect each other.

## Resource limits Cobalt doesn't enforce

- **CPU:** you have to enforce it yourself with interrupts. See [Timeouts and interrupts](06-interrupts.md).
- **Memory:** there's no limit. A script can run `local t = {} while true do t[#t+1] = ('x'):rep(1e6) end`. Only process-level measures help: run untrusted code in a separate JVM, or watch heap use and hard-abort.
- **Stack depth:** deep recursion produces a normal `stack overflow` Lua error, so it doesn't crash the JVM.

## Threading

A `LuaState` is **not thread-safe**. Only touch a state, and the values that belong to it, from one thread at a time. The one exception is `state.interrupt()`, which is designed to be called from other threads.

In a Paper plugin, scripts that call the Bukkit API must run on the server thread. Putting the VM on another thread means every binding has to hop back to the main thread, so it's usually simpler to run on the main thread with a per-tick budget ([Timeouts and interrupts](06-interrupts.md#budget-per-tick-paper-example)).
