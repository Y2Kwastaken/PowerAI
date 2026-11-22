# Getting started

## Dependency

```toml
# gradle/libs.versions.toml
[versions]
cobalt = "0.9.10"

[libraries]
cobalt = { module = "cc.tweaked:cobalt", version.ref = "cobalt" }
```

```kotlin
// build.gradle.kts
repositories {
    maven("https://maven.squiddev.cc") {
        name = "squiddev"
        content { includeGroup("cc.tweaked") }
    }
}

dependencies {
    implementation(libs.cobalt)
}
```

Cobalt has no runtime dependencies. Its only compile-time dependency is Checker Framework annotations.

## 1. Create a state

```java
LuaState state = new LuaState();
```

Use the builder when you need options. The most important one is an interrupt handler, which you need for timeouts (see [Timeouts and interrupts](06-interrupts.md)):

```java
LuaState state = LuaState.builder()
    .interruptHandler(() -> InterruptAction.CONTINUE)
    .errorReporter((error, message) -> logger.warn(message.get(), error))
    .build();
```

| Builder option | Purpose |
|---|---|
| `interruptHandler(InterruptHandler)` | Called when another thread runs `state.interrupt()`. Without one, `interrupt()` throws `IllegalStateException`. |
| `errorReporter(ErrorReporter)` | Called when Java code throws a non-`LuaError` exception inside the VM. Use it to log real bugs. |
| `bytecodeFormat(BytecodeFormat)` | Allows loading precompiled bytecode. Leave it unset: without it, binary chunks are rejected, which is safer. |
| `compiler(FunctionFactory)` | Replaces the closure factory. You won't need it. |

## 2. Install libraries

A new state has **empty globals**: not even `print` exists.

```java
CoreLibraries.standardGlobals(state);   // base, table, string, coroutine, math, utf8
CoreLibraries.debugGlobals(state);      // the same plus `debug`. Don't expose it to untrusted code.
```

`print` comes from a separate, opt-in library because it needs I/O streams, and `dofile` and `loadfile` need file access:

```java
new SystemBaseLib(ResourceLoader.FILES, System.in, System.out).add(state);
```

| `SystemBaseLib` argument | Controls |
|---|---|
| `ResourceLoader` | How `dofile` and `loadfile` open files. `ResourceLoader.FILES` reads **any path on disk**. Write your own for scripts (see [Sandboxing](07-sandboxing.md)). |
| `InputStream` | `stdin`, used when `dofile()` or `loadfile()` is called with no argument |
| `PrintStream` | Where `print` writes |

To send `print` output to a logger instead of stdout, either define your own `print` function (see [Java functions](03-java-functions.md)) or pass a `PrintStream` that wraps the logger.

You can also install single libraries, such as `BaseLib.add(state)`, `MathLib.add(state)` or `Bit32Lib.add(state)`. See [Sandboxing](07-sandboxing.md) for what each one contains.

## 3. Compile code

```java
LuaClosure chunk = LoadState.load(
    state,
    new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)),
    "=main",               // chunk name, used in error messages
    state.globals()        // the environment the chunk sees as _ENV
);
```

`LoadState.load` throws `CompileException` for syntax errors, so you don't need to run the code to find them:

```
CompileException: bad:1: <name> expected near '='
```

### Chunk names

The chunk name decides how errors refer to your code. It follows the same rules as reference Lua:

| Name passed | Error prefix | Use for |
|---|---|---|
| `"script"` | `[string "script"]:1:` | Strings of code (the default style) |
| `"@scripts/hello.lua"` | `scripts/hello.lua:1:` | Code loaded from a file |
| `"=hello"` | `hello:1:` | Any custom label |

### Environment

The last argument becomes the chunk's `_ENV`, so global reads and writes in that chunk go to that table. Pass `state.globals()` for the usual behavior, or pass a separate `LuaTable` to isolate a script (see [Sandboxing](07-sandboxing.md#per-script-environments)).

## 4. Run it

```java
Varargs results = LuaThread.runMain(state, chunk);
```

`runMain` runs the function on the state's main thread and returns everything the chunk returned. It throws `LuaError` if the script errors.

```java
Varargs r = LuaThread.runMain(state, compile("return 1, 'two', nil, {}"));
r.count();                    // 4
r.first();                    // 1
r.arg(2).checkString();       // "two"
r.arg(3).isNil();             // true
r.arg(9).isNil();             // true: out-of-range indexes return nil
r.arg(4) instanceof LuaTable; // true
```

To pass arguments to a chunk, which reads them as `...`:

```java
LuaThread.runMain(state, chunk, ValueFactory.varargsOf(ValueFactory.valueOf("a"), ValueFactory.valueOf(2)));
```

## A reusable helper

Most hosts end up with something like this:

```java
static Varargs exec(LuaState state, String name, String code) throws LuaError, CompileException {
    LuaClosure fn = LoadState.load(state,
        new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)), name, state.globals());
    return LuaThread.runMain(state, fn);
}
```

## Reusing a state

A `LuaState` keeps working after a script errors, so you can run many chunks in one state and they share globals. Make a new state when you want a clean slate, or after a hard abort (see [Timeouts and interrupts](06-interrupts.md#hard-abort-cant-be-caught)).
