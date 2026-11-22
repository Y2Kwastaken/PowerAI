package sh.miles.dialogkit.element;

import com.google.common.base.Preconditions;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public abstract class CustomElement implements ActionElement, DialogActionCallback {

    private Component text = null;
    private Component tooltip = null;
    private int width = -1;

    public CustomElement() {
    }

    @Override
    public void accept(final DialogResponseView response, final Audience audience) {
    }

    protected Player assertPlayer(Audience audience) {
        if (audience instanceof Player player) {
            return player;
        } else {
            throw new IllegalStateException("non player interacting with dialog");
        }
    }

    public void setText(final Component text) {
        this.text = text;
    }

    public void setTooltip(final Component tooltip) {
        this.tooltip = tooltip;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    @Override
    public ActionButton toPaper() {
        Preconditions.checkArgument(text != null, "text must not be null");
        ActionButton.Builder builder = ActionButton.builder(text);
        if (this.tooltip != null) {
            builder.tooltip(this.tooltip);
        }
        if (this.width > 0) {
            builder.width(this.width);
        }
        builder.action(DialogAction.customClick(
                this, ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(Duration.of(5, ChronoUnit.MINUTES))
                        .build()
        ));
        return builder.build();
    }
}
