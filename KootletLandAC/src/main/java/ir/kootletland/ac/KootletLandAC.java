package ir.kootletland.ac;

import ir.kootletland.ac.command.KacCommand;
import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class KootletLandAC extends JavaPlugin {
    private AntiCheatEngine engine;

    @Override public void onEnable() {
        saveDefaultConfig();
        engine = new AntiCheatEngine(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(engine), this);
        KacCommand command = new KacCommand(this, engine);
        getCommand("kac").setExecutor(command);
        getCommand("kac").setTabCompleter(command);
        engine.start();
        getLogger().info("KootletLandAC 1.0.0 enabled. Automatic punishment is permanently disabled.");
    }

    @Override public void onDisable() { if (engine != null) engine.shutdown(); }
    public AntiCheatEngine getEngine() { return engine; }
}
