package sh.miles.powerai.command.v2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.dialog.v2.ScriptOverviewDialog;

public class ScriptsCommandV2 {

    private static final String NAME = "v2scripts";
    private static final String PERMISSION = "powerai.command.scripts";

    public LiteralCommandNode<CommandSourceStack> build(MarketplaceStore store) {
        return Commands.literal(NAME)
                .requires((s) -> Cutie.require(s, PERMISSION))
                .executes((ctx) -> openScriptOverview(ctx, store))
                .then(new SubEditor().build(store))
                .then(new SubView().build(store))
                .then(new SubStore().build(store))
                .build();
    }

    private int openScriptOverview(CommandContext<CommandSourceStack> source, MarketplaceStore store) throws CommandSyntaxException {
        final Player player = source.getSource().getPlayerOrThrow();
        ScriptOverviewDialog.open(store, player);
        return Command.SINGLE_SUCCESS;
    }
}
