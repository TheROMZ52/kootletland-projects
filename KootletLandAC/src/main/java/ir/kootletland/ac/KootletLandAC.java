package ir.kootletland.ac;

import ir.kootletland.ac.api.KootletLandACApi;
import ir.kootletland.ac.api.KootletLandACApiProvider;
import ir.kootletland.ac.command.KacCommand;
import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.engine.PacketPipeline;
import ir.kootletland.ac.integration.DetectedPluginIntegration;
import ir.kootletland.ac.integration.GrimIntegration;
import ir.kootletland.ac.listener.PlayerListener;
import ir.kootletland.ac.network.PluginMessageDetectionBridge;
import ir.kootletland.ac.packet.PacketAdapter;
import ir.kootletland.ac.packet.PaperEventPacketAdapter;
import ir.kootletland.ac.storage.NoopStorageProvider;
import ir.kootletland.ac.storage.SqlStorageProvider;
import ir.kootletland.ac.storage.StorageProvider;
import ir.kootletland.ac.wire.EvidenceMessage;
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

        engine.setStorage(createStorage());
        if (getConfig().getBoolean("network.enabled", false)) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, EvidenceMessage.CHANNEL);
            engine.setBridge(new PluginMessageDetectionBridge(this, getConfig().getString("network.server-name", "backend"),
                    getConfig().getDouble("network.min-confidence", 0.82)));
            getLogger().info("Network bridge enabled as '" + getConfig().getString("network.server-name", "backend") + "' (proxy aggregation).");
        }
        engine.start();
        adapter = new PaperEventPacketAdapter(this);
        adapter.start(new PacketPipeline(engine));
        engine.startIntegrations();
        diagnostics();
        getLogger().info("KootletLandAC " + getDescription().getVersion() + " enabled. Automatic punishment: "
                + (getConfig().getBoolean("settings.auto-punishment", false) ? "ENABLED by config" : "off (default)") + ".");
    }

    /** Storage is chosen once at startup; a failing database falls back to no storage instead of breaking detection. */
    private StorageProvider createStorage() {
        String type = getConfig().getString("storage.type", "none").toLowerCase();
        int queue = getConfig().getInt("storage.queue-size", 2000);
        int days = getConfig().getInt("storage.retention-days", 30);
        try {
            switch (type) {
                case "sqlite" -> {
                    return SqlStorageProvider.sqlite(new java.io.File(getDataFolder(), getConfig().getString("storage.sqlite.file", "evidence.db")), queue, days, getLogger());
                }
                case "mysql" -> {
                    return SqlStorageProvider.mysql(getConfig().getString("storage.mysql.host", "127.0.0.1"), getConfig().getInt("storage.mysql.port", 3306),
                            getConfig().getString("storage.mysql.database", "kootletac"), getConfig().getString("storage.mysql.user", "root"),
                            getConfig().getString("storage.mysql.password", ""), queue, days, getLogger());
                }
                default -> {
                    return new NoopStorageProvider();
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Storage '" + type + "' unavailable, continuing without storage: " + t.getMessage());
            return new NoopStorageProvider();
        }
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
        getLogger().info("Storage: " + engine.storage().name() + " | Network bridge: " + (getConfig().getBoolean("network.enabled", false) ? "enabled" : "off") + " | Profiler: " + (engine.profiler().enabled() ? "on" : "off"));
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
