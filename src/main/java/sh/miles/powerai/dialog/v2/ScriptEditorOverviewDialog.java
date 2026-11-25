package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.dialogkit.shape.NoticeDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.util.PlayerStorage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.NONE;
import static net.kyori.adventure.text.Component.object;
import static net.kyori.adventure.text.minimessage.MiniMessage.Preset.NON_INTERACTABLE;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static sh.miles.powerai.dialog.v2.Cutiepie.*;

public final class ScriptEditorOverviewDialog extends DialogScreen {

    private static final String TITLE = "<owner_head> <owner_name> | <script_name> Overview";
    private static final String EDIT_ACTION = "<gold>Edit";
    private static final String EDIT_ACTION_OTHER = "<gold>Copy and Edit";
    private static final String EDIT_TOOLTIP = "edits this script";
    private static final String EDIT_TOOLTIP_OTHER = "makes a copy of this script and opens an editor";
    private static final String DELETE_ACTION = "<red>Delete";
    private static final String DELETE_TOOLTIP = "deletes this script";
    private static final String EXIT_ACTION = "<red>Exit";
    private static final String EXIT_TOOLTIP = "exits this dialog";

    private static final String SCRIPT_HAS_SAME_NAME = "<red>You already have a script with this name consider changing this script name or it will overwrite your existing script";
    private static final String SCRIPT_NOT_EXISTS = "<red>This script no longer exists or the owner is offline";
    private static final String SCRIPT_CANT_DELETE = "<red> you can't delete a script you don't own";
    private static final String SCRIPT_NO_DESCRIPTION = "<dark_gray><i>This script has no description";
    private static final String DELETE_CONFIRMATION = "<red>Are you sure you wish to delete the script <script_name>";

    private static final String PUBLISHED_NOTICE = "<gold>This script is published in the script store";
    private static final String PUBLISH_ACTION = "<gold>Publish";
    private static final String PUBLISH_TOOLTIP = "publishes this script to the script store";
    private static final String UPDATE_ACTION = "<gold>Update Published";
    private static final String UPDATE_TOOLTIP = "replaces the published copy in the script store with this version";
    private static final String UNPUBLISH_ACTION = "<red>Unpublish";
    private static final String UNPUBLISH_TOOLTIP = "removes this script from the script store";
    private static final String PUBLISHED = "<green>Published <script_name> to the script store";
    private static final String UNPUBLISHED = "<gray>Removed <script_name> from the script store";

    /**
     * Without this permission there are no publish options at all
     */
    public static final String STORE_PERMISSION = "powerai.command.scripts.store";

    public static void open(MarketplaceStore store, Player player, String scriptName) {
        open(store, player, player, scriptName, null);
    }

    public static void open(MarketplaceStore store, Player opener, Player owner, String scriptName) {
        open(store, opener, owner, scriptName, null);
    }

    public static void open(MarketplaceStore store, Player opener, Player owner, String scriptName, Component exception) {
        // publish options are only for the owner, and only with the store permission
        if (!opener.getUniqueId().equals(owner.getUniqueId()) || !opener.hasPermission(STORE_PERMISSION)) {
            opener.showDialog(
                    new ScriptEditorOverviewDialog(store, owner, opener.getUniqueId(), scriptName, exception, false, false).compile()
            );
            return;
        }

        // the publish buttons depend on whether this script is already in the store
        final CompletableFuture<Boolean> published = store.byOwner(owner.getUniqueId())
                .thenApply(entries -> entries.stream().anyMatch(entry -> entry.name().equals(scriptName)));
        ScriptStoreDialog.whenLoaded(opener, published, (isPublished) -> opener.showDialog(
                new ScriptEditorOverviewDialog(store, owner, opener.getUniqueId(), scriptName, exception, true, isPublished).compile()
        ));
    }

    private ScriptEditorOverviewDialog(MarketplaceStore store, Player owner, UUID opener, String scriptName, Component exception, boolean canPublish, boolean published) {
        setTitle(component(TITLE,
                Placeholder.component("owner_head", object(ObjectContents.playerHead(owner))),
                Placeholder.unparsed("owner_name", owner.getName()),
                Placeholder.unparsed("script_name", scriptName)
        ));
        setCloseWithEscape(true);
        setAfterAction(NONE);

        if (exception != null) {
            addText(exception);
        }

        final String scriptContent = PlayerStorage.getScript(owner, scriptName);
        if (scriptContent == null || scriptContent.isBlank() || !owner.isOnline()) {
            addText(component(SCRIPT_NOT_EXISTS));
            setShape(new ConfirmDialogShape());
            return;
        }

        String description = scrapeScriptDescription(scriptContent);
        // COLORS prevents mini message injection by malicious actors since this is user supplied from scripts
        addText(COLORS.deserialize(description.isBlank() ? SCRIPT_NO_DESCRIPTION : description));

        if (published) {
            addText(component(PUBLISHED_NOTICE));
        }

        setShape(new ScriptEditorOverviewShape(store, owner, opener, scriptName, scriptContent, canPublish, published));
    }

    private static class ScriptEditorOverviewShape extends MultiDialogShape {
        private ScriptEditorOverviewShape(MarketplaceStore store, Player owner, UUID opener, String scriptName, String scriptContent, boolean canPublish, boolean published) {
            setColumns(2);
            setExitAction(new ExitElement(store, owner));

            addAction(new EditElement(store, owner.getUniqueId(), opener, scriptName, scriptContent));
            if (owner.getUniqueId().equals(opener)) {
                addAction(new DeleteElement(store, owner, scriptName));
            }

            if (canPublish) {
                addAction(new PublishElement(store, scriptName, published));
                if (published) {
                    addAction(new UnpublishElement(store, scriptName));
                }
            }
        }
    }

    private static class PublishElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        PublishElement(MarketplaceStore store, String scriptName, boolean published) {
            this.store = store;
            this.scriptName = scriptName;

            setText(component(published ? UPDATE_ACTION : PUBLISH_ACTION));
            setTooltip(component(published ? UPDATE_TOOLTIP : PUBLISH_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);
            // the permission could have been taken away while the dialog was open
            if (!player.hasPermission(STORE_PERMISSION)) {
                open(store, player, scriptName);
                return;
            }

            // publish what's saved now, not what was saved when the dialog opened
            final String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                open(store, player, scriptName);
                return;
            }

            ScriptStoreDialog.whenLoaded(player, store.publish(player.getUniqueId(), scriptName, content), (entry) ->
                    open(store, player, player, scriptName, component(PUBLISHED, Placeholder.unparsed("script_name", entry.name())))
            );
        }
    }

    private static class UnpublishElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        UnpublishElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;

            setText(component(UNPUBLISH_ACTION));
            setTooltip(component(UNPUBLISH_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);
            ScriptStoreDialog.whenLoaded(player, store.unpublish(player.getUniqueId(), scriptName), (removed) ->
                    open(store, player, player, scriptName, component(UNPUBLISHED, Placeholder.unparsed("script_name", scriptName)))
            );
        }
    }

    private static class EditElement extends CustomElement {
        private final MarketplaceStore store;
        private final UUID owner;
        private final String scriptName;
        private final String scriptContent;

        EditElement(MarketplaceStore store, UUID owner, UUID opener, String scriptName, String scriptContent) {
            this.store = store;
            this.owner = owner;
            this.scriptName = scriptName;
            this.scriptContent = scriptContent;

            setText(component(opener.equals(owner) ? EDIT_ACTION : EDIT_ACTION_OTHER));
            setTooltip(component(opener.equals(owner) ? EDIT_TOOLTIP : EDIT_TOOLTIP_OTHER));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            Player editor = assertPlayer(audience);
            Component exception = null;
            if (!editor.getUniqueId().equals(owner) && PlayerStorage.hasScript(editor, this.scriptName)) {
                exception = component(SCRIPT_HAS_SAME_NAME);
            }

            if (editor.getUniqueId().equals(owner)) {
                ScriptEditorDialog.open(store, editor, this.scriptName, this.scriptContent, exception);       // own script: rename replaces it
            } else {
                ScriptEditorDialog.open(store, editor, null, this.scriptName, this.scriptContent, exception); // copy: never deletes anything
            }
        }
    }

    private static class DeleteElement extends CustomElement {
        private final MarketplaceStore store;
        private final Player owner;
        private final String scriptName;

        DeleteElement(MarketplaceStore store, Player owner, String scriptName) {
            this.store = store;
            this.owner = owner;
            this.scriptName = scriptName;

            setText(component(DELETE_ACTION));
            setTooltip(component(DELETE_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            assertPlayer(audience).showDialog(
                    new LeaveConfirmDialog(
                            DELETE_CONFIRMATION.replace("<script_name>", scriptName),
                            (player) -> {
                                PlayerStorage.deleteScript(player, scriptName);
                                // only the editor can unpublish, so a deleted script must not stay behind in the store
                                store.unpublish(player.getUniqueId(), scriptName);
                                ScriptOverviewDialog.open(store, player, owner);
                            },
                            (player) -> open(store, player, this.owner, this.scriptName)
                    ).compile()
            );
        }
    }

    private static class ExitElement extends CustomElement {
        private final MarketplaceStore store;
        private final Player owner;

        ExitElement(MarketplaceStore store, Player owner) {
            this.store = store;
            this.owner = owner;

            setText(component(EXIT_ACTION));
            setTooltip(component(EXIT_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            ScriptOverviewDialog.open(store, assertPlayer(audience), this.owner);
        }
    }

}
