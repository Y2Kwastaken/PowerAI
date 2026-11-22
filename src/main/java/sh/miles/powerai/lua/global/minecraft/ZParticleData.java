package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

/**
 * Turns the Lua data argument of {@code particle(...)} into whatever {@link Particle#getDataType()} wants. Every type
 * takes a table, and the simple ones also take a shorthand value:
 * <pre>
 * Color         "#ff0000"           or { color = "#ff0000" }          entity_effect, flash, tinted_leaves
 * DustOptions   "#ff0000"           or { color = "#ff0000", size = 2 } dust
 * DustTransition                       { from = "#ff0000", to = "#0000ff", size = 2 }
 * Spell         "#ff0000"           or { color = "#ff0000", power = 1 } effect, instant_effect
 * BlockData     "stone"             or { block = "stone" }            block, falling_dust, dust_pillar, block_crumble, block_marker
 * ItemStack     "diamond"           or { item = "diamond" }           item
 * Float         0.5                 or { value = 0.5 }                dragon_breath, sculk_charge
 * Integer       10                  or { value = 10 }                 shriek (delay in ticks)
 * Trail                                { x, y, z, color = "#ff0000", duration = 20 }
 * Vibration                            { x, y, z, ticks = 20 }        flies to the block at x, y, z
 * Geyser        3                   or { water = 3 }                  geyser, geyser_plume
 * GeyserBase                           { water = 3, impulse = 1 }      geyser_base, geyser_poof
 * </pre>
 * Colors are {@code "#rrggbb"} or {@code "#aarrggbb"}.
 * @author Claude Code
 */
final class ZParticleData {
    static final float MAX_SIZE = 4f; // vanilla clamps dust size to 0.01..4 anyway

    private ZParticleData() {
    }

    /**
     * @param origin where the particle spawns, used as the world for trail and vibration targets
     * @return the data object, or null for particles that take none
     */
    static Object read(final Particle particle, final LuaValue raw, final Location origin) throws LuaError {
        final Class<?> type = particle.getDataType();
        if (type == Void.class) return null;
        if (raw.isNil()) {
            throw new LuaError("particle " + particle.key().asMinimalString() + " needs data, see LUA.md");
        }

        if (type == Color.class) return color(field(raw, "color"));
        if (type == Particle.DustTransition.class) { // check before DustOptions, it's a subclass
            LuaTable t = table(raw, particle);
            return new Particle.DustTransition(color(t.rawget("from")), color(t.rawget("to")), size(t));
        }
        if (type == Particle.DustOptions.class) {
            return raw instanceof LuaTable t
                    ? new Particle.DustOptions(color(t.rawget("color")), size(t))
                    : new Particle.DustOptions(color(raw), 1f);
        }
        if (type == Particle.Spell.class) {
            return raw instanceof LuaTable t
                    ? new Particle.Spell(color(t.rawget("color")), (float) t.rawget("power").optDouble(1))
                    : new Particle.Spell(color(raw), 1f);
        }
        if (type == BlockData.class) {
            BlockType block = ZArgs.registry(RegistryKey.BLOCK, field(raw, "block"));
            return block.createBlockData();
        }
        if (type == ItemStack.class) {
            ItemType item = ZArgs.registry(RegistryKey.ITEM, field(raw, "item"));
            return item.createItemStack();
        }
        if (type == Float.class) return (float) field(raw, "value").checkDouble();
        if (type == Integer.class) return field(raw, "value").checkInteger();
        if (type == Particle.Trail.class) {
            LuaTable t = table(raw, particle);
            int duration = Math.max(1, t.rawget("duration").optInteger(20));
            return new Particle.Trail(target(t, origin), color(t.rawget("color")), duration);
        }
        if (type == Vibration.class) {
            LuaTable t = table(raw, particle);
            int ticks = Math.max(1, t.rawget("ticks").optInteger(20));
            return new Vibration(new Vibration.Destination.BlockDestination(target(t, origin)), ticks);
        }
        if (type == Particle.Geyser.class) return new Particle.Geyser(water(field(raw, "water")));
        if (type == Particle.GeyserBase.class) {
            LuaTable t = table(raw, particle);
            return new Particle.GeyserBase(water(t.rawget("water")), (float) t.rawget("impulse").optDouble(1));
        }

        throw new LuaError("particle " + particle.key().asMinimalString() + " uses " + type.getSimpleName() + " data, which isn't supported");
    }

    /**
     * Parses {@code "#rrggbb"} or {@code "#aarrggbb"}
     */
    static Color color(final LuaValue value) throws LuaError {
        String hex = ZArgs.utf8(value.checkLuaString());
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (digits.length() == 6) return Color.fromRGB(Integer.parseUnsignedInt(digits, 16));
            if (digits.length() == 8) return Color.fromARGB(Integer.parseUnsignedInt(digits, 16));
        } catch (NumberFormatException ignored) {
        }
        throw new LuaError("bad color '" + hex + "', expected \"#rrggbb\" or \"#aarrggbb\"");
    }

    /**
     * For data that has a shorthand: {@code { key = v }} gives {@code v}, anything else is the value itself
     */
    private static LuaValue field(final LuaValue raw, final String key) {
        return raw instanceof LuaTable t ? t.rawget(key) : raw;
    }

    private static LuaTable table(final LuaValue raw, final Particle particle) throws LuaError {
        if (raw instanceof LuaTable t) return t;
        throw new LuaError("particle " + particle.key().asMinimalString() + " needs a table of data, got " + raw.typeName());
    }

    private static float size(final LuaTable t) throws LuaError {
        return (float) Math.clamp(t.rawget("size").optDouble(1), 0.01, MAX_SIZE);
    }

    private static int water(final LuaValue value) throws LuaError {
        return Math.max(1, value.optInteger(1));
    }

    private static Location target(final LuaTable t, final Location origin) throws LuaError {
        double x = t.rawget("x").checkDouble();
        double y = t.rawget("y").checkDouble();
        double z = t.rawget("z").checkDouble();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new LuaError("target coordinates must be finite numbers");
        }
        return new Location(origin.getWorld(), x, y, z);
    }
}
