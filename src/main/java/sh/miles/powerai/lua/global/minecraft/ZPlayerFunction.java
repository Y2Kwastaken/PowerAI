package sh.miles.powerai.lua.global.minecraft;

import org.bukkit.entity.Player;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LibFunction;

interface ZPlayerFunction {
    Varargs run(Player player, LuaState state, Varargs args) throws LuaError;

    default String permission() {
        return null;
    }

    default LibFunction compile(Player player, String name) {
        return LibFunction.createV((state, args) -> {
            String permission = permission();
            if (permission != null && !player.hasPermission(permission)) {
                return new NoPermissionFunction(name).run(player, state, args);
            }

            return run(player, state, args);
        });
    }

    final class NoPermissionFunction implements ZPlayerFunction {

        private final String name;

        public NoPermissionFunction(String name) {
            this.name = name;
        }

        @Override
        public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
            player.sendRichMessage("<red>You do not have permission to run the function %s".formatted(name));
            return Constants.NONE;
        }
    }
}
