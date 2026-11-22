package sh.miles.dialogkit.shape;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import sh.miles.dialogkit.element.ActionElement;

public class BinaryDialogShape implements DialogShape {

    private ActionElement optionOne = null;
    private ActionElement optionTwo = null;

    public void setOptionOne(final ActionElement optionOne) {
        this.optionOne = optionOne;
    }

    public void setOptionTwo(final ActionElement optionTwo) {
        this.optionTwo = optionTwo;
    }

    @Override
    public DialogType toPaper() {
        Preconditions.checkArgument(optionOne != null && optionTwo != null, "Both options must be non null for BinaryDialogShape");

        return DialogType.confirmation(
                this.optionOne.toPaper(),
                this.optionTwo.toPaper()
        );
    }
}
