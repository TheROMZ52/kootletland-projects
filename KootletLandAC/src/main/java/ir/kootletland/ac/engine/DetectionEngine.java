package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;

public final class DetectionEngine {
    private final PhysicsEngine physics = new PhysicsEngine();

    public void movement(Player p, PlayerData d, java.util.function.Consumer<Evidence> sink) {
        if (physics.contextualGrace(p,d)) { d.movementAnomaly(false); return; }
        double expected=physics.expectedHorizontal(p,d);
        d.lastExpectedSpeed(expected);
        double actual=d.horizontalSpeed();
        double ratio=expected>0?actual/expected:0;
        boolean speed=ratio>1.28 && actual-expected>0.045;
        boolean fly=d.verticalDelta()>0.72 && !p.isJumping() && !p.isOnGround();
        boolean step=d.verticalDelta()>0.95 && d.horizontalSpeed()>0.08;
        boolean phase=!p.isOnGround() && p.getLocation().getBlock().getType().isSolid() && d.horizontalSpeed()>0.2;
        boolean invalid=!finite(d.current().getX())||!finite(d.current().getY())||!finite(d.current().getZ())||Math.abs(d.current().getY())>1.0E7;
        boolean timer=(System.nanoTime()-d.lastMovementNanos())<30_000_000L && d.movementSamples()>25;
        boolean anomaly=speed||fly||step||phase||invalid||timer;
        d.movementAnomaly(anomaly);
        if(!anomaly) return;
        double confidence=0.45;
        if(speed) confidence+=0.22*Math.min(1,(ratio-1.0)/0.8);
        if(fly) confidence+=0.16;
        if(step) confidence+=0.08;
        if(phase) confidence+=0.08;
        if(invalid) confidence=0.99;
        if(timer) confidence+=0.05;
        confidence=Math.min(0.995,confidence + Math.min(0.15,d.consecutiveMovementAnomalies()*0.01));
        double vl=Math.max(0.15,(confidence-0.55)*4.0);
        sink.accept(new Evidence(p.getName(), invalid?"InvalidMovement":speed?"Speed":fly?"Fly":step?"Step":"Movement",confidence,vl,Instant.now(),Map.of("expected",expected,"actual",actual,"ratio",ratio,"dx",d.current().getX()-d.last().getX(),"dy",d.verticalDelta(),"dz",d.current().getZ()-d.last().getZ())));
    }

    public void combat(Player attacker, Entity target, PlayerData d, java.util.function.Consumer<Evidence> sink) {
        if(!(target instanceof Player victim) || attacker.getGameMode().isInvulnerable()) return;
        Location eye=attacker.getEyeLocation(); double distance=eye.distance(victim.getBoundingBox().getCenter());
        boolean reach=distance>4.2 && distance<8.0;
        boolean aim=Math.abs(d.yawDelta())>70 && d.horizontalSpeed()<0.02;
        long interval=d.lastAttackNanos()==0?Long.MAX_VALUE:System.nanoTime()-d.lastAttackNanos();
        boolean click=interval<45_000_000L;
        if(!(reach||aim||click)) { d.combatAnomaly(false); return; }
        d.combatAnomaly(true);
        double c=0.48+(reach?0.22:0)+(aim?0.12:0)+(click?0.10:0)+Math.min(0.08,d.consecutiveCombatAnomalies()*0.01);
        c=Math.min(0.97,c);
        sink.accept(new Evidence(attacker.getName(),reach?"Reach":aim?"Aim":"AutoClicker",c,Math.max(0.1,(c-.5)*3),Instant.now(),Map.of("distance",distance,"yawDelta",d.yawDelta(),"attackIntervalMs",interval/1_000_000.0,"target",victim.getName())));
    }

    private boolean finite(double v){return Double.isFinite(v);}
}
