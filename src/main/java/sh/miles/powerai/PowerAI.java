package sh.miles.powerai;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import sh.miles.powerai.command.ScriptsCommand;
import sh.miles.powerai.command.v2.ScriptsCommandV2;
import sh.miles.powerai.config.LuaConfig;
import sh.miles.powerai.data.DataStore;
import sh.miles.powerai.listener.PlayerQuitListener;
import sh.miles.powerai.lua.LuaScriptExecutor;
import sh.miles.powerai.lua.global.LuaGlobals;

import static sh.miles.powerai.util.Swallow.swallow;

public final class PowerAI extends JavaPlugin {

    private LuaScriptExecutor executor;
    private DataStore store;

    @Override
    public void onEnable() {
        LuaConfig luaConfig = LuaConfig.load(this);
        this.executor = new LuaScriptExecutor(getSLF4JLogger(),
                luaConfig.runtimePerTickMillis(),
                luaConfig.scriptAllowancePerTickMillis(),
                luaConfig.totalScriptAllowanceMillis(),
                LuaGlobals::std
        );
        this.store = new DataStore(this);
        store.connect();

        final ScriptsCommand scriptsCommand = new ScriptsCommand(this.executor, this.store.getMarketplace());
        final ScriptsCommandV2 scriptsCommandV2 = new ScriptsCommandV2();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(scriptsCommand.build(), "Open the script manager, or run, list and stop your scripts");
            event.registrar().register(scriptsCommandV2.build(store.getMarketplace()), "Test command for work in progress features");
        });

        getServer().getScheduler().runTaskTimer(this, executor, 1L, 1L);

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this.executor), this);
    }

    @Override
    public void onDisable() {
        if (this.executor != null) {
            swallow(() -> this.executor.stop());
        }
        if (this.store != null) {
            this.store.close();
        }
    }
}
