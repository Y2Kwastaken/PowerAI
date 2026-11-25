package sh.miles.powerai.command.v2;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import sh.miles.powerai.util.PlayerStorage;

import java.util.concurrent.CompletableFuture;

final class Cutie {
    static boolean require(CommandSourceStack source, String permission) {
        return source.getSender() instanceof Player player && player.hasPermission(permission);
    }

    static CompletableFuture<Suggestions> scriptSuggest(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        if (context.getSource().getSender() instanceof Player player) {
            final String remaining = builder.getRemainingLowerCase();
            for (final String script : PlayerStorage.getScripts(player)) {
                if (script.startsWith(remaining)) {
                    builder.suggest(script);
                }
            }
        }
        return builder.buildFuture();
    }
}
