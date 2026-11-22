# Cobalt guide

Cobalt is a Lua VM written in pure Java. It started as a fork of LuaJ and is maintained for the CC: Tweaked Minecraft mod.

- **Source:** https://codeberg.org/cc-tweaked/Cobalt
- **Maven:** `cc.tweaked:cobalt` from `https://maven.squiddev.cc`
- **Covers version:** `0.9.10`. Every code sample and every output shown in these pages was compiled and run against that version.

> **Pin the version.** Cobalt's README warns that it changes along with CC: Tweaked and has no API-stability guarantee between versions. When you upgrade, re-check these docs against the new sources.

## Pages

1. [Getting started](01-getting-started.md): create a `LuaState`, load libraries, run code, read results
2. [Values](02-values.md): `LuaValue`, numbers, strings, tables, `Varargs`, userdata
3. [Java functions](03-java-functions.md): expose Java to Lua, check arguments, build libraries, wrap Java objects
4. [Calling Lua from Java](04-calling-lua.md): call Lua functions, host-driven coroutines, Java functions that call Lua callbacks
5. [Errors](05-errors.md): `LuaError`, tracebacks, Java exceptions, `pcall`
6. [Timeouts and interrupts](06-interrupts.md): kill runaway scripts, and pause or resume long-running work
7. [Sandboxing](07-sandboxing.md): what each library exposes, file access, and isolated environments

## The 30-second version

```java
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.lib.CoreLibraries;
import org.squiddev.cobalt.lib.system.ResourceLoader;
import org.squiddev.cobalt.lib.system.SystemBaseLib;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

LuaState state = new LuaState();
CoreLibraries.standardGlobals(state);                                      // base, table, string, coroutine, math, utf8
new SystemBaseLib(ResourceLoader.FILES, System.in, System.out).add(state); // print, dofile, loadfile, collectgarbage

state.globals().rawset("greet", LibFunction.create((s, name) ->
    ValueFactory.valueOf("Hello, " + name.checkString())));

String code = "print(greet('world')) return 6 * 7";
LuaClosure chunk = LoadState.load(state,
    new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)), "=main", state.globals());

Varargs result = LuaThread.runMain(state, chunk);   // prints "Hello, world"
int answer = result.first().checkInteger();         // 42
```

## Mental model

| Concept | Cobalt type | Notes |
|---|---|---|
| A Lua VM | `LuaState` | Holds the globals, the registry, metatables for primitive types and the main thread. **Not thread-safe:** use each state from one thread at a time. |
| Global table (`_G`) | `LuaTable`, from `state.globals()` | Read and write it with `rawget` and `rawset`. |
| Any Lua value | `LuaValue` | Subclasses include `LuaNil`, `LuaBoolean`, `LuaInteger`, `LuaDouble`, `LuaString`, `LuaTable`, `LuaFunction`, `LuaUserdata` and `LuaThread`. |
| Several values (arguments or returns) | `Varargs` | `LuaValue` itself is a `Varargs` of one value. Indexes start at **1**. |
| Compiled chunk | `LuaClosure` | Returned by `LoadState.load`. |
| Java function visible to Lua | `LibFunction` | Create one with `LibFunction.create`, `createV` or `createS`. |
| Running code | `LuaThread.runMain` / `LuaThread.run` | You can't call a function directly. Every call goes through a thread. |
| Lua error | `LuaError` (checked exception) | Every `check*` method throws it. |

## What's different from LuaJ

If you find LuaJ examples online, their ideas carry over but their method names don't:

| LuaJ | Cobalt |
|---|---|
| `JsePlatform.standardGlobals()` returns `Globals` | `new LuaState()` + `CoreLibraries.standardGlobals(state)`. The globals live in `state.globals()` |
| `globals.load(code).call()` | `LuaThread.runMain(state, LoadState.load(...))` |
| `fn.call(arg)` | `LuaThread.runMain(state, fn, arg)` from outside the VM, `Dispatch.call(state, fn, arg)` from inside it |
| `new OneArgFunction() { call(LuaValue) }` | `LibFunction.create((state, arg) -> ...)`. The subclasses are sealed. |
| `value.checkjstring()`, `checkint()` | `checkString()`, `checkInteger()` (camelCase) |
| `LuaValue.valueOf(...)` | `ValueFactory.valueOf(...)` |
| `table.set(k, v)` / `get(k)` | `rawset` / `rawget` (no metamethods), or `OperationHelper.setTable` / `getTable` (with metamethods) |
| `luajava` library, `CoerceJavaToLua` | **Doesn't exist.** You expose Java functions and userdata yourself. |
| `debug.sethook` for timeouts | `LuaState.builder().interruptHandler(...)` plus `state.interrupt()` |

## Language level

Cobalt is mostly Lua 5.2 (`_VERSION` is `"Lua 5.2"`), with additions:

- **Lua 5.1 compatibility:** `getfenv`, `setfenv`, `loadstring` and a global `unpack` exist, and `goto` still works.
- **Backported from 5.3 and 5.4:** `utf8.*`, `string.pack`, `string.unpack`, `string.packsize` and `table.move`.
- **Not supported:** the `//` operator, bitwise operators (`&`, `|`, `~`, `<<`), `math.type` and the integer/float distinction you see in Lua 5.3. Numbers print without a trailing `.0`, so `10 / 2` prints `5`.
- **Not installed by default:** `io`, `os`, `require`, `package`, `debug` and `bit32`. See [Sandboxing](07-sandboxing.md).

For the language itself, use the Lua 5.2 reference manual: https://www.lua.org/manual/5.2/

## Learning from the source

- IntelliJ: Ctrl+click a Cobalt class, then choose **Download Sources**. Cobalt publishes a sources jar but no javadoc jar.
- Cobalt's tests in `src/test/java/org/squiddev/cobalt/`. `ScriptHelper.java`, `CoroutineTest.java` and `ProtectionTest.java` are the most useful.
- CC: Tweaked's `CobaltLuaMachine.java` (`projects/core/src/main/java/dan200/computercraft/core/lua/`) shows a production integration: sandboxing, timeouts, value conversion and host-driven coroutines.
