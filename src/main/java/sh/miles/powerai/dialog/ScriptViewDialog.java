package sh.miles.powerai.dialog;

import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.input.TextDialogField;
import sh.miles.dialogkit.shape.BinaryDialogShape;
import sh.miles.powerai.util.PlayerStorage;

import java.util.Locale;

import static net.kyori.adventure.text.Component.text;

public class ScriptViewDialog extends DialogScreen {

    private static final String SCRIPT_NAME = "script_name";
    private static final String SCRIPT_CONTENT = "script_content";

    public static final int MAX_SCRIPT_LENGTH = 8192;

    public ScriptViewDialog() {
        this(null, null, null, null);
    }

    public ScriptViewDialog(String name, String content) {
        this(name, name, content, null);
    }


    private ScriptViewDialog(String originalName, String name, String content, Component error) {
        super.setTitle(text(originalName == null ? "New Script" : "Edit Script: " + originalName));
        if (error != null) {
            super.addText(error);
        }

        BinaryDialogShape shape = new BinaryDialogShape();
        shape.setOptionOne(new SaveElement(originalName));
        shape.setOptionTwo(new CancelElement());
        super.setShape(shape);

        TextDialogField scriptName = new TextDialogField();
        scriptName.setInitial(name);
        scriptName.setKey(SCRIPT_NAME);
        scriptName.setMaxLength(PlayerStorage.MAX_SCRIPT_NAME_LENGTH);
        scriptName.setLabel(text("Script Name"));
        super.addInput(scriptName);

        TextDialogField scriptContent = new TextDialogField();
        scriptContent.setInitial(content);
        scriptContent.setKey(SCRIPT_CONTENT);
        scriptContent.setMultiline(true);
        scriptContent.setMaxLength(MAX_SCRIPT_LENGTH);
        scriptContent.setHeight(512);
        scriptContent.setWidth(1024);
        scriptContent.setLabel(text("Script content"));
        super.addInput(scriptContent);

        super.setCloseWithEscape(false);
        super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
    }

    private static class SaveElement extends CustomElement {
        private final String originalName;

        private SaveElement(String originalName) {
            this.originalName = originalName;
            setText(text("Save Script"));
            setTooltip(text("Saves the current player script"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            String rawName = response.getText(SCRIPT_NAME);
            String scriptContent = response.getText(SCRIPT_CONTENT);
            String scriptName = rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);

            String error = null;
            if (!PlayerStorage.isValidScriptName(scriptName)) {
                error = "Script names must be 1-" + PlayerStorage.MAX_SCRIPT_NAME_LENGTH + " characters of a-z, 0-9, '.', '_' or '-'";
            } else if (scriptContent == null || scriptContent.isBlank()) {
                error = "Script content must not be empty";
            } else if (!scriptName.equals(originalName) && PlayerStorage.hasScript(player, scriptName)) {
                error = "A script named '" + scriptName + "' already exists";
            }

            if (error != null) {
                // reopen with what the player typed so their work isn't lost
                player.showDialog(new ScriptViewDialog(originalName, rawName, scriptContent, text(error, NamedTextColor.RED)).compile());
                return;
            }

            if (!PlayerStorage.saveScript(player, scriptName, scriptContent)) {
                player.showDialog(new ScriptViewDialog(originalName, rawName, scriptContent, text("Failed to save the script", NamedTextColor.RED)).compile());
                return;
            }

            // renamed while editing, so remove the script under its old name
            if (originalName != null && !originalName.equals(scriptName)) {
                PlayerStorage.deleteScript(player, originalName);
            }

            ScriptManagerDialog.open(player);
        }
    }

    private static class CancelElement extends CustomElement {
        private CancelElement() {
            setText(text("Cancel"));
            setTooltip(text("Leave without saving the script"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            ScriptManagerDialog.open(player);
        }
    }
}
