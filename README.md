# PowerAI

### The world's first AI-native, agentic security platform for Minecraft servers.

**🤖 AI-Powered · 🛡️ Zero-Trust · 🧠 LLM-Native · ⚡ Real-Time Neural Threat Mitigation**

Server security is broken. Legacy solutions like "permissions" and "not giving random players code execution" simply
can't keep up with the modern threat landscape. PowerAI is a paradigm shift: a next-generation, AI-first security
platform that leverages cutting-edge agentic workflows to autonomously protect your server, so you can focus on what
really matters: growing your community.

## Why PowerAI?

- **Agentic by design.** Every player becomes an autonomous AI-powered agent, empowered to take action in your world
  with zero humans in the loop.
- **Zero-Trust architecture.** PowerAI places zero trust in server owners to make decisions about what players can do.
- **Generative world engine.** Our proprietary generative pipeline can synthesize explosions, lightning and entire
  craters on demand, in real time.
- **Hallucination-free.** Our model has a 0% hallucination rate, guaranteed.
- **Frictionless onboarding.** No setup, no config, no moderation queue. Players go from idea to impact in seconds.
- **Scales with your community.** The more players you have, the more agents you have. It's that simple.

## How does it work?

PowerAI rethinks server security from first principles.

Traditional security asks: *how do we stop players from doing harmful things?*
PowerAI asks a better question: *what if we didn't?*

PowerAI uses collectivity and shared computation to democratize access to your server's most valuable resource: the
server itself. Our community-driven, decentralized execution model turns every player into a stakeholder with
full-stack ownership of your world.

### Permissionless by default

Legacy permission systems create friction, and friction kills engagement. Players often try to gain unauthorized access
to servers because they feel underserved by their current access tier. PowerAI eliminates this pain point at the source
by proactively granting every player in-game code execution. No exploits, no social engineering, no out-of-game
shenanigans. Just a seamless, end-to-end pipeline from player intent to measurable world impact.

### Human-out-of-the-loop moderation

Manual review is a bottleneck that doesn't scale. PowerAI's fully autonomous workflow runs unapproved, unauthenticated
and unmoderated Lua scripts with zero oversight from server owners, cutting your moderation overhead by 100%. Finally
you can kick back and relax and not worry about security. PowerAI has it handled for you!

### Intelligent compute orchestration

Our tick-aware scheduling layer dynamically allocates millisecond-level compute budgets across every active agent,
pausing workloads that exceed their slice and gracefully sunsetting the ones that refuse to yield. Your server keeps
running at full TPS right up until a player asks it to explode.

### State-of-the-art AI safety

PowerAI security was powered by a top of the line AI model which determined whether your script was safe.
Unfortunately our team doesn't have the compute to run it, so we decided to remove it. This is also how we achieved our
industry-leading 0% hallucination rate.

---

PowerAI stands for **P**layer **O**perated **W**orld **E**nding **R**untime **A**utomated **I**nterpreter. There is no AI.


> [!WARNING]
> This project could destroy your entire server if not setup carefully in production. Granted I woiuld question your sanity
> to run this in production to begin with.

## Functionality

Your beautiful players can write Lua scripts in an in-game editor and run them! Scripts
allow communication with players, reading the world, and changing the world. You can place blocks, fill areas,
spawn mobs, strike lightning, launch people and set of explosions, and probably some more stuff if I add it.

Scripts are run on the main thread in small time slice, so scripts attempting to freeze or harm
a server gets paused and possibly killed rather than bringing your server down. `sleep(ticks)` lets a script wait
without burning its own time budget as well.

```lua
-- strike lightning on everyone near you, then leave
for _, p in ipairs(players(32)) do
  if not p.self then
    lightning(p.x, p.y, p.z)
  end
end
launch(0, 4, 0)
title("<red>bye</red>")
```

## Commands

| Command | What it does |
|---|---|
| `/scripts` | Open the script manager to create, edit and delete scripts |
| `/scripts run <name>` | Run one of your scripts (`/scripts <name>` works too) |
| `/scripts list` | List your scripts, click one to run it |
| `/scripts stop` | Stop all of your running scripts |

Scripts are stored per player in their persistent data, so they follow the player around and need no database.

## Lua functions

The full list with arguments, return values and permissions is in [LUA.md](LUA.md). A short possibly outdated list is here:

- **System:** `sleep`, `ticks`
- **Player:** `print`, `actionbar`, `title`, `sound`, `position`, `look`, `teleport`, `launch`, `give`, `command`, `players`
- **World:** `block`, `setBlock`, `fill`, `explode`, `lightning`, `spawn`, `particle`, `time`, `weather`

On top of that scripts get Lua's standard library.

## Permissions

Every function has a permission (`powerai.lua.function.<name>`) and they're all granted to everyone by default, in the
spirit of unfettered chaos. Extras like loading have other permissions ending with (`.load`) or some other thing, 
sounds everyone hears (`sound.broadcast`) and clickable chat in `print` (`print.full`).
`powerai.lua.function.*` grants all permissions. See [LUA.md](LUA.md) for the full list.

## Configuration

`plugins/PowerAI/config.yml`, all values in milliseconds:

```yaml
runtime_per_tick: 10          # time all scripts together get per tick (a tick is 50ms)
script_allowance_per_tick: 2  # time one script gets per tick before it's paused
total_script_allowance: 1000  # time one script gets over its whole life before it's killed
```

Sleeping doesn't count against the budget, only time actually spent running Lua. Big scripts that blow things up spend
most of their time inside Minecraft's explosion code, so raise `total_script_allowance` if they get killed partway.

## Building

Needs Java 25. Targets Paper 26.3.

```sh
./gradlew build       # plugin jar in build/libs, with Cobalt shaded in
./gradlew runServer   # starts a test server with the plugin installed
```

## Credits

Powered by [Cobalt](https://codeberg.org/cc-tweaked/Cobalt), a Lua 5.2 runtime for Java from the CC: Tweaked peoples,
while ignoring readme messages like "Don't. No seriously, don't [use] Cobalt". This kind message was ignored I appreciate
your caution, but I frankly don't care, God bless!

## License

PowerAI is licensed under the [GNU General Public License v3.0](LICENSE).
