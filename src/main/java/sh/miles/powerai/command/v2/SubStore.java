package sh.miles.powerai.command.v2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.dialog.v2.ScriptEditorOverviewDialog;
import sh.miles.powerai.dialog.v2.ScriptStoreDialog;

import static io.papermc.paper.command.brigadier.Commands.literal;

final class SubStore {

    private static final String NAME = "store";
    private static final String PERMISSION = ScriptEditorOverviewDialog.STORE_PERMISSION;


    LiteralCommandNode<CommandSourceStack> build(MarketplaceStore store) {
        return literal(NAME)
                .requires((s) -> Cutie.require(s, PERMISSION))
                .executes((ctx) -> openStore(ctx, store))
                .build();
    }

    private int openStore(CommandContext<CommandSourceStack> context, MarketplaceStore store) throws CommandSyntaxException {
        final Player player = context.getSource().getPlayerOrThrow();
        ScriptStoreDialog.open(store, player, 0);
        return Command.SINGLE_SUCCESS;
    }


}
