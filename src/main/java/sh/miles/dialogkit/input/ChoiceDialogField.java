package sh.miles.dialogkit.input;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ChoiceDialogField implements DialogField {

    private final List<Option> options = new ArrayList<>(5);
    private String key = null;
    private Component label = null;
    private String initial = null;
    private boolean showLabel = true;
    private int width = -1;

    public void setKey(final String key) {
        this.key = key;
    }

    public void setLabel(final Component label) {
        this.label = label;
    }

    public void addOption(final String id) {
        addOption(id, null);
    }

    public void addOption(final String id, final Component display) {
        Preconditions.checkArgument(id != null, "id must not be null");
        this.options.add(new Option(id, display));
    }

    public void setInitial(final String id) {
        this.initial = id;
    }

    public void setShowLabel(final boolean showLabel) {
        this.showLabel = showLabel;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    @Override
    public DialogInput toPaper() {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(label != null, "label must not be null");
        Preconditions.checkArgument(!options.isEmpty(), "must have at least one option");
        final List<SingleOptionDialogInput.OptionEntry> entries = this.options.stream()
                .map(option -> SingleOptionDialogInput.OptionEntry.create(option.id(), option.display(), option.id().equals(this.initial)))
                .toList();
        SingleOptionDialogInput.Builder builder = DialogInput.singleOption(this.key, this.label, entries);
        builder.labelVisible(this.showLabel);
        if (this.width > 0) {
            builder.width(this.width);
        }
        return builder.build();
    }

    private record Option(String id, Component display) {
    }
}
