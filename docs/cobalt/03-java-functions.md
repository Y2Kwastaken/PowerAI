# Java functions

Java functions visible to Lua are `LibFunction` objects. You build them from lambdas. The `ZeroArgFunction`, `OneArgFunction` and similar subclasses are **sealed**, so you can't extend them as you would in LuaJ.

## Factories

| Factory | Lambda | Use when |
|---|---|---|
| `LibFunction.create(ZeroArg)` | `(state) -> LuaValue` | No arguments, one return value |
| `LibFunction.create(OneArg)` | `(state, a) -> LuaValue` | One argument, one return value |
| `LibFunction.create(TwoArg)` | `(state, a, b) -> LuaValue` | Two arguments, one return value |
| `LibFunction.create(ThreeArg)` | `(state, a, b, c) -> LuaValue` | Three arguments, one return value |
| `LibFunction.createV(ManyArgs)` | `(state, args) -> Varargs` | Any number of arguments or return values |
| `LibFunction.createS(Suspended)` | `(state, frame, args) -> Varargs` | **The function calls back into Lua**. See [Calling Lua](04-calling-lua.md#java-functions-that-call-lua-callbacks). |

The fixed-arity versions fill missing arguments with `nil` and ignore extra ones, so `add(1)` passes `NIL` as `b`.

Every lambda may throw `LuaError`. Only the `createS` lambda may also throw `UnwindThrowable`. That's why calling `Dispatch.call` inside a `create` or `createV` lambda **doesn't compile**, and it's intentional (see page 4).

```java
LuaTable g = state.globals();

g.rawset("add", LibFunction.create((s, x, y) ->
    valueOf(x.checkDouble() + y.checkDouble())));

g.rawset("greet", LibFunction.create((s, name) ->
    valueOf("Hello, " + name.optString("stranger"))));

g.rawset("minmax", LibFunction.createV((s, args) -> {
    double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
    for (int i = 1; i <= args.count(); i++) {
        double v = args.arg(i).checkDouble();
        min = Math.min(min, v);
        max = Math.max(max, v);
    }
    return varargsOf(valueOf(min), valueOf(max));   // multiple return values
}));
```

```lua
print(add(1, 2), greet('Ada'), greet(), minmax(5, 1, 9))
-- 3   Hello, Ada   Hello, stranger   1   9
```

Return `Constants.NIL` for "no value". From `createV`, return `Constants.NONE` for zero return values.

## Argument errors

`check*` failures turn into Lua errors at the caller's line:

```
add(1, 'x')   ->  test:1: bad argument (number expected, got string)
```

**These messages don't include the argument number or the function name.** That differs from reference Lua's `bad argument #2 to 'add'`. If you want the number, throw it yourself:

```java
g.rawset("needpos", LibFunction.create((s, n) -> {
    if (n.checkInteger() < 0) throw ErrorFactory.argError(1, "must be positive");
    return n;
}));
// needpos(-1)  ->  test:1: bad argument #1 (must be positive)
```

| Helper | Message |
|---|---|
| `ErrorFactory.argError(int index, String msg)` | `bad argument #index (msg)` |
| `ErrorFactory.argError(LuaValue value, String expected)` | `bad argument (expected expected, got <type>)` |
| `ErrorFactory.typeError(LuaValue value, String expected)` | `<expected> expected, got <type>` |
| `new LuaError("msg")` | `msg`, with the caller's position added |
| `new LuaError("msg", 0)` | `msg` with no position (level 0, like `error(msg, 0)`) |

## Libraries (tables of functions)

`RegisteredFunction` builds a table of named functions. It also sets each function's debug name, which then appears in tracebacks.

```java
LuaTable lib = RegisteredFunction.bind(new RegisteredFunction[]{
    RegisteredFunction.of("double", (s, x) -> valueOf(x.checkDouble() * 2)),
    RegisteredFunction.of("version", s -> valueOf("1.0")),
    RegisteredFunction.ofV("many", (s, args) -> args),                 // varargs
    // RegisteredFunction.ofS("withCallback", (s, frame, args) -> ...) // see page 4
});

LibFunction.setGlobalLibrary(state, "mylib", lib);   // sets _G.mylib and registers it in the registry's _LOADED table
```

```lua
print(mylib.double(21), mylib.version())   -- 42   1.0
```

`RegisteredFunction.bind(existingTable, functions)` adds functions to a table you already have.

## Wrapping Java objects (userdata)

To give Lua a handle on a Java object, wrap it in userdata and give the userdata a metatable whose `__index` holds its methods. Lua's `obj:method()` passes the userdata as the first argument (`self`).

```java
record Point(int x, int y) {}

static Point checkPoint(LuaValue v) throws LuaError {
    if (v instanceof LuaUserdata u && u.userdata() instanceof Point p) return p;
    throw ErrorFactory.argError(v, "Point");
}

LuaTable pointMeta = new LuaTable();
pointMeta.rawset(Constants.INDEX, RegisteredFunction.bind(new RegisteredFunction[]{
    RegisteredFunction.of("x", (s, self) -> valueOf(checkPoint(self).x())),
    RegisteredFunction.of("y", (s, self) -> valueOf(checkPoint(self).y())),
}));
pointMeta.rawset(Constants.NAME, valueOf("Point"));      // the type name used in error messages
pointMeta.rawset(Constants.TOSTRING, LibFunction.create((s, self) ->
    valueOf("Point" + checkPoint(self))));

state.globals().rawset("origin", userdataOf(new Point(3, 4), pointMeta));
```

```lua
print(origin:x(), origin:y(), type(origin), tostring(origin))
-- 3   4   userdata   PointPoint[x=3, y=4]

origin.x(5)     -- bad argument (Point expected, got number)          (used . instead of :)
origin.z = 1    -- attempt to index global 'origin' (a Point value)   (no __newindex, and __name is used)
```

Tips:
- **Build each metatable once** and share it across all instances of a type.
- **Check `self` in every method.** Scripts can call `getmetatable(obj).__index.x(somethingElse)`, unless you set `__metatable` to hide the metatable.
- **Holding a Java object in Lua keeps it reachable from Java's point of view.** For things like players or worlds, store an ID or a weak reference and look the object up on each call, so scripts can't keep stale objects alive.

## Replacing `print`

To route `print` somewhere else, such as a plugin logger or a player's chat:

```java
state.globals().rawset("print", LibFunction.createV((s, args) -> {
    StringBuilder line = new StringBuilder();
    for (int i = 1; i <= args.count(); i++) {
        if (i > 1) line.append('\t');
        line.append(OperationHelper.toStringDirect(args.arg(i)));
    }
    logger.info(line.toString());
    return Constants.NONE;
}));
```

`OperationHelper.toStringDirect` doesn't call `__tostring`, because calling it could run Lua code. If you need `__tostring` support, write the function with `createS` and `OperationHelper.toString(state, v)`.
