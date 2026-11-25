package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.util.PlayerStorage;

import java.util.List;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.NONE;
import static sh.miles.powerai.dialog.v2.Cutiepie.component;

public final class ScriptOverviewDialog extends DialogScreen {

    private static final String TITLE = "<owner_head> <player>'s Scripts";
    private static final String SCRIPT_ACTION = "<gold><script_name>";
    private static final String SCRIPT_TOOLTIP = "opens the editor overview for <script_name>";
    private static final String ADD_ACTION = "<green>+ New Script";
    private static final String ADD_TOOLTIP = "opens the script editor";
    private static final String STORE_ACTION = "<aqua>Script Store";
    private static final String STORE_TOOLTIP = "browse and install scripts other players have published";
    private static final String EXIT_ACTION = "<red>Exit";
    private static final String EXIT_TOOLTIP = "exits this dialog";
    private static final String NO_SCRIPTS = "<player> has no scripts to view";

    public static void open(MarketplaceStore store, Player opener, Player owner) {
        opener.showDialog(
                new ScriptOverviewDialog(store, opener, owner).compile()
        );
    }

    public static void open(MarketplaceStore store, Player owner) {
        open(store, owner, owner);
    }

    private ScriptOverviewDialog(MarketplaceStore store, Player opener, Player player) {
        setTitle(component(TITLE,
                Placeholder.component("owner_head", Component.object(ObjectContents.playerHead(player))),
                Placeholder.unparsed("player", player.getName()))
        );
        setCloseWithEscape(true);
        setAfterAction(NONE);

        final List<String> scripts = PlayerStorage.getScripts(player);
        if (scripts.isEmpty()) {
            addText(component(NO_SCRIPTS, Placeholder.unparsed("player", player.getName())));
        }
        setShape(new ScriptOverviewShape(store, player, scripts, opener.hasPermission(ScriptEditorOverviewDialog.STORE_PERMISSION)));
    }

    private static class ScriptOverviewShape extends MultiDialogShape {
        ScriptOverviewShape(MarketplaceStore store, Player owner, List<String> scripts, boolean canOpenStore) {
            setColumns(3);
            setExitAction(new ExitElement());
            addAction(new AddElement(store));
            if (canOpenStore) {
                addAction(new StoreElement(store));
            }
            scripts.stream().map((scriptName) -> new ScriptElement(store, owner, scriptName)).forEach(this::addAction);
        }
    }

    private static class AddElement extends CustomElement {
        private final MarketplaceStore store;

        private AddElement(MarketplaceStore store) {
            this.store = store;
            setText(component(ADD_ACTION));
            setTooltip(component(ADD_TOOLTIP));
        }
        @Override
        public void accept(DialogResponseView response, Audience audience) {
            ScriptEditorDialog.open(store, assertPlayer(audience));
        }
    }

    private static class StoreElement extends CustomElement {
        private final MarketplaceStore store;

        private StoreElement(MarketplaceStore store) {
            this.store = store;
            setText(component(STORE_ACTION));
            setTooltip(component(STORE_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);
            // the permission could have been taken away while the dialog was open
            if (!player.hasPermission(ScriptEditorOverviewDialog.STORE_PERMISSION)) {
                audience.closeDialog();
                return;
            }
            ScriptStoreDialog.open(store, player, 0);
        }
    }

    private static class ScriptElement extends CustomElement {
        private final MarketplaceStore store;
        private final Player owner;
        private final String scriptName;

        private ScriptElement(MarketplaceStore store, Player owner, String scriptName) {
            this.store = store;
            this.owner = owner;
            this.scriptName = scriptName;

            setText(component(SCRIPT_ACTION, Placeholder.unparsed("script_name", scriptName)));
            setTooltip(component(SCRIPT_TOOLTIP, Placeholder.unparsed("script_name", scriptName)));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            ScriptEditorOverviewDialog.open(
                    store,
                    assertPlayer(audience),
                    owner,
                    scriptName
            );
        }
    }

    private static class ExitElement extends CustomElement {
        ExitElement() {
            setText(component(EXIT_ACTION));
            setTooltip(component(EXIT_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            audience.closeDialog();
        }
    }
}
