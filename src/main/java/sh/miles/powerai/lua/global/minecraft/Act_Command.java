package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.entity.Player;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.ValueFactory.valueOf;

final class Act_Command implements ZPlayerFunction {
    static final String COMMAND_PERMISSION = "powerai.lua.function.command";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        String command = ZArgs.utf8(args.arg(1).checkLuaString()).strip();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            throw new LuaError("empty command");
        }

        return valueOf(player.performCommand(command));
    }

    @Override
    public String permission() {
        return COMMAND_PERMISSION;
    }
}
