package sh.miles.powerai.command.v2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import org.bukkit.entity.Player;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.dialog.v2.ScriptEditorDialog;
import sh.miles.powerai.util.PlayerStorage;

import java.util.Locale;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;

final class SubEditor {

    private static final String NAME = "editor";
    private static final String PERMISSION = "powerai.command.scripts.editor";
    private static final String ARGUMENT_SCRIPT = "script";

    private static final DynamicCommandExceptionType UNKNOWN_SCRIPT = new DynamicCommandExceptionType(
            name -> MessageComponentSerializer.message().serialize(text("You don't have a script named '" + name + "'"))
    );


    LiteralCommandNode<CommandSourceStack> build(MarketplaceStore store) {
        return literal(NAME)
                .requires((s) -> Cutie.require(s, PERMISSION))
                .executes((ctx) -> openEditor(ctx, store))
                .then(
                        argument(ARGUMENT_SCRIPT, StringArgumentType.word())
                                .suggests(Cutie::scriptSuggest)
                                .executes((ctx) -> openEditorFromScript(ctx, store))
                )
                .build();
    }

    private int openEditor(final CommandContext<CommandSourceStack> context, final MarketplaceStore store) throws CommandSyntaxException {
        ScriptEditorDialog.open(store, context.getSource().getPlayerOrThrow());
        return Command.SINGLE_SUCCESS;
    }

    private int openEditorFromScript(final CommandContext<CommandSourceStack> context, final MarketplaceStore store) throws CommandSyntaxException {
        final Player player = context.getSource().getPlayerOrThrow();
        final String name = StringArgumentType.getString(context, "script").toLowerCase(Locale.ROOT);
        final String content = PlayerStorage.getScript(player, name);
        if (content == null) {
            throw UNKNOWN_SCRIPT.create(name);
        }

        ScriptEditorDialog.open(store, player, name, content);
        return Command.SINGLE_SUCCESS;
    }
}
