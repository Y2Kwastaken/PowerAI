package sh.miles.dialogkit.input;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import net.kyori.adventure.text.Component;

public class SliderDialogField implements DialogField {

    private String key = null;
    private Component label = null;
    private Float start = null;
    private Float end = null;
    private Float initial = null;
    private Float step = null;
    private String labelFormat = null;
    private int width = -1;

    public void setKey(final String key) {
        this.key = key;
    }

    public void setLabel(final Component label) {
        this.label = label;
    }

    public void setRange(final float start, final float end) {
        this.start = start;
        this.end = end;
    }

    public void setInitial(final float initial) {
        this.initial = initial;
    }

    public void setStep(final float step) {
        this.step = step;
    }

    public void setLabelFormat(final String labelFormat) {
        this.labelFormat = labelFormat;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    @Override
    public DialogInput toPaper() {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(label != null, "label must not be null");
        Preconditions.checkArgument(start != null && end != null, "range must be set");
        NumberRangeDialogInput.Builder builder = DialogInput.numberRange(this.key, this.label, this.start, this.end);
        if (this.initial != null) {
            builder.initial(this.initial);
        }
        if (this.step != null) {
            builder.step(this.step);
        }
        if (this.labelFormat != null) {
            builder.labelFormat(this.labelFormat);
        }
        if (this.width > 0) {
            builder.width(this.width);
        }
        return builder.build();
    }
}
