package sh.miles.powerai.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import sh.miles.powerai.dialog.ScriptManagerDialog;
import sh.miles.powerai.lua.LuaScriptExecutor;
import sh.miles.powerai.lua.global.LuaGlobals;
import sh.miles.powerai.util.PlayerStorage;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.text;

public final class ScriptsCommand {

    private static final DynamicCommandExceptionType UNKNOWN_SCRIPT = new DynamicCommandExceptionType(
            name -> MessageComponentSerializer.message().serialize(text("You don't have a script named '" + name + "'"))
    );
    private static final SimpleCommandExceptionType NESTED_SCRIPT = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(text("Scripts can't start other scripts"))
    );
    private static final SimpleCommandExceptionType NOT_STARTED = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(text("The script didn't start, check the error above"))
    );

    private final LuaScriptExecutor executor;

    public ScriptsCommand(final LuaScriptExecutor executor) {
        this.executor = executor;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("scripts")
                .requires(source -> source.getSender() instanceof Player)
                .executes(this::openMenu)
                .then(Commands.literal("run")
                        .then(scriptArgument()))
                .then(Commands.literal("list")
                        .executes(this::list))
                .then(Commands.literal("stop")
                        .executes(this::stop))
                // shorthand /scripts <name>, deliberately without suggestions so tab completion only shows subcommands
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::run))
                .build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> scriptArgument() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests(this::suggestScripts)
                .executes(this::run);
    }

    private int openMenu(final CommandContext<CommandSourceStack> context) {
        ScriptManagerDialog.open(player(context));
        return Command.SINGLE_SUCCESS;
    }

    private int run(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final Player player = player(context);
        final String name = StringArgumentType.getString(context, "name").toLowerCase(Locale.ROOT);
        final String content = PlayerStorage.getScript(player, name);
        if (content == null) {
            throw UNKNOWN_SCRIPT.create(name);
        }
        if (executor.isRunningScript()) {
            throw NESTED_SCRIPT.create();
        }

        final boolean started = executor.addScriptFromString(name, player.getUniqueId(), content,
                state -> LuaGlobals.minecraft(state, player));
        if (!started) {
            throw NOT_STARTED.create();
        }

        context.getSource().sendSuccess(text("Running " + name + ", ", NamedTextColor.GRAY)
                .append(text("[stop]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/scripts stop"))
                        .hoverEvent(HoverEvent.showText(text("Stop all of your scripts")))), false);
        return Command.SINGLE_SUCCESS;
    }

    private int list(final CommandContext<CommandSourceStack> context) {
        final List<String> scripts = PlayerStorage.getScripts(player(context));
        if (scripts.isEmpty()) {
            context.getSource().sendSuccess(text("You don't have any scripts yet, use /scripts to make one", NamedTextColor.GRAY), false);
            return 0;
        }

        Component line = text("Your scripts: ", NamedTextColor.GRAY);
        for (int i = 0; i < scripts.size(); i++) {
            final String name = scripts.get(i);
            if (i > 0) line = line.append(text(", ", NamedTextColor.GRAY));
            line = line.append(text(name, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/scripts run " + name))
                    .hoverEvent(HoverEvent.showText(text("Click to run " + name))));
        }
        context.getSource().sendSuccess(line, false);
        return scripts.size();
    }

    private int stop(final CommandContext<CommandSourceStack> context) {
        final int stopped = executor.cancelOwnedBy(player(context).getUniqueId());
        if (stopped == 0) {
            context.getSource().sendSuccess(text("You don't have any scripts running", NamedTextColor.GRAY), false);
        } else {
            context.getSource().sendSuccess(text("Stopped " + stopped + (stopped == 1 ? " script" : " scripts"), NamedTextColor.GRAY), false);
        }
        return stopped;
    }

    private CompletableFuture<Suggestions> suggestScripts(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        if (context.getSource().getSender() instanceof Player player) {
            final String prefix = builder.getRemainingLowerCase();
            for (String name : PlayerStorage.getScripts(player)) {
                if (name.startsWith(prefix)) builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static Player player(final CommandContext<CommandSourceStack> context) {
        return (Player) context.getSource().getSender();
    }
}
