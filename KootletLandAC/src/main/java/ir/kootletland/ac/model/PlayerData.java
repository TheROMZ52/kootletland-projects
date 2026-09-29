package ir.kootletland.ac.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;
    private final Deque<Evidence> evidence = new ArrayDeque<>();
    private Location last;
    private Location current;
    private Vector lastVelocity = new Vector();
    private long lastMovementNanos;
    private long lastAttackNanos;
    private long lastTeleportNanos;
    private long lastDamageNanos;
    private double violation;
    private double confidence;
    private int movementSamples;
    private int attackSamples;
    private double yawDelta;
    private double pitchDelta;
    private double horizontalSpeed;
    private double verticalDelta;
    private int consecutiveMovementAnomalies;
    private int consecutiveCombatAnomalies;
    private double lastExpectedSpeed;

    public PlayerData(Player player) { uuid=player.getUniqueId(); current=player.getLocation().clone(); last=current.clone(); lastMovementNanos=System.nanoTime(); }
    public UUID uuid(){return uuid;}
    public Location last(){return last;}
    public Location current(){return current;}
    public void update(Location next){ last=current; current=next.clone(); yawDelta=angleDelta(current.getYaw(),last.getYaw()); pitchDelta=current.getPitch()-last.getPitch(); horizontalSpeed=Math.hypot(current.getX()-last.getX(),current.getZ()-last.getZ()); verticalDelta=current.getY()-last.getY(); movementSamples++; }
    private double angleDelta(float a,float b){ double d=a-b; while(d>180)d-=360; while(d<-180)d+=360; return d; }
    public Vector velocity(){return lastVelocity;}
    public void velocity(Vector v){lastVelocity=v.clone();}
    public long lastMovementNanos(){return lastMovementNanos;}
    public long lastMovementTick(){return lastMovementTick;}
    public void movementTick(long tick){lastMovementTick=tick;}
    public long lastAttackTick(){return lastAttackTick;}
    public void attackTick(long tick){lastAttackTick=tick;}
    public void touchMovement(){lastMovementNanos=System.nanoTime();}
    public long lastAttackNanos(){return lastAttackNanos;}
    public void touchAttack(){lastAttackNanos=System.nanoTime(); attackSamples++;}
    public long lastTeleportNanos(){return lastTeleportNanos;}
    public void touchTeleport(){lastTeleportNanos=System.nanoTime();}
    public long lastDamageNanos(){return lastDamageNanos;}
    public void touchDamage(){lastDamageNanos=System.nanoTime();}
    public double violation(){return violation;}
    public double confidence(){return confidence;}
    public void addViolation(double v){violation=Math.min(100,Math.max(0,violation+v));}
    public void decay(double amount){violation=Math.max(0,violation-amount); confidence=Math.max(0,confidence-amount*0.03);}
    public void confidence(double c){confidence=Math.max(0,Math.min(1,c));}
    public Deque<Evidence> evidence(){return evidence;}
    public void addEvidence(Evidence e,int max){evidence.addFirst(e);while(evidence.size()>max)evidence.removeLast();}
    public double yawDelta(){return yawDelta;}
    public double pitchDelta(){return pitchDelta;}
    public double horizontalSpeed(){return horizontalSpeed;}
    public double verticalDelta(){return verticalDelta;}
    public int movementSamples(){return movementSamples;}
    public int attackSamples(){return attackSamples;}
    public int consecutiveMovementAnomalies(){return consecutiveMovementAnomalies;}
    public void movementAnomaly(boolean anomaly){consecutiveMovementAnomalies=anomaly?consecutiveMovementAnomalies+1:Math.max(0,consecutiveMovementAnomalies-1);}
    public int consecutiveCombatAnomalies(){return consecutiveCombatAnomalies;}
    public void combatAnomaly(boolean anomaly){consecutiveCombatAnomalies=anomaly?consecutiveCombatAnomalies+1:Math.max(0,consecutiveCombatAnomalies-1);}
    public double lastExpectedSpeed(){return lastExpectedSpeed;}
    public void lastExpectedSpeed(double v){lastExpectedSpeed=v;}
}
