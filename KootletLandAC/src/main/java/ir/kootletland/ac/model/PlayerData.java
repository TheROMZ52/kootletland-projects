package ir.kootletland.ac.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;
    private final Deque<Evidence> evidence=new ArrayDeque<>();
    private final Deque<Double> attackIntervals=new ArrayDeque<>();
    private Location last;
    private Location current;
    private Vector lastVelocity=new Vector();
    private long lastMovementNanos;
    private long lastAttackNanos;
    private long lastTeleportNanos;
    private long lastDamageNanos;
    private long lastVelocityNanos;
    private long lastMovementTick;
    private long lastAttackTick;
    private double violation;
    private double confidence;
    private int movementSamples;
    private int attackSamples;
    private double yawDelta;
    private double pitchDelta;
    private double horizontalSpeed;
    private double speedMean;
    private double speedM2;
    private long speedCount;
    private double verticalDelta;
    private double lastExpectedSpeed;
    private double movementIntervalMs;
    private double attackIntervalMs;
    private double movementMean;
    private double movementM2;
    private long movementCount;
    private double attackMean;
    private double attackM2;
    private long attackCount;
    private int consecutiveMovementAnomalies;
    private int consecutiveCombatAnomalies;
    private double yawAcceleration;
    private double pitchAcceleration;
    private double previousYawDelta;
    private double previousPitchDelta;

    public PlayerData(Player player){
        uuid=player.getUniqueId();
        current=player.getLocation().clone();
        last=current.clone();
        lastMovementNanos=System.nanoTime();
    }

    public UUID uuid(){return uuid;}
    public Location last(){return last;}
    public Location current(){return current;}

    public void update(Location next){
        long now=System.nanoTime();
        last=current;
        current=next.clone();
        long delta=now-lastMovementNanos;
        if(delta>0){
            movementIntervalMs=delta/1_000_000.0;
            movementCount++;
            double diff=movementIntervalMs-movementMean;
            movementMean+=diff/movementCount;
            movementM2+=diff*(movementIntervalMs-movementMean);
        }
        yawDelta=angleDelta(current.getYaw(),last.getYaw());
        pitchDelta=current.getPitch()-last.getPitch();
        yawAcceleration=yawDelta-previousYawDelta;
        pitchAcceleration=pitchDelta-previousPitchDelta;
        previousYawDelta=yawDelta;
        previousPitchDelta=pitchDelta;
        horizontalSpeed=Math.hypot(current.getX()-last.getX(),current.getZ()-last.getZ());
        if(Double.isFinite(horizontalSpeed)){
            speedCount++;
            double diff=horizontalSpeed-speedMean;
            speedMean+=diff/speedCount;
            speedM2+=diff*(horizontalSpeed-speedMean);
        }
        verticalDelta=current.getY()-last.getY();
        movementSamples++;
    }

    private double angleDelta(float a,float b){
        double d=a-b;
        while(d>180)d-=360;
        while(d<-180)d+=360;
        return d;
    }

    public Vector velocity(){return lastVelocity;}
    public void velocity(Vector v){lastVelocity=v.clone();lastVelocityNanos=System.nanoTime();}
    public long lastVelocityNanos(){return lastVelocityNanos;}
    public long lastMovementNanos(){return lastMovementNanos;}
    public long lastMovementTick(){return lastMovementTick;}
    public void movementTick(long tick){lastMovementTick=tick;}
    public long lastAttackTick(){return lastAttackTick;}
    public void attackTick(long tick){lastAttackTick=tick;}
    public void touchMovement(){lastMovementNanos=System.nanoTime();}
    public long lastAttackNanos(){return lastAttackNanos;}

    public void touchAttack(){
        long now=System.nanoTime();
        if(lastAttackNanos>0){
            long delta=now-lastAttackNanos;
            if(delta>0){
                attackIntervalMs=delta/1_000_000.0;
                attackCount++;
                double diff=attackIntervalMs-attackMean;
                attackMean+=diff/attackCount;
                attackM2+=diff*(attackIntervalMs-attackMean);
                attackIntervals.addFirst(attackIntervalMs);
                while(attackIntervals.size()>40)attackIntervals.removeLast();
            }
        }
        lastAttackNanos=now;
        attackSamples++;
    }

    public long lastTeleportNanos(){return lastTeleportNanos;}
    public void touchTeleport(){lastTeleportNanos=System.nanoTime();}
    public long lastDamageNanos(){return lastDamageNanos;}
    public void touchDamage(){lastDamageNanos=System.nanoTime();}
    public double violation(){return violation;}
    public double confidence(){return confidence;}
    public void addViolation(double v){violation=Math.min(100,Math.max(0,violation+Math.max(0,v)));}
    public void decay(double amount){violation=Math.max(0,violation-amount);confidence=Math.max(0,confidence-amount*.03);}
    public void confidence(double c){confidence=Math.max(0,Math.min(1,c));}
    public Deque<Evidence> evidence(){return evidence;}
    public void addEvidence(Evidence e,int max){evidence.addFirst(e);while(evidence.size()>max)evidence.removeLast();}
    public double yawDelta(){return yawDelta;}
    public double pitchDelta(){return pitchDelta;}
    public double yawAcceleration(){return yawAcceleration;}
    public double pitchAcceleration(){return pitchAcceleration;}
    public double horizontalSpeed(){return horizontalSpeed;}
    public double speedMean(){return speedMean;}
    public double speedVariance(){return speedCount>1?speedM2/(speedCount-1):0;}
    public double speedStdDev(){return Math.sqrt(speedVariance());}
    public long speedCount(){return speedCount;}
    public double verticalDelta(){return verticalDelta;}
    public double lastExpectedSpeed(){return lastExpectedSpeed;}
    public void lastExpectedSpeed(double v){lastExpectedSpeed=v;}
    public double movementIntervalMs(){return movementIntervalMs;}
    public double attackIntervalMs(){return attackIntervalMs;}
    public double movementVariance(){return movementCount>1?movementM2/(movementCount-1):0;}
    public double attackVariance(){return attackCount>1?attackM2/(attackCount-1):0;}
    public double movementStdDev(){return Math.sqrt(movementVariance());}
    public double attackStdDev(){return Math.sqrt(attackVariance());}
    public double attackCoefficientOfVariation(){return attackMean>0?attackStdDev()/attackMean:0;}
    public int attackIntervalSamples(){return attackIntervals.size();}
    public int movementSamples(){return movementSamples;}
    public int attackSamples(){return attackSamples;}
    public PlayerSnapshot snapshot(Player player,long ageTicks){
        return new PlayerSnapshot(current.clone(),last.clone(),lastVelocity.clone(),horizontalSpeed,verticalDelta,yawDelta,pitchDelta,
                player.getPing(),Bukkit.getTPS()[0],player.isOnGround(),player.isSprinting(),player.isSneaking(),
                player.isSwimming(),player.isInWater(),player.isInLava(),player.isInsideVehicle(),player.isFlying(),
                player.getGameMode(),ageTicks);
    }
    public int consecutiveMovementAnomalies(){return consecutiveMovementAnomalies;}
    public void movementAnomaly(boolean anomaly){consecutiveMovementAnomalies=anomaly?Math.min(100,consecutiveMovementAnomalies+1):Math.max(0,consecutiveMovementAnomalies-1);}
    public int consecutiveCombatAnomalies(){return consecutiveCombatAnomalies;}
    public void combatAnomaly(boolean anomaly){consecutiveCombatAnomalies=anomaly?Math.min(100,consecutiveCombatAnomalies+1):Math.max(0,consecutiveCombatAnomalies-1);}
}
