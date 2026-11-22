package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

public class Out_Sound implements ZPlayerFunction {
    static final String SOUND_PERMISSION = "powerai.lua.function.sound";
    static final String SOUND_IN_WORLD_PERMISSION = "powerai.lua.function.sound.broadcast";

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        Sound sound = ZArgs.registry(RegistryKey.SOUND_EVENT, args.arg(1));
        float pitch = (float) args.arg(2).optDouble(1.0f);
        float volume = (float) args.arg(3).optDouble(1.0F);
        boolean broadcast = args.arg(4).optBoolean(false);
        if (!broadcast || !player.hasPermission(SOUND_IN_WORLD_PERMISSION)) {
            player.playSound(player, sound, volume, pitch);
        } else {
            player.getLocation().getWorld().playSound(player.getLocation(), sound, volume, pitch);
        }
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return SOUND_PERMISSION;
    }
}
