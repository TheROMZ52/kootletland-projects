package ir.kootletland.acv;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ir.kootletland.ac.wire.EvidenceMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Central alert aggregation for KootletLandAC. Every backend keeps deciding on its own; this proxy plugin only
 * relays high-confidence evidence to staff who are on a different server. It never punishes anyone.
 */
@Plugin(id = "kootletlandac-velocity", name = "KootletLandAC-Velocity", version = "1.2.0", authors = {"TheROMZ52"})
public final class KootletLandACVelocity {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("kootletlandac", "evidence");
    private static final String PERMISSION = "kootletlandac.alerts.network";

    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public KootletLandACVelocity(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        logger.info("KootletLandAC proxy aggregation ready on channel {}", EvidenceMessage.CHANNEL);
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;
        // never forward this channel to players or servers
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        // only trust messages that arrive from a backend server connection, never from a client
        if (!(event.getSource() instanceof ServerConnection)) return;
        try {
            relay(EvidenceMessage.decode(event.getData()));
        } catch (IOException | RuntimeException e) {
            logger.warn("Dropped malformed KootletLandAC message: {}", e.getMessage());
        }
    }

    private void relay(EvidenceMessage m) {
        Component text = Component.text("[KootletAC/" + m.server() + "] ", NamedTextColor.RED)
                .append(Component.text(m.player(), NamedTextColor.WHITE))
                .append(Component.text(" -> " + m.check() + " confidence="
                        + Math.round(m.confidence() * 100) + "% VL=" + String.format("%.1f", m.violation()), NamedTextColor.GRAY));
        for (Player p : proxy.getAllPlayers()) {
            if (!p.hasPermission(PERMISSION)) continue;
            // the backend already alerted its own staff
            String current = p.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("");
            if (current.equalsIgnoreCase(m.server())) continue;
            p.sendMessage(text);
        }
        logger.info("[{}] {} {} confidence={}", m.server(), m.player(), m.check(), String.format("%.2f", m.confidence()));
    }
}
