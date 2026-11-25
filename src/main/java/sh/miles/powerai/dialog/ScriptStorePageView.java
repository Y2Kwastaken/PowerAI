package sh.miles.powerai.dialog;

import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.BinaryDialogShape;
import sh.miles.dialogkit.shape.MultiDialogShape;
import sh.miles.powerai.data.MarketplaceStore;
import sh.miles.powerai.data.objs.ScriptStoreEntry;
import sh.miles.powerai.util.PlayerStorage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

public final class ScriptStorePageView extends DialogScreen {

    public ScriptStorePageView(MarketplaceStore store, MarketplaceStore.Page page) {
        super.setTitle(text("Script Store"));
        if (page.entries().isEmpty()) {
            super.addText(text(page.number() == 0
                    ? "Nobody has published a script yet. Be the first!"
                    : "There's nothing on this page."));
        } else {
            super.addText(text("Page " + (page.number() + 1) + ". Pick a script to preview and install it."));
        }

        final Consumer<Player> back = player -> open(player, store, page.number());
        MultiDialogShape shape = new MultiDialogShape();
        for (ScriptStoreEntry entry : page.entries()) {
            shape.addAction(new EntryElement(store, entry, back));
        }
        if (page.hasPrevious()) shape.addAction(new PageElement(store, page.number() - 1, "< Previous"));
        if (page.hasNext()) shape.addAction(new PageElement(store, page.number() + 1, "Next >"));
        shape.addAction(new PublishMenuElement(store));
        shape.addAction(new PublishedMenuElement(store));
        shape.setExitAction(new ScriptManagerElement(store, "Back"));
        shape.setColumns(2);
        super.setShape(shape);

        super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
    }

    public static void open(Player player, MarketplaceStore store, int page) {
        whenLoaded(player, store.page(page), loaded -> player.showDialog(new ScriptStorePageView(store, loaded).compile()));
    }

    static <T> void whenLoaded(Player player, CompletableFuture<T> request, Consumer<T> onMain) {
        final Plugin plugin = JavaPlugin.getProvidingPlugin(ScriptStorePageView.class);
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

    /**
     * Shows every script one player has published. When the viewer is the owner they can unpublish from here
     *
     * @param ownerName shown in the title, the store only knows the owner's UUID
     */
    public static void openOwner(Player viewer, MarketplaceStore store, UUID owner, String ownerName) {
        whenLoaded(viewer, store.byOwner(owner),
                entries -> viewer.showDialog(new PublishedDialog(store, viewer, owner, ownerName, entries).compile()));
    }

    private static void openPublished(Player player, MarketplaceStore store) {
        openOwner(player, store, player.getUniqueId(), player.getName());
    }

    private static void openPublishPicker(Player player, MarketplaceStore store) {
        whenLoaded(player, store.byOwner(player.getUniqueId()), published -> {
            Set<String> names = published.stream().map(ScriptStoreEntry::name).collect(Collectors.toSet());
            player.showDialog(new PublishDialog(store, player, names).compile());
        });
    }

    private static void install(Player player, ScriptStoreEntry entry, Consumer<Player> back) {
        if (PlayerStorage.saveScript(player, entry.name(), entry.script())) {
            player.sendMessage(text("Installed " + entry.name() + ", run it with /scripts run " + entry.name(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(text("Couldn't install " + entry.name(), NamedTextColor.RED));
        }
        back.accept(player);
    }

    private static String ownerName(ScriptStoreEntry entry) {
        String name = Bukkit.getOfflinePlayer(entry.owner()).getName();
        return name == null ? "unknown" : name;
    }

    private static String ago(Instant instant) {
        long seconds = Math.max(0, Duration.between(instant, Instant.now()).toSeconds());
        if (seconds < 60) return "just now";
        if (seconds < 3600) return plural(seconds / 60, "minute") + " ago";
        if (seconds < 86400) return plural(seconds / 3600, "hour") + " ago";
        return plural(seconds / 86400, "day") + " ago";
    }

    private static String plural(long amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s");
    }

    private static final class EntryDialog extends DialogScreen {
        private EntryDialog(MarketplaceStore store, Player viewer, ScriptStoreEntry entry, Consumer<Player> back) {
            super.setTitle(text(entry.name()));
            super.addText(text("by " + ownerName(entry) + ", published " + ago(entry.publishedAt()), NamedTextColor.GRAY));
            super.addText(text(ScriptManagerDialog.preview(entry.script()), NamedTextColor.GRAY), 400);

            MultiDialogShape shape = new MultiDialogShape();
            shape.addAction(new InstallElement(entry, back));
            if (viewer.getUniqueId().equals(entry.owner())) {
                shape.addAction(new UnpublishElement(store, entry, back));
            }
            shape.setExitAction(new BackElement(back));
            shape.setColumns(2);
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static final class OverwriteDialog extends DialogScreen {
        private OverwriteDialog(ScriptStoreEntry entry, Consumer<Player> back) {
            super.setTitle(text("Replace Script"));
            super.addText(text("You already have a script named '" + entry.name() + "'. Installing will replace it, this can't be undone."));

            BinaryDialogShape shape = new BinaryDialogShape();
            shape.setOptionOne(new ConfirmInstallElement(entry, back));
            shape.setOptionTwo(new BackElement(back));
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static final class PublishDialog extends DialogScreen {
        private PublishDialog(MarketplaceStore store, Player player, Set<String> alreadyPublished) {
            super.setTitle(text("Publish a Script"));

            List<String> scripts = PlayerStorage.getScripts(player);
            MultiDialogShape shape = new MultiDialogShape();
            if (scripts.isEmpty()) {
                super.addText(text("You don't have any scripts to publish yet."));
                shape.addAction(new ScriptManagerElement(store, "Open Script Manager"));
            } else {
                super.addText(text("Pick a script to share it with everyone. Publishing one that's already in the store updates it."));
                for (String script : scripts) {
                    shape.addAction(new PublishElement(store, script, alreadyPublished.contains(script)));
                }
            }
            shape.setExitAction(new BackElement(viewer -> open(viewer, store, 0)));
            shape.setColumns(2);
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static final class PublishedDialog extends DialogScreen {
        private PublishedDialog(MarketplaceStore store, Player viewer, UUID owner, String ownerName, List<ScriptStoreEntry> entries) {
            final boolean self = viewer.getUniqueId().equals(owner);
            super.setTitle(text(self ? "Your Published Scripts" : "Scripts by " + ownerName, NamedTextColor.AQUA));

            MultiDialogShape shape = new MultiDialogShape();
            if (entries.isEmpty()) {
                if (self) {
                    super.addText(text("You haven't published anything yet."));
                    shape.addAction(new PublishMenuElement(store));
                } else {
                    super.addText(text(ownerName + " hasn't published anything yet."));
                    shape.addAction(new PageElement(store, 0, "Browse the Store"));
                }
            } else {
                super.addText(text(self ? "Select one to view or unpublish it." : "Select one to preview and install it."));
                final Consumer<Player> back = player -> openOwner(player, store, owner, ownerName);
                for (ScriptStoreEntry entry : entries) {
                    shape.addAction(new EntryElement(store, entry, back));
                }
            }
            shape.setExitAction(new BackElement(player -> open(player, store, 0)));
            shape.setColumns(2);
            super.setShape(shape);

            super.setAfterAction(DialogBase.DialogAfterAction.CLOSE);
        }
    }

    private static final class EntryElement extends CustomElement {
        private final MarketplaceStore store;
        private final ScriptStoreEntry entry;
        private final Consumer<Player> back;

        private EntryElement(MarketplaceStore store, ScriptStoreEntry entry, Consumer<Player> back) {
            this.store = store;
            this.entry = entry;
            this.back = back;
            setText(text(entry.name() + " by " + ownerName(entry)));
            setTooltip(text("by " + ownerName(entry) + ", published " + ago(entry.publishedAt())));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            player.showDialog(new EntryDialog(store, player, entry, back).compile());
        }
    }

    private static final class PageElement extends CustomElement {
        private final MarketplaceStore store;
        private final int number;

        private PageElement(MarketplaceStore store, int number, String label) {
            this.store = store;
            this.number = number;
            setText(text(label, NamedTextColor.YELLOW));
            setTooltip(text("Go to page " + (number + 1)));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            open(assertPlayer(audience), store, number);
        }
    }

    private static final class InstallElement extends CustomElement {
        private final ScriptStoreEntry entry;
        private final Consumer<Player> back;

        private InstallElement(ScriptStoreEntry entry, Consumer<Player> back) {
            this.entry = entry;
            this.back = back;
            setText(text("Install", NamedTextColor.GREEN));
            setTooltip(text("Copy " + entry.name() + " into your scripts"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            if (PlayerStorage.hasScript(player, entry.name())) {
                player.showDialog(new OverwriteDialog(entry, back).compile());
            } else {
                install(player, entry, back);
            }
        }
    }

    private static final class ConfirmInstallElement extends CustomElement {
        private final ScriptStoreEntry entry;
        private final Consumer<Player> back;

        private ConfirmInstallElement(ScriptStoreEntry entry, Consumer<Player> back) {
            this.entry = entry;
            this.back = back;
            setText(text("Replace", NamedTextColor.RED));
            setTooltip(text("Overwrite your " + entry.name() + " with this one"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            install(assertPlayer(audience), entry, back);
        }
    }

    private static final class UnpublishElement extends CustomElement {
        private final MarketplaceStore store;
        private final ScriptStoreEntry entry;
        private final Consumer<Player> back;

        private UnpublishElement(MarketplaceStore store, ScriptStoreEntry entry, Consumer<Player> back) {
            this.store = store;
            this.entry = entry;
            this.back = back;
            setText(text("▼ Unpublish", NamedTextColor.RED));
            setTooltip(text("Remove " + entry.name() + " from the store. Your own copy stays"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            whenLoaded(player, store.unpublish(entry.owner(), entry.name()), removed -> {
                player.sendMessage(removed
                        ? text("Removed " + entry.name() + " from the store", NamedTextColor.GRAY)
                        : text(entry.name() + " wasn't in the store anymore", NamedTextColor.GRAY));
                back.accept(player);
            });
        }
    }

    private static final class PublishMenuElement extends CustomElement {
        private final MarketplaceStore store;

        private PublishMenuElement(MarketplaceStore store) {
            this.store = store;
            setText(text("▲ Publish a Script", NamedTextColor.GOLD));
            setTooltip(text("Share one of your scripts with everyone"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openPublishPicker(assertPlayer(audience), store);
        }
    }

    private static final class PublishedMenuElement extends CustomElement {
        private final MarketplaceStore store;

        private PublishedMenuElement(MarketplaceStore store) {
            this.store = store;
            setText(text("Your Published Scripts", NamedTextColor.AQUA));
            setTooltip(text("See and unpublish what you've shared"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            openPublished(assertPlayer(audience), store);
        }
    }

    private static final class PublishElement extends CustomElement {
        private final MarketplaceStore store;
        private final String scriptName;

        private PublishElement(MarketplaceStore store, String scriptName, boolean republish) {
            this.store = store;
            this.scriptName = scriptName;
            setText(republish ? text(scriptName).append(text(" (update)", NamedTextColor.GRAY)) : text(scriptName));
            setTooltip(text(republish ? "Update the published copy of " + scriptName : "Publish " + scriptName));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            var player = assertPlayer(audience);
            String content = PlayerStorage.getScript(player, scriptName);
            if (content == null) {
                openPublishPicker(player, store);
                return;
            }

            whenLoaded(player, store.publish(player.getUniqueId(), scriptName, content), entry -> {
                player.sendMessage(text("Published " + entry.name() + " to the store", NamedTextColor.GREEN));
                open(player, store, 0);
            });
        }
    }

    private static final class ScriptManagerElement extends CustomElement {
        private final MarketplaceStore store;

        private ScriptManagerElement(MarketplaceStore store, String label) {
            this.store = store;
            setText(text(label));
            setTooltip(text("Go to your scripts"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            ScriptManagerDialog.open(assertPlayer(audience), store);
        }
    }

    private static final class BackElement extends CustomElement {
        private final Consumer<Player> back;

        private BackElement(Consumer<Player> back) {
            this.back = back;
            setText(text("Back"));
        }

        @Override
        public void accept(final DialogResponseView response, final Audience audience) {
            back.accept(assertPlayer(audience));
        }
    }
}
