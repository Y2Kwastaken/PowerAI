package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.data.MarketplaceStore.Page;
import sh.miles.powerai.data.objs.ScriptStoreEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.NONE;
import static net.kyori.adventure.text.Component.text;
import static sh.miles.powerai.dialog.v2.Cutiepie.component;

public final class ScriptStoreDialog extends DialogScreen {

    private static final String TITLE = "Script Store";

    private static final String SCRIPT_ACTION = "<script_name> by <gold><owner_name>";
    private static final String STORE_EMPTY = "<gray>No scripts have been published yet";
    private static final String SCRIPT_TOOLTIP = "click to view the install page for <gray><italic><script_name>";
    private static final String NEXT_ACTION = "Next Page →";
    private static final String NEXT_TOOLTIP = "navigates to the next store page";
    private static final String PREVIOUS_ACTION = "← Previous Page";
    private static final String PREVIOUS_TOOLTIP = "navigates to the previous store page";
    private static final String EXIT_ACTION = "<red>Exit";
    private static final String EXIT_TOOLTIP = "exits the store";

    public static void open(MarketplaceStore store, Player player, int pageNumber) {
        open(store, player, pageNumber, null);
    }

    public static void open(MarketplaceStore store, Player player, int pageNumber, Component status) {
        whenLoaded(player, store.page(pageNumber), (page) -> {
            player.showDialog(
                    new ScriptStoreDialog(store, page, status).compile()
            );
        });
    }

    private final MarketplaceStore store;
    private final Page page;

    private ScriptStoreDialog(MarketplaceStore store, Page page, Component status) {
        this.store = store;
        this.page = page;

        setTitle(component(TITLE));
        setCloseWithEscape(true);
        setAfterAction(NONE);

        if (status != null) {
            addText(status);
        }

        if (page.entries().isEmpty() && !page.hasPrevious()) {
            addText(component(STORE_EMPTY));
        }

        setShape(new ScriptStoreShape());
    }

    private class ScriptStoreShape extends MultiDialogShape {
        private ScriptStoreShape() {
            setColumns(2);
            setExitAction(new ExitElement());

            boolean addedButtons = addNextPrevButtons();
            final List<ScriptStoreEntry> entries = page.entries();
            for (ScriptStoreEntry entry : entries) {
                addAction(new ScriptElement(entry));
            }
            if (entries.size() % 2 != 0 && addedButtons) {
                addAction(new EmptyButton());
            }
            addNextPrevButtons();
        }

        private boolean addNextPrevButtons() {
            int added = 0;
            if (page.hasPrevious()) {
                PageChangeElement prev = new PageChangeElement(page.number() - 1);
                prev.setText(component(PREVIOUS_ACTION));
                prev.setTooltip(component(PREVIOUS_TOOLTIP));
                addAction(prev);
                added++;
            }

            if (page.hasNext()) {
                PageChangeElement next = new PageChangeElement(page.number() + 1);
                next.setText(component(NEXT_ACTION));
                next.setTooltip(component(NEXT_TOOLTIP));
                addAction(next);
                added++;
            }

            if (added == 1) {
                addAction(new EmptyButton());
            }
            return added > 0;
        }
    }

    private class PageChangeElement extends CustomElement {
        private final int changeTo;

        private PageChangeElement(int changeTo) {
            this.changeTo = changeTo;
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);
            open(store, player, changeTo);
        }
    }

    private class ScriptElement extends CustomElement {
        private final OfflinePlayer owner;
        private final ScriptStoreEntry entry;

        private ScriptElement(ScriptStoreEntry entry) {
            this.owner = Bukkit.getOfflinePlayer(entry.owner());
            this.entry = entry;
            setText(component(SCRIPT_ACTION,
                    Placeholder.unparsed("script_name", entry.name()),
                    Placeholder.unparsed("owner_name", owner.getName() == null ? "???" : owner.getName())
            ));
            setTooltip(component(SCRIPT_TOOLTIP, Placeholder.unparsed("script_name", entry.name())));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            final Player player = assertPlayer(audience);
            ScriptStoreInstallerDialog.open(
                    player,
                    store,
                    owner,
                    entry.name(),
                    entry.script(),
                    page.number()
            );
        }
    }

    private static class ExitElement extends CustomElement {
        private ExitElement() {
            setText(component(EXIT_ACTION));
            setTooltip(component(EXIT_TOOLTIP));
        }
        @Override
        public void accept(DialogResponseView response, Audience audience) {
            audience.closeDialog();
        }
    }

    private static class EmptyButton extends CustomElement {
        private EmptyButton() {
            setText(component(""));
            setTooltip(component(""));
        }
    }

    static <T> void whenLoaded(Player player, CompletableFuture<T> request, Consumer<T> onMain) {
        final Plugin plugin = JavaPlugin.getProvidingPlugin(ScriptStoreDialog.class);
        request.whenComplete((value, error) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (error != null) {
                    plugin.getSLF4JLogger().error("script store request failed", error);
                    player.sendMessage(text("The script store is unavailable right now, try again later", NamedTextColor.RED));
                    return;
                }
                onMain.accept(value);
            });
        });
    }


}
