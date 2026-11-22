package sh.miles.powerai.lua.global.minecraft;

import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import java.util.ArrayList;
import java.util.List;

final class Act_Give implements ZPlayerFunction {
    static final String GIVE_PERMISSION = "powerai.lua.function.give";

    static final int MAX_COUNT = 36 * 64;

    @Override
    public Varargs run(final Player player, final LuaState state, final Varargs args) throws LuaError {
        final ItemType item = ZArgs.registry(RegistryKey.ITEM, args.arg(1));
        int remaining = Math.clamp(args.arg(2).optInteger(1), 1, MAX_COUNT);

        final int maxStack = item.getMaxStackSize();
        final List<ItemStack> stacks = new ArrayList<>();
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            stacks.add(item.createItemStack(amount));
            remaining -= amount;
        }

        player.give(stacks, true);
        return Constants.NONE;
    }

    @Override
    public String permission() {
        return GIVE_PERMISSION;
    }
}
