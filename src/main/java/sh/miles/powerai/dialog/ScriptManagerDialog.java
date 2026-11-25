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
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.data.objs.ScriptStoreEntry;
import sh.miles.powerai.util.PlayerStorage;

import java.util.List;

import static net.kyori.adventure.text.Component.text;

public class ScriptManagerDialog extends DialogScreen {

    private static final int PREVIEW_LINES = 8;
    private static final int PREVIEW_LINE_LENGTH = 80;

    public ScriptManagerDialog(Player player, MarketplaceStore store) {
        super.setTitle(text("Script Manager"));

        List<String> scripts = PlayerStorage.getScripts(player);
        if (scripts.isEmpty()) {
            super.addText(text("You don't have any scripts yet. Create one to get started."));
        } else {
            super.addText(text("You have " + scripts.size() + (scripts.size() == 1 ? " script" : " scripts") + ". Select one to edit, delete or publish it."));
        }

        MultiDialogShape shape = new MultiDialogShape();
        shape.addAction(new NewScriptElement(store));
        shape.addAction(new OpenStoreElement(store));
        for (String script : scripts) {
            shape.addAction(new OpenScriptElement(store, script));
        }
        shape.setExitAction(new CloseElement());
        shape.setColumns(2);
        super.setShape(shape);

        super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
    }

    public static void open(Player player, MarketplaceStore store) {
        player.showDialog(new ScriptManagerDialog(player, store).compile());
    }

    /**
     * Looks up whether the script is published first, so the options can offer publish or unpublish
     */
    private static void openScript(Player player, MarketplaceStore store, String scriptName) {
        if (!PlayerStorage.hasScript(player, scriptName)) {
            open(player, store);
            return;
        }

        ScriptStorePageView.whenLoaded(player, store.byOwner(player.getUniqueId()), published -> {
            // read the content after the lookup, the script could have been edited while we waited
            String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                open(player, store);
                return;
            }

            boolean isPublished = published.stream().map(ScriptStoreEntry::name).anyMatch(scriptName::equals);
            player.showDialog(new ScriptOptionsDialog(store, scriptName, content, isPublished).compile());
        });
    }

    static String preview(String content) {
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

    private static class PublishScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private PublishScriptElement(MarketplaceStore store, String scriptName, boolean isPublished) {
            this.store = store;
            this.scriptName = scriptName;
            if (isPublished) {
                setText(text("▲ Update Published", NamedTextColor.GOLD));
                setTooltip(text("replaces the published copy in the script store with this version"));
            } else {
                setText(text("▲ Publish Script", NamedTextColor.GOLD));
                setTooltip(text("publishes your script to the server script store"));
            }
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            var player = assertPlayer(audience);
            String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                open(player, store);
                return;
            }

            ScriptStorePageView.whenLoaded(player, store.publish(player.getUniqueId(), scriptName, content), entry -> {
                player.sendMessage(text("Published " + entry.name() + " to the script store", NamedTextColor.GREEN));
                openScript(player, store, scriptName);
            });
        }
    }

    private static class UnpublishScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private UnpublishScriptElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text("▼ Unpublish Script", NamedTextColor.RED));
            setTooltip(text("removes your script from the global script store"));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            var player = assertPlayer(audience);
            ScriptStorePageView.whenLoaded(player, store.unpublish(player.getUniqueId(), scriptName), removed -> {
                player.sendMessage(removed
                        ? text("Removed " + scriptName + " from the script store", NamedTextColor.GRAY)
                        : text(scriptName + " wasn't in the script store", NamedTextColor.GRAY));
                openScript(player, store, scriptName);
            });
        }
    }

    private static class NewScriptElement extends CustomElement {
        private final MarketplaceStore store;

        private NewScriptElement(MarketplaceStore store) {
            this.store = store;
            setText(text("+ New Script", NamedTextColor.GREEN));
            setTooltip(text("Write a new script"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            player.showDialog(new ScriptViewDialog(store).compile());
        }
    }

    private static class OpenStoreElement extends CustomElement {
        private final MarketplaceStore store;

        private OpenStoreElement(MarketplaceStore store) {
            this.store = store;
            setText(text("Script Store", NamedTextColor.AQUA));
            setTooltip(text("Browse and install scripts other players have published"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            ScriptStorePageView.open(assertPlayer(audience), store, 0);
        }
    }

    private static class OpenScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private OpenScriptElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text(scriptName));
            setTooltip(text("Edit, delete or publish " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openScript(assertPlayer(audience), store, scriptName);
        }
    }

    private static class CloseElement extends CustomElement {
        private CloseElement() {
            setText(text("Close"));
        }
    }

    private static class ScriptOptionsDialog extends DialogScreen {
        private ScriptOptionsDialog(MarketplaceStore store, String scriptName, String content, boolean isPublished) {
            super.setTitle(text("Script: " + scriptName));
            if (isPublished) {
                super.addText(text("Published in the script store", NamedTextColor.GOLD));
            }
            super.addText(text(preview(content), NamedTextColor.GRAY), 400);

            MultiDialogShape shape = new MultiDialogShape();
            shape.addActions(new EditScriptElement(store, scriptName), new DeleteScriptElement(store, scriptName));
            shape.addAction(new PublishScriptElement(store, scriptName, isPublished));
            if (isPublished) {
                shape.addAction(new UnpublishScriptElement(store, scriptName));
            }
            shape.setExitAction(new BackElement(store));
            shape.setColumns(2);
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static class EditScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private EditScriptElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text("Edit"));
            setTooltip(text("Edit or rename " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                open(player, store);
                return;
            }

            player.showDialog(new ScriptViewDialog(store, scriptName, content).compile());
        }
    }

    private static class DeleteScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private DeleteScriptElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text("Delete", NamedTextColor.RED));
            setTooltip(text("Delete " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            if (!PlayerStorage.hasScript(player, scriptName)) {
                open(player, store);
                return;
            }

            player.showDialog(new DeleteConfirmDialog(store, scriptName).compile());
        }
    }

    private static class BackElement extends CustomElement {
        private final MarketplaceStore store;

        private BackElement(MarketplaceStore store) {
            this.store = store;
            setText(text("Back"));
            setTooltip(text("Return to the script manager"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            open(assertPlayer(audience), store);
        }
    }

    private static class DeleteConfirmDialog extends DialogScreen {
        private DeleteConfirmDialog(MarketplaceStore store, String scriptName) {
            super.setTitle(text("Delete Script"));
            super.addText(text("Are you sure you want to delete '" + scriptName + "'? This can't be undone. If it's published, the store copy stays until you unpublish it."));

            BinaryDialogShape shape = new BinaryDialogShape();
            shape.setOptionOne(new ConfirmDeleteElement(store, scriptName));
            shape.setOptionTwo(new CancelDeleteElement(store, scriptName));
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static class ConfirmDeleteElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private ConfirmDeleteElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text("Delete", NamedTextColor.RED));
            setTooltip(text("Permanently delete " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            PlayerStorage.deleteScript(player, scriptName);
            open(player, store);
        }
    }

    private static class CancelDeleteElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private CancelDeleteElement(MarketplaceStore store, String scriptName) {
            this.store = store;
            this.scriptName = scriptName;
            setText(text("Cancel"));
            setTooltip(text("Keep " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openScript(assertPlayer(audience), store, scriptName);
        }
    }
}
