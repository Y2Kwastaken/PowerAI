package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.NoticeDialogShape;

final class ConfirmDialogShape extends NoticeDialogShape {
    private static final Component CONFIRM_ACTION = Component.text("Confirm");

    ConfirmDialogShape() {
        setAction(new CloseAction());
    }

    private static class CloseAction extends CustomElement {
        CloseAction() {
            setText(CONFIRM_ACTION);
        }
        @Override
        public void accept(DialogResponseView response, Audience audience) {
            audience.closeDialog();
        }
    }
}
