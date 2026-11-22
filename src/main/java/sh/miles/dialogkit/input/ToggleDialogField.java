package sh.miles.dialogkit.input;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;

public class ToggleDialogField implements DialogField {

    private String key = null;
    private Component label = null;
    private boolean initial = false;
    private String onTrue = null;
    private String onFalse = null;

    public void setKey(final String key) {
        this.key = key;
    }

    public void setLabel(final Component label) {
        this.label = label;
    }

    public void setInitial(final boolean initial) {
        this.initial = initial;
    }

    public void setOnTrue(final String onTrue) {
        this.onTrue = onTrue;
    }

    public void setOnFalse(final String onFalse) {
        this.onFalse = onFalse;
    }

    @Override
    public DialogInput toPaper() {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(label != null, "label must not be null");
        BooleanDialogInput.Builder builder = DialogInput.bool(this.key, this.label);
        builder.initial(this.initial);
        if (this.onTrue != null) {
            builder.onTrue(this.onTrue);
        }
        if (this.onFalse != null) {
            builder.onFalse(this.onFalse);
        }
        return builder.build();
    }
}
