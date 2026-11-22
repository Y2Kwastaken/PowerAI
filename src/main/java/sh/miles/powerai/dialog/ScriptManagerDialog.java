package sh.miles.powerai.dialog;

import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.BinaryDialogShape;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.powerai.util.PlayerStorage;

import java.util.List;

import static net.kyori.adventure.text.Component.text;

/**
 * Lists a player's scripts and lets them create, edit and delete them.
 * <p>
 * Flow: manager -> script options (edit / delete / back) -> {@link ScriptViewDialog} or delete confirmation -> manager
 */
public class ScriptManagerDialog extends DialogScreen {

    private static final int PREVIEW_LINES = 8;
    private static final int PREVIEW_LINE_LENGTH = 80;

    public ScriptManagerDialog(Player player) {
        super.setTitle(text("Script Manager"));

        List<String> scripts = PlayerStorage.getScripts(player);
        if (scripts.isEmpty()) {
            super.addText(text("You don't have any scripts yet. Create one to get started."));
        } else {
            super.addText(text("You have " + scripts.size() + (scripts.size() == 1 ? " script" : " scripts") + ". Select one to edit or delete it."));
        }

        MultiDialogShape shape = new MultiDialogShape();
        shape.addAction(new NewScriptElement());
        for (String script : scripts) {
            shape.addAction(new OpenScriptElement(script));
        }
        shape.setExitAction(new CloseElement());
        shape.setColumns(2);
        super.setShape(shape);

        super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
    }

    public static void open(Player player) {
        player.showDialog(new ScriptManagerDialog(player).compile());
    }

    /**
     * Shows a single script's options, or the manager if the script no longer exists
     */
    private static void openScript(Player player, String scriptName) {
        String content = PlayerStorage.getScript(player, scriptName);
        if (content == null) {
            open(player);
            return;
        }

        player.showDialog(new ScriptOptionsDialog(scriptName, content).compile());
    }

    private static String preview(String content) {
        String[] lines = content.split("\\R", -1);
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, PREVIEW_LINES); i++) {
            if (i > 0) preview.append('\n');
            String line = lines[i];
            preview.append(line.length() > PREVIEW_LINE_LENGTH ? line.substring(0, PREVIEW_LINE_LENGTH) + "..." : line);
        }
        if (lines.length > PREVIEW_LINES) {
            preview.append("\n... (").append(lines.length - PREVIEW_LINES).append(" more lines)");
        }
        return preview.toString();
    }

    private static class NewScriptElement extends CustomElement {
        private NewScriptElement() {
            setText(text("+ New Script", NamedTextColor.GREEN));
            setTooltip(text("Write a new script"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            player.showDialog(new ScriptViewDialog().compile());
        }
    }

    private static class OpenScriptElement extends CustomElement {
        private final String scriptName;

        private OpenScriptElement(String scriptName) {
            this.scriptName = scriptName;
            setText(text(scriptName));
            setTooltip(text("Edit or delete " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openScript(assertPlayer(audience), scriptName);
        }
    }

    private static class CloseElement extends CustomElement {
        private CloseElement() {
            setText(text("Close"));
        }
    }

    /**
     * Options for a single script: edit, delete or go back to the manager
     */
    private static class ScriptOptionsDialog extends DialogScreen {
        private ScriptOptionsDialog(String scriptName, String content) {
            super.setTitle(text("Script: " + scriptName));
            super.addText(text(preview(content), NamedTextColor.GRAY), 400);

            MultiDialogShape shape = new MultiDialogShape();
            shape.addActions(new EditScriptElement(scriptName), new DeleteScriptElement(scriptName));
            shape.setExitAction(new BackElement());
            shape.setColumns(2);
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static class EditScriptElement extends CustomElement {
        private final String scriptName;

        private EditScriptElement(String scriptName) {
            this.scriptName = scriptName;
            setText(text("Edit"));
            setTooltip(text("Edit or rename " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                open(player);
                return;
            }

            player.showDialog(new ScriptViewDialog(scriptName, content).compile());
        }
    }

    private static class DeleteScriptElement extends CustomElement {
        private final String scriptName;

        private DeleteScriptElement(String scriptName) {
            this.scriptName = scriptName;
            setText(text("Delete", NamedTextColor.RED));
            setTooltip(text("Delete " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            if (!PlayerStorage.hasScript(player, scriptName)) {
                open(player);
                return;
            }

            player.showDialog(new DeleteConfirmDialog(scriptName).compile());
        }
    }

    private static class BackElement extends CustomElement {
        private BackElement() {
            setText(text("Back"));
            setTooltip(text("Return to the script manager"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            open(assertPlayer(audience));
        }
    }

    private static class DeleteConfirmDialog extends DialogScreen {
        private DeleteConfirmDialog(String scriptName) {
            super.setTitle(text("Delete Script"));
            super.addText(text("Are you sure you want to delete '" + scriptName + "'? This can't be undone."));

            BinaryDialogShape shape = new BinaryDialogShape();
            shape.setOptionOne(new ConfirmDeleteElement(scriptName));
            shape.setOptionTwo(new CancelDeleteElement(scriptName));
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static class ConfirmDeleteElement extends CustomElement {
        private final String scriptName;

        private ConfirmDeleteElement(String scriptName) {
            this.scriptName = scriptName;
            setText(text("Delete", NamedTextColor.RED));
            setTooltip(text("Permanently delete " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            PlayerStorage.deleteScript(player, scriptName);
            open(player);
        }
    }

    private static class CancelDeleteElement extends CustomElement {
        private final String scriptName;

        private CancelDeleteElement(String scriptName) {
            this.scriptName = scriptName;
            setText(text("Cancel"));
            setTooltip(text("Keep " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openScript(assertPlayer(audience), scriptName);
        }
    }
}
