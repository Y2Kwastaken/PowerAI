# Lua Functions

## System

### sleep(int n)
sleeps for n ticks (minimum 1)

permission: none

### ticks()
returns the script tick counter, the same clock `sleep` uses. only the difference between two calls means anything

returns: `ticks`

permission: none

## Minecraft

### print(string... args)
sends args to the player in chat joined by spaces, formatted with MiniMessage

permission: `powerai.lua.function.print`  
full formatting (click and hover tags): `powerai.lua.functions.print.full`

### actionbar(string... args)
shows args on the player's action bar joined by spaces, formatted with MiniMessage

permission: `powerai.lua.function.actionbar`

### title(string title, string subtitle, int fadeIn, int stay, int fadeOut)
shows a title and subtitle to the player, formatted with MiniMessage. all arguments are optional: title and subtitle default to empty, times are in ticks (default 10, 70, 10, minimum 0)

permission: `powerai.lua.function.title`

### sound(string sound, number pitch, number volume, boolean broadcast)
plays a sound to the player, e.g. `sound("entity.cow.ambient")`. pitch and volume default to 1. if broadcast is true the sound plays at the player's location so everyone nearby hears it (needs the broadcast permission, otherwise only the player hears it)

permission: `powerai.lua.function.sound`  
broadcast: `powerai.lua.function.sound.broadcast`

### position()
returns the player's position

returns: `x, y, z, yaw, pitch, world` (world without the `minecraft:` namespace, e.g. `overworld`)

permission: `powerai.lua.function.position`

### look(int distance)
finds the block the player is looking at, up to distance blocks away (default 7, clamped to 1..128)

returns: `x, y, z, type` (type without the `minecraft:` namespace, e.g. `stone`), or `nil` if no block is in range

permission: `powerai.lua.function.look`

### teleport(number x, number y, number z, number yaw, number pitch)
teleports the player in their current world. yaw and pitch are optional and default to where the player is already facing, pitch is clamped to -90..90. far away targets load in the background so the teleport may land a moment later

returns: `true` if the teleport started, `false` if the target is outside the world border

permission: `powerai.lua.function.teleport`

### launch(number vx, number vy, number vz, boolean add)
sets the player's velocity in blocks per tick, e.g. `launch(0, 3, 0)` throws you straight up. each axis is clamped to -10..10. if add is true the velocity is added to the player's current velocity instead of replacing it (default false)

permission: `powerai.lua.function.launch`

### give(string item, int count)
gives the player items, e.g. `give("diamond", 64)`. count defaults to 1 and is clamped to 1..2304 (a full inventory), anything that doesn't fit drops at the player's feet

permission: `powerai.lua.function.give`

### command(string command)
runs a command as the player, e.g. `command("gamemode creative")`. the leading `/` is optional. only works for commands the player is allowed to run, and `/scripts` can't be run from a script

returns: `true` if the command ran, `false` if it failed or doesn't exist

permission: `powerai.lua.function.command`

### players(number radius)
finds players in the same world within radius blocks of the player (default 64, clamped to 1..256), nearest first. includes the player running the script

returns: a list of `{ name, x, y, z, self }` tables, e.g. `for _, p in ipairs(players()) do if not p.self then lightning(p.x, p.y, p.z) end end`

permission: `powerai.lua.function.players`

## World

all world functions happen in the player's current world. the ones that would load a chunk return `false` (or `nil` for `block`) instead when the chunk isn't loaded, unless the player has that function's `.load` permission

### block(int x, int y, int z)
reads the block at a position

returns: the block type without the `minecraft:` namespace (e.g. `stone`, `air`), or `nil` if the chunk isn't loaded

permission: `powerai.lua.function.block`  
load chunks: `powerai.lua.function.block.load`

### setBlock(int x, int y, int z, string type)
places a block, e.g. `setBlock(x, y, z, "gold_block")`

returns: `true` if placed, `false` if y is outside the world or the chunk isn't loaded

permission: `powerai.lua.function.setblock`  
load chunks: `powerai.lua.function.setblock.load`

### fill(int x1, int y1, int z1, int x2, int y2, int z2, string type)
fills the box between two corners with a block, e.g. `fill(x - 5, y, z - 5, x + 5, y, z + 5, "glass")`. the box is clipped to the world's height and errors if it's bigger than 32768 blocks (the same limit as `/fill`). blocks are placed without physics, so sand and gravel float until something updates them

returns: the number of blocks placed, `0` if the box is outside the world or a chunk isn't loaded

permission: `powerai.lua.function.fill`  
load chunks: `powerai.lua.function.fill.load`

### explode(number x, number y, number z, number power, boolean fire, boolean breakBlocks)
creates an explosion. power defaults to 4 (tnt) and is clamped to 0..128, fire defaults to false, breakBlocks defaults to true

returns: `true` if it exploded, `false` if the chunk isn't loaded

permission: `powerai.lua.function.explode`  
load chunks: `powerai.lua.function.explode.load`

### lightning(number x, number y, number z, boolean effectOnly)
strikes lightning. if effectOnly is true it's just the visual and sound, with no damage or fire (default false)

returns: `true` if it struck, `false` if the chunk isn't loaded

permission: `powerai.lua.function.lightning`  
load chunks: `powerai.lua.function.lightning.load`

### spawn(string type, number x, number y, number z)
spawns an entity, e.g. `spawn("pig", x, y, z)`. errors for types that can't be spawned like `player`

returns: `true` if spawned, `false` if the chunk isn't loaded

permission: `powerai.lua.function.spawn`  
load chunks: `powerai.lua.function.spawn.load`

### particle(string type, number x, number y, number z, int count, number spread, number speed, data)
spawns particles visible to everyone in view distance, e.g. `particle("flame", x, y, z, 20, 0.5)`. count defaults to 1 and is clamped to 1..1000, spread is the random offset in blocks on each axis (default 0), speed defaults to 0

some particles need data, passed as the 8th argument. colors are `"#rrggbb"` or `"#aarrggbb"`, and the simple ones take a shorthand instead of a table:

| particles | data | shorthand |
|---|---|---|
| `dust` | `{ color = "#ff0000", size = 1 }` (size 0.01..4) | `"#ff0000"` |
| `dust_color_transition` | `{ from = "#ff0000", to = "#0000ff", size = 1 }` | |
| `entity_effect`, `flash`, `tinted_leaves` | `{ color = "#ff0000" }` | `"#ff0000"` |
| `effect`, `instant_effect` | `{ color = "#ff0000", power = 1 }` | `"#ff0000"` |
| `block`, `falling_dust`, `dust_pillar`, `block_crumble`, `block_marker` | `{ block = "stone" }` | `"stone"` |
| `item` | `{ item = "diamond" }` | `"diamond"` |
| `dragon_breath`, `sculk_charge` | `{ value = 0.5 }` | `0.5` |
| `shriek` | `{ value = 10 }` (delay in ticks) | `10` |
| `trail` | `{ x = 0, y = 64, z = 0, color = "#ff0000", duration = 20 }` | |
| `vibration` | `{ x = 0, y = 64, z = 0, ticks = 20 }` (flies to that block) | |
| `geyser`, `geyser_plume` | `{ water = 3 }` | `3` |
| `geyser_base`, `geyser_poof` | `{ water = 3, impulse = 1 }` | |

e.g. `particle("dust", x, y, z, 50, 1, 0, { color = "#ff8800", size = 2 })` or `particle("block", x, y, z, 30, 0.5, 0, "diamond_block")`

permission: `powerai.lua.function.particle`

### time(int t)
reads or sets the time of day in ticks (0 is sunrise, 6000 noon, 13000 night, 18000 midnight). with no argument it just reads it. values wrap around 24000

returns: the time of day after any change

permission: `powerai.lua.function.time`

### weather(string type, int ticks)
reads or sets the weather. type is `clear`, `rain` or `thunder`, ticks is how long it lasts (default 6000, 5 minutes). with no arguments it just reads it

returns: the weather after any change, `clear`, `rain` or `thunder`

permission: `powerai.lua.function.weather`

## Permissions

every function's permission is granted to everyone by default. the extras (`.load` chunk loading, `sound.broadcast`, `print.full`) default to ops only. `powerai.lua.function.*` grants everything
