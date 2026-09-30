package ir.kootletland.ac;

import ir.kootletland.ac.command.KacCommand;
import ir.kootletland.ac.api.KootletLandACApiProvider;
import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class KootletLandAC extends JavaPlugin {
    private AntiCheatEngine engine;
    private KootletLandACApiProvider api;

    @Override public void onEnable() {
        saveDefaultConfig();
        engine = new AntiCheatEngine(this);
        api = new KootletLandACApiProvider(engine);
        getServer().getPluginManager().registerEvents(new PlayerListener(engine), this);
        KacCommand command = new KacCommand(this, engine);
        getCommand("kac").setExecutor(command);
        getCommand("kac").setTabCompleter(command);
        engine.start();
        diagnostics();
        getLogger().info("KootletLandAC "+getDescription().getVersion()+" enabled. Automatic punishment is permanently disabled.");
    }

    private void diagnostics() {
        String paper=getServer().getName()+" "+getServer().getBukkitVersion();
        String java=System.getProperty("java.version");
        getLogger().info("Diagnostics: "+paper+" | Java "+java+" | TPS "+String.format("%.2f",getServer().getTPS()[0]));
        for(String name:new String[]{"GrimAC","LiteBans","LuckPerms"}) {
            var plugin=getServer().getPluginManager().getPlugin(name);
            getLogger().info("Integration "+name+": "+(plugin==null?"not detected":"detected "+plugin.getDescription().getVersion()));
        }
        getLogger().info("Packet adapter: Bukkit event layer active; raw packet adapter unavailable.");
    }

    @Override public void onDisable() { if (engine != null) engine.shutdown(); }
    public AntiCheatEngine getEngine() { return engine; }
    public KootletLandACApiProvider getApi() { return api; }
}
