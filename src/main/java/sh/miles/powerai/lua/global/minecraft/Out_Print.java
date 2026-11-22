package sh.miles.powerai.lua.global.minecraft;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Out_Print implements ZPlayerFunction {
    static final String MESSAGE_PERMISSION = "powerai.lua.function.print";
    static final String MESSAGE_PERMISSION_FULL = "powerai.lua.functions.print.full";

    static final MiniMessage FULL = MiniMessage.miniMessage();
    static final MiniMessage LIMITED = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE);

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        StringBuilder joiner = ZArgs.stringJoiner(args);
        if (joiner.isEmpty()) {
            return Constants.NONE;
        }

        MiniMessage formatter = player.hasPermission(MESSAGE_PERMISSION_FULL) ? FULL : LIMITED;
        player.sendMessage(formatter.deserialize(joiner.toString()));
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return MESSAGE_PERMISSION;
    }
}
