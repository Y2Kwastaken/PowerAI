# Values

Every Lua value is a `LuaValue`. Values are immutable, except for `LuaTable`.

```java
import static org.squiddev.cobalt.ValueFactory.*;   // valueOf, tableOf, listOf, varargsOf, userdataOf
import org.squiddev.cobalt.Constants;               // NIL, TRUE, FALSE, NONE, metamethod names
```

## Creating values (`ValueFactory`)

| Java | Lua |
|---|---|
| `Constants.NIL` | `nil` |
| `valueOf(true)`, `Constants.TRUE` or `Constants.FALSE` | boolean |
| `valueOf(42)` | number, a `LuaInteger` |
| `valueOf(4.2)` | number, a `LuaDouble` (or a `LuaInteger` if the value is whole) |
| `valueOf("text")` | string, a `LuaString` |
| `valueOf(byte[])` | string made of raw bytes |
| `tableOf()` | `{}` |
| `listOf(a, b, c)` | `{a, b, c}` |
| `tableOf(k1, v1, k2, v2)` | `{[k1]=v1, [k2]=v2}`. Throws `LuaError`, and skips nil values. |
| `userdataOf(obj)` / `userdataOf(obj, metatable)` | userdata wrapping a Java object |
| `varargsOf(a, b, ...)` | several values, for function arguments or returns |
| `Constants.NONE` | zero values |

## Reading values

Each type has three families of accessor:

| Family | On the wrong type | Example |
|---|---|---|
| `checkX()` | Throws `LuaError` | `v.checkString()`, `checkInteger()`, `checkLong()`, `checkDouble()`, `checkBoolean()`, `checkTable()`, `checkFunction()`, `checkThread()`, `checkNumber()`, `checkLuaString()` |
| `optX(default)` | Returns `default` if the value is nil, and throws `LuaError` for any other wrong type | `v.optString("stranger")`, `optInteger(0)`, `optTable(null)` |
| `toX()` | Never throws. Returns a "falsy" fallback. | `v.toBoolean()` (Lua truthiness), `toInteger()`, `toDouble()`, `toString()` |

Use `check*` and `opt*` for function arguments, so wrong input gives the script a proper Lua error.

Observed behavior in 0.9.10:

```java
valueOf(3.9).checkInteger();        // 3: truncates, doesn't throw
valueOf("12").checkInteger();       // 12: numeric strings are coerced, like in Lua
valueOf("abc").checkInteger();      // LuaError: "bad argument (number expected, got string)"
valueOf(3.9).toInteger();           // 3
```

### Type checks

```java
v.isNil();   v.isNumber();   v.isString();   v.isThread();
v.type();        // an int, compare against Constants.TNIL, TBOOLEAN, TNUMBER, TSTRING, TTABLE, TFUNCTION, TUSERDATA, TTHREAD
v.typeName();    // "nil", "number", "table", ...
```

For tables, functions and userdata, use `instanceof` with a pattern variable:

```java
if (v instanceof LuaTable t) { ... }
if (v instanceof LuaFunction f) { ... }
if (v instanceof LuaUserdata u && u.userdata() instanceof MyThing thing) { ... }
```

`isString()` and `isNumber()` follow Lua's coercion rules, so a number counts as a string and a numeric string counts as a number. Use `type()` when you need the exact type.

## Strings

`LuaString` is a sequence of **bytes**, as in reference Lua, not a Java `String`. `valueOf(String)` encodes to UTF-8, and `toString()` and `checkString()` decode it again. If you're handling binary data, use `checkLuaString()` together with `length()`, `byteAt(i)`, `copyTo(...)` and `toInputStream()`.

## Varargs

`Varargs` holds several values: function arguments, return values or a chunk's `...`.

```java
args.count();          // how many values there are
args.first();          // the same as arg(1). Returns NIL if empty.
args.arg(i);           // 1-based. Returns NIL past the end, never throws.
args.subargs(2);       // drops the first value
args.checkValue(i);    // like arg(i), but throws "bad argument #i (value expected)" if it's missing
```

A single `LuaValue` already is a `Varargs`, so you can return `valueOf(x)` where `Varargs` is expected. `varargsOf(LuaValue)` with a single argument is deprecated for this reason.

## Tables

```java
LuaTable t = new LuaTable();
t.rawset("name", valueOf("Steve"));         // t.name = "Steve"
t.rawset(1, valueOf("first"));              // t[1] = "first"
t.rawset(valueOf(true), valueOf("yes"));    // any key. Throws LuaError for a nil or NaN key.

t.rawget("name");    // LuaValue, NIL if missing
t.rawget(1);
t.length();          // the # operator without __len
```

### raw versus metamethod-aware access

`rawget` and `rawset` **ignore metatables**. On a proxy table that uses `__index`, `rawget` returns nil:

```java
LuaTable proxy = /* setmetatable({}, { __index = function(_, k) return 'meta ' .. k end }) */;
proxy.rawget("foo");                                       // nil
OperationHelper.getTable(state, proxy, valueOf("foo"));    // "meta foo"
```

`OperationHelper.getTable`, `setTable`, `length`, `eq`, `lt`, `add` and similar methods respect metamethods. They can call back into Lua, so they throw `UnwindThrowable` as well as `LuaError`. You can only call them from inside the VM, from a `createS` function (see [Calling Lua](04-calling-lua.md#java-functions-that-call-lua-callbacks)). For tables your own Java code builds, `rawget` and `rawset` are what you want.

### Iterating

```java
LuaValue k = Constants.NIL;
while (true) {
    Varargs next = table.next(k);    // throws LuaError if k isn't a key in the table
    k = next.first();
    if (k.isNil()) break;
    LuaValue v = next.arg(2);
    // ...
}
```

The array part comes first, in order, and then the hash part in no particular order. Observed output for `{ x = 1, y = 2, 'first', 'second' }`:

```
1=first 2=second y=2 x=1
```

For array-style tables only:

```java
for (int i = 1; i <= table.length(); i++) {
    LuaValue item = table.rawget(i);
}
```

### Metatables

```java
LuaTable meta = new LuaTable();
meta.rawset(Constants.INDEX, someTableOrFunction);   // "__index"
table.setMetatable(state, meta);
table.getMetatable(state);
```

`Constants` defines every metamethod name: `INDEX`, `NEWINDEX`, `CALL`, `TOSTRING`, `NAME`, `LEN`, `EQ`, `LT`, `LE`, `ADD`, `CONCAT`, `PAIRS`, `MODE` and `METATABLE`.

## Userdata

Userdata lets Lua hold a reference to a Java object without being able to see inside it.

```java
LuaUserdata u = userdataOf(myObject, metatable);
Object back = u.userdata();
```

The metatable controls what Lua can do with the value: methods through `__index`, `tostring` through `__tostring`, and the type name in error messages through `__name`. [Java functions](03-java-functions.md#wrapping-java-objects-userdata) has a complete example.

## Converting Java collections

Cobalt doesn't convert collections for you. Two helpers cover most cases:

```java
static LuaTable toLua(List<String> list) {
    LuaTable t = new LuaTable(list.size(), 0);
    for (int i = 0; i < list.size(); i++) t.rawset(i + 1, valueOf(list.get(i)));
    return t;
}

static Map<String, LuaValue> toJava(LuaTable t) throws LuaError {
    Map<String, LuaValue> out = new LinkedHashMap<>();
    LuaValue k = Constants.NIL;
    while (true) {
        Varargs n = t.next(k);
        if ((k = n.first()).isNil()) break;
        if (k.isString()) out.put(k.toString(), n.arg(2));
    }
    return out;
}
```

Be careful with tables that contain themselves: `t.x = t` makes a naive recursive converter loop forever.
