package ir.kootletland.ac.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    private static final int DEVIATION_WINDOW=8;
    private static final int SPEED_WINDOW=6;

    private final UUID uuid;
    private final Deque<Evidence> evidence=new ArrayDeque<>();
    private final Deque<long[]> targets=new ArrayDeque<>();
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
    private long lastGlideNanos;
    private long lastSlipperyNanos;
    private long lastActionNanos;
    private double violation;
    private double confidence;
    private double behaviorScore;
    private int movementSamples;
    private int attackSamples;
    private double yawDelta;
    private double pitchDelta;
    private double horizontalSpeed;
    private double verticalDelta;
    private double lastExpectedSpeed;
    private double movementIntervalMs;
    private double attackIntervalMs;
    private int consecutiveMovementAnomalies;
    private int consecutiveCombatAnomalies;
    private double yawAcceleration;
    private double pitchAcceleration;
    private double previousYawDelta;
    private double previousPitchDelta;
    private int liquidSurfaceTicks;
    private final TimerBalance timer=new TimerBalance();
    private final StatsWindow speedWindow=new StatsWindow(60);
    private final StatsWindow moveIntervalWindow=new StatsWindow(60);
    private final StatsWindow attackWindow=new StatsWindow(40);
    private final StatsWindow actionWindow=new StatsWindow(40);
    private final RotationAnalyzer rotation=new RotationAnalyzer(40);
    private final Map<String,Long> actionCounts=new HashMap<>();

    // state engine
    private boolean clientOnGround;
    private boolean serverOnGround;
    private boolean wasServerOnGround;
    private boolean jumping;
    private boolean onClimbable;
    private boolean onSlime;
    private boolean onIce;
    private boolean inWater;
    private boolean inLava;
    private int groundSpoofTicks;
    private int transactionLatencyMs;
    private final Map<String,Integer> effects=new HashMap<>();

    // prediction
    private final VerticalMotionTracker vertical=new VerticalMotionTracker();
    private final boolean[] speedOver=new boolean[SPEED_WINDOW];
    private int speedOverNext;

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
            if(movementIntervalMs<250.0)moveIntervalWindow.add(movementIntervalMs);
        }
        yawDelta=angleDelta(current.getYaw(),last.getYaw());
        pitchDelta=current.getPitch()-last.getPitch();
        yawAcceleration=yawDelta-previousYawDelta;
        pitchAcceleration=pitchDelta-previousPitchDelta;
        previousYawDelta=yawDelta;
        previousPitchDelta=pitchDelta;
        if(yawDelta!=0||pitchDelta!=0)rotation.add(yawDelta,pitchDelta);
        horizontalSpeed=Math.hypot(current.getX()-last.getX(),current.getZ()-last.getZ());
        // only sample actual movement: standing still would drag the mean towards zero
        if(Double.isFinite(horizontalSpeed)&&horizontalSpeed>.02)speedWindow.add(horizontalSpeed);
        verticalDelta=current.getY()-last.getY();
        movementSamples++;
    }

    /** Refreshes the state model (ground, liquids, effects...) from the server. Call once per processed move. */
    public void refreshState(Player p){
        clientOnGround=p.isOnGround();
        wasServerOnGround=serverOnGround;
        serverOnGround=serverGround(p);
        inWater=p.isInWater();
        inLava=p.isInLava();
        Material feet=p.getLocation().getBlock().getType();
        Material support=p.getLocation().clone().subtract(0,.2,0).getBlock().getType();
        onSlime=feet==Material.SLIME_BLOCK||support==Material.SLIME_BLOCK;
        onIce=isIce(feet)||isIce(support);
        onClimbable=isClimbable(feet);
        jumping=wasServerOnGround&&!serverOnGround&&verticalDelta>.2;
        boolean spoof=clientOnGround&&!serverOnGround&&verticalDelta<-.08;
        groundSpoofTicks=spoof?Math.min(40,groundSpoofTicks+1):Math.max(0,groundSpoofTicks-1);
        // Transaction packets are not available on the Bukkit event layer, so ping is the closest proxy.
        transactionLatencyMs=Math.max(0,p.getPing());
        if(movementSamples%5==0){
            effects.clear();
            for(PotionEffect e:p.getActivePotionEffects())effects.put(e.getType().getKey().getKey(),e.getAmplifier());
        }
    }

    private boolean serverGround(Player p){
        BoundingBox b=p.getBoundingBox();
        BoundingBox under=new BoundingBox(b.getMinX()+.02,b.getMinY()-.08,b.getMinZ()+.02,b.getMaxX()-.02,b.getMinY()+.001,b.getMaxZ()-.02);
        return p.getWorld().hasCollisionsIn(under);
    }

    private static boolean isIce(Material m){
        return m==Material.ICE||m==Material.PACKED_ICE||m==Material.BLUE_ICE||m==Material.FROSTED_ICE;
    }

    private static boolean isClimbable(Material m){
        return m==Material.LADDER||m==Material.VINE||m==Material.SCAFFOLDING||m==Material.TWISTING_VINES
                ||m==Material.TWISTING_VINES_PLANT||m==Material.WEEPING_VINES||m==Material.WEEPING_VINES_PLANT
                ||m==Material.CAVE_VINES||m==Material.CAVE_VINES_PLANT;
    }

    private double angleDelta(float a,float b){
        double d=a-b;
        while(d>180)d-=360;
        while(d<-180)d+=360;
        return d;
    }

    // ---- state accessors
    public boolean clientOnGround(){return clientOnGround;}
    public boolean serverOnGround(){return serverOnGround;}
    public boolean jumping(){return jumping;}
    public boolean onClimbable(){return onClimbable;}
    public boolean onSlime(){return onSlime;}
    public boolean onIce(){return onIce;}
    public boolean inWater(){return inWater;}
    public boolean inLava(){return inLava;}
    public int groundSpoofTicks(){return groundSpoofTicks;}
    public int transactionLatencyMs(){return transactionLatencyMs;}
    public Map<String,Integer> effects(){return effects;}
    public boolean recentlyTeleported(){return System.nanoTime()-lastTeleportNanos<900_000_000L;}
    public boolean recentlyDamaged(){return System.nanoTime()-lastDamageNanos<650_000_000L;}
    public boolean recentlyKnockedBack(){return System.nanoTime()-lastVelocityNanos<900_000_000L&&lastVelocity.lengthSquared()>.025;}

    // ---- prediction state
    public VerticalMotionTracker vertical(){return vertical;}
    public double predictedDy(){return vertical.predicted();}
    public void predictedDy(double v){vertical.predicted(v);}
    public boolean wasServerOnGround(){return wasServerOnGround;}
    public void pushVerticalDeviation(double deviation){vertical.push(deviation);}
    public void clearVerticalDeviations(){vertical.clear();}
    public int deviationSamples(){return vertical.samples();}
    public int deviationsAbove(double threshold){return vertical.above(threshold);}

    public void pushSpeedOver(boolean over){
        speedOver[speedOverNext]=over;
        speedOverNext=(speedOverNext+1)%SPEED_WINDOW;
    }

    public int speedOverCount(){
        int n=0;
        for(boolean b:speedOver)if(b)n++;
        return n;
    }

    // ---- velocity / timing
    public Vector velocity(){return lastVelocity;}
    public void velocity(Vector v){
        lastVelocity=v.clone();
        lastVelocityNanos=System.nanoTime();
        vertical.applyVelocity(v.getY());
    }
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
                // long gaps are fight pauses, not clicking rhythm
                if(attackIntervalMs<1500.0)attackWindow.add(attackIntervalMs);
            }
        }
        lastAttackNanos=now;
        attackSamples++;
    }

    public void recordTarget(UUID target,long nowNanos){
        targets.addFirst(new long[]{nowNanos,target.getMostSignificantBits()^target.getLeastSignificantBits()});
        while(targets.size()>12)targets.removeLast();
    }

    /** Number of different entities hit within the window. */
    public int distinctTargets(long windowNanos,long nowNanos){
        java.util.Set<Long> ids=new java.util.HashSet<>();
        for(long[] t:targets){
            if(nowNanos-t[0]>windowNanos)break;
            ids.add(t[1]);
        }
        return ids.size();
    }

    public void recordAction(String type){
        long now=System.nanoTime();
        if(lastActionNanos>0)actionWindow.add((now-lastActionNanos)/1_000_000.0);
        lastActionNanos=now;
        actionCounts.merge(type,1L,Long::sum);
    }

    public Map<String,Long> actionCounts(){return actionCounts;}
    public StatsWindow actionWindow(){return actionWindow;}
    public RotationAnalyzer rotation(){return rotation;}
    public StatsWindow attackWindow(){return attackWindow;}
    public StatsWindow speedWindow(){return speedWindow;}

    public long lastGlideNanos(){return lastGlideNanos;}
    public void touchGlide(){lastGlideNanos=System.nanoTime();}
    public long lastSlipperyNanos(){return lastSlipperyNanos;}
    public void touchSlippery(){lastSlipperyNanos=System.nanoTime();}
    public int liquidSurfaceTicks(){return liquidSurfaceTicks;}
    public void liquidSurfaceTicks(int v){liquidSurfaceTicks=Math.max(0,Math.min(1000,v));}
    public TimerBalance timer(){return timer;}
    public long lastTeleportNanos(){return lastTeleportNanos;}
    public void touchTeleport(){lastTeleportNanos=System.nanoTime();timer.reset();clearVerticalDeviations();}
    public long lastDamageNanos(){return lastDamageNanos;}
    public void touchDamage(){lastDamageNanos=System.nanoTime();}
    public double violation(){return violation;}
    public double confidence(){return confidence;}
    public double behaviorScore(){return behaviorScore;}
    public void behaviorScore(double v){behaviorScore=Math.max(0,Math.min(1,v));}
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
    public double speedMean(){return speedWindow.mean();}
    public double speedVariance(){return speedWindow.variance();}
    public double speedStdDev(){return speedWindow.stdDev();}
    public long speedCount(){return speedWindow.size();}
    public double verticalDelta(){return verticalDelta;}
    public double lastExpectedSpeed(){return lastExpectedSpeed;}
    public void lastExpectedSpeed(double v){lastExpectedSpeed=v;}
    public double movementIntervalMs(){return movementIntervalMs;}
    public double attackIntervalMs(){return attackIntervalMs;}
    public double movementVariance(){return moveIntervalWindow.variance();}
    public double attackVariance(){return attackWindow.variance();}
    public double movementStdDev(){return moveIntervalWindow.stdDev();}
    public double attackStdDev(){return attackWindow.stdDev();}
    public double attackCoefficientOfVariation(){return attackWindow.coefficientOfVariation();}
    public int attackIntervalSamples(){return attackWindow.size();}
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
