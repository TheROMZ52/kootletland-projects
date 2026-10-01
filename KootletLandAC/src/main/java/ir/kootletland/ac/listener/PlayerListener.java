package ir.kootletland.ac.listener;

import ir.kootletland.ac.engine.AntiCheatEngine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Lifecycle only. Gameplay events arrive through the packet adapter. */
public final class PlayerListener implements Listener {
    private final AntiCheatEngine engine;
    public PlayerListener(AntiCheatEngine engine){this.engine=engine;}
    @EventHandler public void join(PlayerJoinEvent e){engine.add(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){engine.remove(e.getPlayer().getUniqueId());}
}
