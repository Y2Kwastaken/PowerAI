package sh.miles.powerai.lua.global.minecraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Varargs;

final class Out_Title implements ZPlayerFunction {
    static final String TITLE_PERMISSION = "powerai.lua.function.title";

    static final MiniMessage LIMITED = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE);

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final LuaString title = args.arg(1).optLuaString(Constants.EMPTYSTRING);
        final LuaString subTitle = args.arg(2).optLuaString(Constants.EMPTYSTRING);
        final int fadeIn = Math.max(0, args.arg(3).optInteger(10));
        final int stay = Math.max(0, args.arg(4).optInteger(70));
        final int fadeOut = Math.max(0, args.arg(5).optInteger(10));

        Component ctitle = LIMITED.deserialize(ZArgs.utf8(title));
        Component stitle = LIMITED.deserialize(ZArgs.utf8(subTitle));

        Title titleParts = Title.title(
                ctitle,
                stitle,
                Title.Times.times(Ticks.duration(fadeIn),
                        Ticks.duration(stay),
                        Ticks.duration(fadeOut))
        );
        player.showTitle(titleParts);
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return TITLE_PERMISSION;
    }
}
