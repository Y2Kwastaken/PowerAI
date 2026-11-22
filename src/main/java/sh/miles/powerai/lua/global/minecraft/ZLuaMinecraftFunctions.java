package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class ZLuaMinecraftFunctions {

    public static void write(LuaState state, Player player) {
        final LuaTable globals = state.globals();

        // output
        set(globals, player, "print", new Out_Print());
        set(globals, player, "actionbar", new Out_ActionBar());
        set(globals, player, "title", new Out_Title());
        set(globals, player, "sound", new Out_Sound());

        // reading
        set(globals, player, "position", new Read_Position());
        set(globals, player, "look", new Read_Look());
        set(globals, player, "block", new Read_Block());
        set(globals, player, "players", new Read_Players());

        // player actions
        set(globals, player, "teleport", new Act_Teleport());
        set(globals, player, "launch", new Act_Launch());
        set(globals, player, "give", new Act_Give());
        set(globals, player, "command", new Act_Command());

        // world actions
        set(globals, player, "setBlock", new Act_SetBlock());
        set(globals, player, "fill", new Act_Fill());
        set(globals, player, "explode", new Act_Explode());
        set(globals, player, "lightning", new Act_Lightning());
        set(globals, player, "spawn", new Act_Spawn());
        set(globals, player, "particle", new Act_Particle());
        set(globals, player, "time", new Act_Time());
        set(globals, player, "weather", new Act_Weather());
    }

    private static void set(LuaTable globals, Player player, String name, ZPlayerFunction function) {
        globals.rawset(name, function.compile(player, name));
    }
}
