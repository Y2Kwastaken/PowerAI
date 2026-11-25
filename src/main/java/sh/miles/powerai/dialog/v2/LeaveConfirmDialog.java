package sh.miles.powerai.dialog.v2;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import sh.miles.dialogkit.DialogScreen;
import sh.miles.dialogkit.element.CustomElement;
import sh.miles.dialogkit.shape.BinaryDialogShape;

import java.util.function.Consumer;

import static io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.NONE;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static sh.miles.powerai.dialog.v2.Cutiepie.component;

final class LeaveConfirmDialog extends DialogScreen {

    private static final String TITLE = "Action Confirmation";

    private static final String CONFIRM_ACTION = "<red>Confirm Exit";
    private static final String CONFIRM_TOOLTIP = "confirming leaves without saving any progress";
    private static final String CANCEL_ACTION = "Go Back";
    private static final String CANCEL_TOOLTIP = "returns to your previous work";

    private static final Consumer<Player> NAN = Audience::closeDialog;

    private final Consumer<Player> confirm;
    private final Consumer<Player> restore;

    LeaveConfirmDialog(String message, final Consumer<Player> restore) {
        this(message, NAN, restore);
    }

    LeaveConfirmDialog(String message, final Consumer<Player> confirm, final Consumer<Player> restore) {
        this.confirm = confirm;
        this.restore = restore;

        setTitle(component(TITLE));
        setCloseWithEscape(false);
        setAfterAction(NONE);
        setShape(new LeaveConfirmShape());

        addText(component(message));
    }

    private class LeaveConfirmShape extends BinaryDialogShape {
        LeaveConfirmShape() {
            setOptionOne(new ConfirmExitElement());
            setOptionTwo(new GoBackElement());
        }
    }

    private class ConfirmExitElement extends CustomElement {
        ConfirmExitElement() {
            setText(component(CONFIRM_ACTION));
            setTooltip(component(CONFIRM_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            confirm.accept(assertPlayer(audience));
        }
    }

    private class GoBackElement extends CustomElement {
        GoBackElement() {
            setText(component(CANCEL_ACTION));
            setTooltip(component(CANCEL_TOOLTIP));
        }

        @Override
        public void accept(DialogResponseView response, Audience audience) {
            restore.accept(assertPlayer(audience));
        }
    }
}
