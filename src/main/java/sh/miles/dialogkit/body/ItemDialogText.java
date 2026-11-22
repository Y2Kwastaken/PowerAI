package sh.miles.dialogkit.body;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public class ItemDialogText implements DialogText {

    private ItemStack item = null;
    private Component text = null;
    private boolean showDecorations = true;
    private boolean showTooltip = true;
    private int textWidth = -1;
    private int width = -1;
    private int height = -1;

    public void setItem(final ItemStack item) {
        this.item = item;
    }

    public void setText(final Component text) {
        this.text = text;
    }

    public void setShowDecorations(final boolean showDecorations) {
        this.showDecorations = showDecorations;
    }

    public void setShowTooltip(final boolean showTooltip) {
        this.showTooltip = showTooltip;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setTextWidth(final int textWidth) {
        this.textWidth = textWidth;
    }

    @Override
    public DialogBody toPaper() {
        Preconditions.checkArgument(item != null, "item must not be null");
        ItemDialogBody.Builder builder = DialogBody.item(item);
        if (text != null) {
            if (textWidth > 0) {
                builder.description(DialogBody.plainMessage(this.text, this.textWidth));
            } else {
                builder.description(DialogBody.plainMessage(this.text));
            }
        }
        builder.showDecorations(this.showDecorations);
        builder.showTooltip(this.showTooltip);
        if (this.height > 0) {
            builder.height(this.height);
        }
        if (this.width > 0) {
            builder.width(this.width);
        }
        return builder.build();
    }
}
