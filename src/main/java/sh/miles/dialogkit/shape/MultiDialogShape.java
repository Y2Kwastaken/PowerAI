package sh.miles.dialogkit.shape;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import sh.miles.dialogkit.element.ActionElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiDialogShape implements DialogShape {

    private final List<ActionElement> actions = new ArrayList<>(5);
    private ActionElement exitAction = null;
    private int columns = -1;

    public boolean addAction(ActionElement element) {
        Preconditions.checkArgument(element != null, "element must not be null");
        return this.actions.add(element);
    }

    public boolean addActions(ActionElement... elements) {
        for (final ActionElement element : elements) {
            if (!addAction(element)) {
                return false;
            }
        }
        return true;
    }

    public void setActions(Collection<ActionElement> elements) {
        this.actions.clear();
        this.actions.addAll(elements);
    }

    public void setExitAction(final ActionElement exitAction) {
        this.exitAction = exitAction;
    }

    public void setColumns(final int columns) {
        this.columns = columns;
    }

    @Override
    public DialogType toPaper() {
        final List<ActionButton> buttons = this.actions.stream().map(ActionElement::toPaper).toList();
        MultiActionType.Builder builder = DialogType.multiAction(buttons);
        if (this.exitAction != null) {
            builder.exitAction(this.exitAction.toPaper());
        }
        if (this.columns > 0) {
            builder.columns(this.columns);
        }
        return builder.build();
    }
}
