package sh.miles.powerai.command.v2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.dialog.v2.ScriptOverviewDialog;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

final class SubView {
    private static final String NAME = "view";
    private static final String PERMISSION = "powerai.command.scripts.view";
    private static final String ARGUMENT_PLAYER = "player";

    LiteralCommandNode<CommandSourceStack> build(MarketplaceStore store) {
        return literal(NAME)
                .requires((s) -> Cutie.require(s, PERMISSION))
                .executes((ctx) -> openScriptOverview(ctx, store))
                .then(
                        argument(ARGUMENT_PLAYER, ArgumentTypes.player())
                                .executes((ctx) -> openScriptOverviewOther(ctx, store))
                )
                .build();
    }

    private int openScriptOverview(CommandContext<CommandSourceStack> source, MarketplaceStore store) throws CommandSyntaxException {
        ScriptOverviewDialog.open(
                store,
                source.getSource().getPlayerOrThrow()
        );
        return Command.SINGLE_SUCCESS;
    }

    private int openScriptOverviewOther(CommandContext<CommandSourceStack> source, MarketplaceStore store) throws CommandSyntaxException {
        final Player target = source.getArgument(ARGUMENT_PLAYER, PlayerSelectorArgumentResolver.class)
                .resolve(source.getSource())
                .getFirst();

        ScriptOverviewDialog.open(
                store,
                source.getSource().getPlayerOrThrow(),
                target
        );
        return Command.SINGLE_SUCCESS;
    }


}
