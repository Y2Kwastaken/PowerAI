package sh.miles.powerai.lua.global.minecraft;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

final class Out_ActionBar implements ZPlayerFunction {
    static final String ACTIONBAR_PERMISSION = "powerai.lua.function.actionbar";

    static final MiniMessage LIMITED = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE);

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        StringBuilder joiner = ZArgs.stringJoiner(args);
        if (joiner.isEmpty()) {
            return Constants.NONE;
        }

        player.sendActionBar(LIMITED.deserialize(joiner.toString()));
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return ACTIONBAR_PERMISSION;
    }
}
