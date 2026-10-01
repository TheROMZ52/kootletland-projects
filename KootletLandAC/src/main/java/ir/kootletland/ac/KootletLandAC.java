package ir.kootletland.ac;

import ir.kootletland.ac.api.KootletLandACApi;
import ir.kootletland.ac.api.KootletLandACApiProvider;
import ir.kootletland.ac.command.KacCommand;
import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.engine.PacketPipeline;
import ir.kootletland.ac.integration.DetectedPluginIntegration;
import ir.kootletland.ac.integration.GrimIntegration;
import ir.kootletland.ac.listener.PlayerListener;
import ir.kootletland.ac.packet.PacketAdapter;
import ir.kootletland.ac.packet.PaperEventPacketAdapter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class KootletLandAC extends JavaPlugin {
    private AntiCheatEngine engine;
    private KootletLandACApiProvider api;
    private PacketAdapter adapter;
    private GrimIntegration grim;

    @Override public void onEnable() {
        saveDefaultConfig();
        engine = new AntiCheatEngine(this);
        api = new KootletLandACApiProvider(engine);
        getServer().getServicesManager().register(KootletLandACApi.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new PlayerListener(engine), this);

        KacCommand command = new KacCommand(this, engine);
        getCommand("kac").setExecutor(command);
        getCommand("kac").setTabCompleter(command);

        grim = new GrimIntegration(this, engine);
        engine.registerIntegration(grim);
        engine.registerIntegration(new DetectedPluginIntegration(this, "LiteBans", "punishment commands run through the console, so LiteBans records them like any other staff action."));
        engine.registerIntegration(new DetectedPluginIntegration(this, "LuckPerms", "kootletlandac.bypass / kootletlandac.alerts are normal permission nodes LuckPerms resolves."));

        engine.start();
        adapter = new PaperEventPacketAdapter(this);
        adapter.start(new PacketPipeline(engine));
        engine.startIntegrations();
        diagnostics();
        getLogger().info("KootletLandAC " + getDescription().getVersion() + " enabled. Automatic punishment: "
                + (getConfig().getBoolean("settings.auto-punishment", false) ? "ENABLED by config" : "off (default)") + ".");
    }

    private void diagnostics() {
        getLogger().info("Diagnostics: " + getServer().getName() + " " + getServer().getBukkitVersion()
                + " | Java " + System.getProperty("java.version") + " | TPS " + String.format("%.2f", getServer().getTPS()[0]));
        for (String name : new String[]{"GrimAC", "LiteBans", "LuckPerms"}) {
            var plugin = getServer().getPluginManager().getPlugin(name);
            getLogger().info("Integration " + name + ": " + (plugin == null ? "not detected" : "detected " + plugin.getDescription().getVersion()));
        }
        if (getServer().getPluginManager().getPlugin("GrimAC") != null) {
            getLogger().info("Grim bridge: " + grim.status());
        }
        getLogger().info("Packet integration: " + adapter.name() + (adapter.isAvailable() ? " OK" : " UNAVAILABLE")
                + (adapter.providesRawPackets() ? "" : " (raw packets / transactions: NOT IMPLEMENTED)"));
        boolean movementOk = !engine.faultedChecks().contains("movement");
        boolean combatOk = !engine.faultedChecks().contains("combat");
        getLogger().info("Movement engine: " + (movementOk ? "OK" : "FAULTED") + " | Combat engine: " + (combatOk ? "OK" : "FAULTED")
                + " | Behavior, lag compensation, decision engines: OK | Enabled checks: " + engine.enabledChecks());
    }

    @Override public void onDisable() {
        if (adapter != null) adapter.stop();
        if (engine != null) engine.shutdown();
    }

    public AntiCheatEngine getEngine() { return engine; }
    public KootletLandACApiProvider getApi() { return api; }
}
