package sh.miles.powerai.listener;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import sh.miles.powerai.lua.LuaScriptExecutor;

public class PlayerQuitListener implements Listener {

    private final LuaScriptExecutor executor;
    public PlayerQuitListener(LuaScriptExecutor executor) {
        this.executor = executor;
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        this.executor.cancelOwnedBy(event.getPlayer().getUniqueId());
    }

}
