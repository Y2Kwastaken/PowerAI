package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.input.TextDialogField;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.util.PlayerStorage;

import java.util.Locale;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.CLOSE;
import static net.kyori.adventure.text.Component.object;
import static sh.miles.powerai.dialog.v2.Cutiepie.*;

public final class ScriptStoreInstallerDialog extends DialogScreen {

    private static final String TITLE = "<owner_head> <owner_name> | <script_name>";
    private static final String LINE_COUNT = "<dark_gray><line_count> lines";
    private static final String INSTALL_SUCCESS = "<green>Successfully installed <script_name> by <owner_name>";
    private static final String INSTALL_FAILURE_NAME = "<red>Could not install <script_name> as \"<input_name>\", script names must be 1-" + PlayerStorage.MAX_SCRIPT_NAME_LENGTH + " characters of a-z, 0-9, '.', '_' or '-'";
    private static final String INSTALL_FAILURE_REPEAT = "<red>Could not install, you already have a script named <script_name>";
    private static final String INSTALL_FAILURE_UNKNOWN = "<red>Could not install <script_name> for an unknown reason";

    private static final String INSTALL_ACTION = "<green>Install";
    private static final String INSTALL_TOOLTIP = "installs this script into your script directory";
    private static final String EXIT_ACTION = "<red>Exit";
    private static final String EXIT_TOOLTIP = "exit this script screen and return to the store";
    private static final String NAME_LABEL = "Script Name";

    private static final String INSTALLED_NAME = "installed_name";
    private static final int PREVIEW_ROWS = 10;

    public static void open(Player player, MarketplaceStore store, OfflinePlayer owner, String scriptName, String scriptContent, int storePage) {
        open(player, store, owner, scriptName, scriptContent, storePage, null);
    }

    public static void open(Player player, MarketplaceStore store, OfflinePlayer owner, String scriptName, String scriptContent, int storePage, Component exception) {
        player.showDialog(
                new ScriptStoreInstallerDialog(
                        store,
                        owner,
                        scriptName,
                        scriptContent,
                        storePage,
                        exception
                ).compile()
        );
    }

    private final MarketplaceStore store;
    private final OfflinePlayer owner;
    private final String scriptName;
    private final String scriptContent;
    private final int storePage;

    private ScriptStoreInstallerDialog(MarketplaceStore store, OfflinePlayer owner, String scriptName, String scriptContent, int storePage, Component exception) {
        this.store = store;
        this.owner = owner;
        this.scriptName = scriptName;
        this.scriptContent = scriptContent;
        this.storePage = storePage;

        setTitle(component(TITLE,
                Placeholder.component("owner_head", object(ObjectContents.playerHead(owner))),
                Placeholder.unparsed("owner_name", owner.getName() == null ? "???" : owner.getName()),
                Placeholder.unparsed("script_name", scriptName)
        ));
        setAfterAction(CLOSE);
        setCloseWithEscape(true);

        if (exception != null) {
            addText(exception);
        }

        final String description = scrapeScriptDescription(scriptContent);
        if (description.isEmpty()) {
            // raw lua, so it must not go through MiniMessage
            final int lineCount = getScriptLineCount(scriptContent);
            final String preview = scrapeFirstNRows(scriptContent, PREVIEW_ROWS) + (lineCount > PREVIEW_ROWS ? "\n..." : "");
            addText(Component.text(preview, NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
            addText(COLORS.deserialize(description));
        }
        addText(component(LINE_COUNT,
                Placeholder.unparsed("line_count", getScriptLineCount(scriptContent) + "")
        ));

        TextDialogField nameField = new TextDialogField();
        nameField.setLabel(component(NAME_LABEL));
        nameField.setInitial(scriptName);
        nameField.setMaxLength(PlayerStorage.MAX_SCRIPT_NAME_LENGTH);
        nameField.setKey(INSTALLED_NAME);
        addInput(nameField);

        setShape(new ScriptStoreInstallerShape());
    }

    private class ScriptStoreInstallerShape extends MultiDialogShape {
        private ScriptStoreInstallerShape() {
            setColumns(2);
            setExitAction(new ExitElement());

            addAction(new InstallElement());
        }
    }

    private class InstallElement extends CustomElement {
        InstallElement() {
            setText(component(INSTALL_ACTION));
            setTooltip(component(INSTALL_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);

            final String rawName = response.getText(INSTALLED_NAME);
            final String inputtedName = rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
            if (!PlayerStorage.isValidScriptName(inputtedName)) {
                open(player, store, owner, scriptName, scriptContent, storePage, component(INSTALL_FAILURE_NAME,
                        Placeholder.unparsed("script_name", scriptName),
                        Placeholder.unparsed("input_name", inputtedName)
                ));
                return;
            }

            if (PlayerStorage.hasScript(player, inputtedName)) {
                open(player, store, owner, scriptName, scriptContent, storePage, component(INSTALL_FAILURE_REPEAT,
                        Placeholder.unparsed("script_name", inputtedName)
                ));
                return;
            }

            if (PlayerStorage.saveScript(player, inputtedName, scriptContent)) {
                ScriptStoreDialog.open(store, player, storePage, component(INSTALL_SUCCESS,
                        Placeholder.unparsed("script_name", inputtedName),
                        Placeholder.unparsed("owner_name", owner.getName() == null ? "???" : owner.getName())
                ));
            } else {
                open(player, store, owner, scriptName, scriptContent, storePage, component(INSTALL_FAILURE_UNKNOWN,
                        Placeholder.unparsed("script_name", scriptName)
                ));
            }
        }
    }

    private class ExitElement extends CustomElement {
        ExitElement() {
            setText(component(EXIT_ACTION));
            setTooltip(component(EXIT_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            ScriptStoreDialog.open(store, assertPlayer(audience), storePage);
        }
    }


}
