package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.input.TextDialogField;
import sh.miles.dialogkit.shape.BinaryDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.util.PlayerStorage;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.NONE;
import static sh.miles.powerai.dialog.v2.Cutiepie.component;

public final class ScriptEditorDialog extends DialogScreen {

    public static final int MAX_SCRIPT_LENGTH = 8192;

    private static final String SCRIPT_NAME_EMPTY = "<red>script name must not be empty";
    private static final String SCRIPT_NAME_INVALID = "<red>Script names must be 1-" + PlayerStorage.MAX_SCRIPT_NAME_LENGTH + " characters of a-z, 0-9, '.', '_' or '-'";
    private static final String SCRIPT_CONTENT_EMPTY = "<red>script content must not be empty";

    private static final String TITLE = "Script Editor";
    private static final String INPUT_NAME_LABEL = "Script Name";
    private static final String INPUT_CONTENT_LABEL = "Script Content";
    private static final String SAVE_ACTION = "<green>Save";
    private static final String SAVE_TOOLTIP = "saves the script in its current state";
    private static final String CANCEL_ACTION = "<red>Cancel";
    private static final String CANCEL_TOOLTIP = "cancels the edit of this script without saving";
    private static final String LEAVE_EARLY_WARNING = "Are you sure you want to cancel editing without saving? You will lose all progress.";
    private static final String UNPUBLISHED_RENAME = "<gray><old_name> was removed from the script store because it was renamed, publish <script_name> to share it again";

    private static final String INPUT_NAME = "script_name";
    private static final String INPUT_CONTENT = "script_content";

    public static void open(MarketplaceStore store, Player player) {
        open(store, player, null, null, null, null);
    }

    public static void open(MarketplaceStore store, Player player, String name, String content) {
        open(store, player, name, name, content, null);
    }

    public static void open(MarketplaceStore store, Player player, String name, String content, Component exception) {
        open(store, player, name, name, content, exception);
    }

    public static void open(MarketplaceStore store, Player player, String name, String rawName, String content, Component exception) {
        final ScriptEditorDialog dialog = new ScriptEditorDialog(store, name, rawName, content, exception);
        player.showDialog(dialog.compile());
    }

    private final MarketplaceStore store;
    private final String defaultName;

    ScriptEditorDialog(MarketplaceStore store, String defaultName, String rawName, String defaultContent, Component exception) {
        this.store = store;
        this.defaultName = defaultName;
        rawName = rawName == null ? defaultName : rawName;

        setTitle(component(TITLE));
        setAfterAction(NONE);
        setCloseWithEscape(false);
        setShape(new ScriptEditorShape());

        if (exception != null) {
            addText(exception);
        }

        final TextDialogField nameField = new TextDialogField();
        nameField.setInitial(rawName);
        nameField.setLabel(component(INPUT_NAME_LABEL));
        nameField.setMaxLength(PlayerStorage.MAX_SCRIPT_NAME_LENGTH);
        nameField.setKey(INPUT_NAME);
        addInput(nameField);

        final TextDialogField contentField = new TextDialogField();
        contentField.setInitial(defaultContent);
        contentField.setMaxLength(MAX_SCRIPT_LENGTH);
        contentField.setHeight(400);
        contentField.setWidth(768);
        contentField.setMultiline(true);
        contentField.setLabel(component(INPUT_CONTENT_LABEL));
        contentField.setKey(INPUT_CONTENT);
        addInput(contentField);
    }

    private class ScriptEditorShape extends BinaryDialogShape {
        ScriptEditorShape() {
            setOptionOne(new SaveActionElement());
            setOptionTwo(new CancelActionElement());
        }
    }

    private class SaveActionElement extends CustomElement {
        SaveActionElement() {
            setText(component(SAVE_ACTION));
            setTooltip(component(SAVE_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            var player = assertPlayer(audience);

            final String scriptName = response.getText(INPUT_NAME);
            final String scriptContent = response.getText(INPUT_CONTENT);

            if (scriptName == null || scriptName.isBlank()) {
                open(store, player, defaultName, scriptName, scriptContent, component(SCRIPT_NAME_EMPTY));
                return;
            }

            if (!PlayerStorage.isValidScriptName(scriptName)) {
                open(store, player, defaultName, scriptName, scriptContent, component(SCRIPT_NAME_INVALID));
                return;
            }

            if (scriptContent == null || scriptContent.isBlank()) {
                open(store, player, defaultName, scriptName, scriptContent, component(SCRIPT_CONTENT_EMPTY));
                return;
            }

            if (defaultName != null && !scriptName.equals(defaultName)) {
                PlayerStorage.deleteScript(player, defaultName);
                // the store is keyed by name, a published copy under the old name could never be unpublished again
                ScriptStoreDialog.whenLoaded(player, store.unpublish(player.getUniqueId(), defaultName), (removed) -> {
                    if (removed) {
                        player.sendMessage(component(UNPUBLISHED_RENAME,
                                Placeholder.unparsed("old_name", defaultName),
                                Placeholder.unparsed("script_name", scriptName)
                        ));
                    }
                });
            }

            PlayerStorage.saveScript(player, scriptName, scriptContent);
            audience.closeDialog();
        }
    }

    private class CancelActionElement extends CustomElement {
        CancelActionElement() {
            setText(component(CANCEL_ACTION));
            setTooltip(component(CANCEL_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final String scriptName = response.getText(INPUT_NAME);
            final String scriptContent = response.getText(INPUT_CONTENT);

            if (scriptContent == null || scriptContent.isBlank()) {
                audience.closeDialog();
                return;
            }

            assertPlayer(audience).showDialog(
                    new LeaveConfirmDialog(
                            LEAVE_EARLY_WARNING,
                            (player) -> open(store, player, defaultName, scriptName, scriptContent, null)
                    ).compile()
            );
        }
    }
}
