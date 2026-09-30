package ir.kootletland.ac.listener;

import ir.kootletland.ac.engine.AntiCheatEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public final class PlayerListener implements Listener {
    private final AntiCheatEngine engine;
    public PlayerListener(AntiCheatEngine engine){this.engine=engine;}
    @EventHandler public void join(PlayerJoinEvent e){engine.add(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){engine.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void move(PlayerMoveEvent e){if(e.getTo()!=null) engine.move(e.getPlayer());}
    @EventHandler public void attack(EntityDamageByEntityEvent e){
        // Only real melee hits. Sweep, thorns and other indirect damage would fake 0ms intervals and long reach.
        if(e.getCause()!=EntityDamageEvent.DamageCause.ENTITY_ATTACK)return;
        if(e.getDamager() instanceof Player p) engine.attack(p,e.getEntity());
    }
    @EventHandler public void velocity(PlayerVelocityEvent e){engine.velocity(e.getPlayer(),e.getVelocity());}
    @EventHandler public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p) engine.damage(p);}
    @EventHandler public void teleport(PlayerTeleportEvent e){engine.teleport(e.getPlayer());}
}
