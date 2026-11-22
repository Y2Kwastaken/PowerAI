package sh.miles.dialogkit.shape;

import io.papermc.paper.registry.data.dialog.type.DialogType;
import sh.miles.dialogkit.element.ActionElement;

public class NoticeDialogShape implements DialogShape {

    private ActionElement action;

    public void setAction(final ActionElement action) {
        this.action = action;
    }

    @Override
    public DialogType toPaper() {
        if (action == null) {
            return DialogType.notice();
        }
        return DialogType.notice(
                this.action.toPaper()
        );
    }
}
