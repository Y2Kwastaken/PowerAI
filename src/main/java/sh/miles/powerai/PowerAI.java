package sh.miles.powerai;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import sh.miles.powerai.command.ScriptsCommand;
import sh.miles.powerai.config.LuaConfig;
import sh.miles.powerai.listener.PlayerQuitListener;
import sh.miles.powerai.lua.global.LuaGlobals;
import sh.miles.powerai.lua.LuaScriptExecutor;

import static sh.miles.powerai.util.Swallow.swallow;

public final class PowerAI extends JavaPlugin {

    private LuaScriptExecutor executor;

    @Override
    public void onEnable() {
        LuaConfig luaConfig = LuaConfig.load(this);
        this.executor = new LuaScriptExecutor(getSLF4JLogger(),
                luaConfig.runtimePerTickMillis(),
                luaConfig.scriptAllowancePerTickMillis(),
                luaConfig.totalScriptAllowanceMillis(),
                LuaGlobals::std
        );

        final ScriptsCommand scriptsCommand = new ScriptsCommand(this.executor);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(scriptsCommand.build(), "Open the script manager, or run, list and stop your scripts");
        });

        getServer().getScheduler().runTaskTimer(this, executor, 1L, 1L);
        
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this.executor), this);
    }

    @Override
    public void onDisable() {
        if (this.executor != null) {
            swallow(() -> this.executor.stop());
        }
    }
}
