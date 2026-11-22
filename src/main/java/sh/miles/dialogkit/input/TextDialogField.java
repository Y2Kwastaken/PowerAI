package sh.miles.dialogkit.input;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;

public class TextDialogField implements DialogField {

    private String key = null;
    private Component label = null;
    private boolean showLabel = true;
    private String initial = null;
    private int maxLength = -1;
    private int width = -1;
    private boolean multiline = false;
    private int maxLines = -1;
    private int height = -1;

    public void setKey(final String key) {
        this.key = key;
    }

    public void setLabel(final Component label) {
        this.label = label;
    }

    public void setShowLabel(final boolean showLabel) {
        this.showLabel = showLabel;
    }

    public void setInitial(final String initial) {
        this.initial = initial;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setMultiline(final boolean multiline) {
        this.multiline = multiline;
    }

    public void setMaxLines(final int maxLines) {
        this.maxLines = maxLines;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    @Override
    public DialogInput toPaper() {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(label != null, "label must not be null");
        TextDialogInput.Builder builder = DialogInput.text(this.key, this.label);
        builder.labelVisible(this.showLabel);
        if (this.maxLength > 0) {
            builder.maxLength(this.maxLength);
        }
        if (this.initial != null) {
            builder.initial(this.initial);
        }
        if (this.width > 0) {
            builder.width(this.width);
        }
        if (this.multiline) {
            builder.multiline(TextDialogInput.MultilineOptions.create(
                    this.maxLines > 0 ? this.maxLines : null,
                    this.height > 0 ? this.height : null
            ));
        }
        return builder.build();
    }
}
