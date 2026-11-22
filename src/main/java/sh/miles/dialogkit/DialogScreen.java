package sh.miles.dialogkit;

import com.google.common.base.Preconditions;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import sh.miles.dialogkit.body.DialogText;
import sh.miles.dialogkit.body.ItemDialogText;
import sh.miles.dialogkit.body.PureDialogText;
import sh.miles.dialogkit.input.DialogField;
import sh.miles.dialogkit.shape.DialogShape;

import java.util.ArrayList;
import java.util.List;

public class DialogScreen {

    private Component title;
    private DialogShape shape;
    private boolean closeWithEscape = true;
    private DialogBase.DialogAfterAction afterAction = null;

    private List<DialogText> body = new ArrayList<>(5);
    private List<DialogField> inputs = new ArrayList<>(5);

    public void addText(Component text) {
        PureDialogText pure = new PureDialogText();
        pure.setText(text);

        body.add(pure);
    }

    public void addText(Component text, int width) {
        PureDialogText pure = new PureDialogText();
        pure.setText(text);
        pure.setWidth(width);

        body.add(pure);
    }

    public void addText(ItemStack item) {
        ItemDialogText text = new ItemDialogText();
        text.setItem(item);

        body.add(text);
    }

    public void addText(ItemStack item, Component text) {
        ItemDialogText itemText = new ItemDialogText();
        itemText.setItem(item);
        itemText.setText(text);

        this.body.add(itemText);
    }

    public void addText(DialogText text) {
        this.body.add(text);
    }

    public void addInput(DialogField input) {
        this.inputs.add(input);
    }

    public void setTitle(final Component title) {
        this.title = title;
    }

    public void setShape(final DialogShape shape) {
        this.shape = shape;
    }

    public void setCloseWithEscape(final boolean closeWithEscape) {
        this.closeWithEscape = closeWithEscape;
    }

    public void setAfterAction(final DialogBase.DialogAfterAction afterAction) {
        this.afterAction = afterAction;
    }

    public Dialog compile() {
        Preconditions.checkArgument(title != null, "title must not be null");
        Preconditions.checkArgument(shape != null, "shape must not be null");

        DialogBase.Builder base = DialogBase.builder(this.title);
        base.canCloseWithEscape(this.closeWithEscape);
        if (this.afterAction != null) {
            base.afterAction(this.afterAction);
        }
        base.body(this.body.stream().map(DialogText::toPaper).toList());
        base.inputs(this.inputs.stream().map(DialogField::toPaper).toList());

        final DialogBase builtBase = base.build();
        return Dialog.create(factory -> factory.empty()
                .base(builtBase)
                .type(this.shape.toPaper())
        );
    }
}
