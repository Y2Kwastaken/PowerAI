package sh.miles.dialogkit.body;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;

public class PureDialogText implements DialogText {

    private Component text = null;
    private int width = -1;

    public void setText(final Component text) {
        this.text = text;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    @Override
    public DialogBody toPaper() {
        Preconditions.checkArgument(text != null, "Text must not be null");
        if (width > 0) {
            return DialogBody.plainMessage(this.text, this.width);
        }

        return DialogBody.plainMessage(this.text);
    }
}
