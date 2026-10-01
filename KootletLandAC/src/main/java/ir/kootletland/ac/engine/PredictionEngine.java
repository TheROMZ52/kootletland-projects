package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import ir.kootletland.ac.model.VerticalMotionTracker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class PredictionEngine {
    public static final double GRAVITY=VerticalMotionTracker.GRAVITY;
    public static final double DRAG=VerticalMotionTracker.DRAG;
    public static final double JUMP_VELOCITY=VerticalMotionTracker.JUMP_VELOCITY;
    /** A single tick off the model means nothing; the model only speaks after this many of the last 8 ticks. */
    public static final double DEVIATION_THRESHOLD=VerticalMotionTracker.THRESHOLD;
    public static final int DEVIATION_REQUIRED=VerticalMotionTracker.REQUIRED;

    /** Expected vs actual state for one tick, explainable in evidence. */
    public record Prediction(double expectedX,double expectedY,double expectedZ,
                             double actualX,double actualY,double actualZ,
                             double horizontalDeviation,double verticalDeviation){}

    /** Vanilla vertical motion: y += vy; vy = (vy - 0.08) * 0.98. */
    public static double nextVelocity(double velocity){return VerticalMotionTracker.nextVelocity(velocity);}

    /**
     * Advances the vertical model by one processed tick and returns this tick's deviation (NaN when the model did not apply).
     * Adopting the observed velocity each tick keeps the
     * chain from drifting; a cheater hovering or rising shows up as a steady positive deviation instead.
     */
    public double step(Player p,PlayerData d,boolean graced,double lagTolerance){
        boolean skip=graced||d.onClimbable()||lagTolerance>.25||d.movementIntervalMs()>65.0||d.movementIntervalMs()<20.0;
        return d.vertical().step(d.verticalDelta(),d.serverOnGround(),d.wasServerOnGround(),skip,PhysicsEngine.jumpBonus(p));
    }

    /** True when the last 8 ticks were mostly above what gravity allows. */
    public boolean sustainedUpwardDeviation(PlayerData d){return d.vertical().sustainedUpward();}

    public Prediction predict(PlayerData d,double expectedHorizontal){
        Location cur=d.current(),prev=d.last();
        double dx=cur.getX()-prev.getX(),dz=cur.getZ()-prev.getZ();
        double len=Math.hypot(dx,dz);
        double scale=len>expectedHorizontal&&len>0?expectedHorizontal/len:1.0;
        double expectedDy=d.serverOnGround()?0.0:d.predictedDy();
        return new Prediction(prev.getX()+dx*scale,prev.getY()+expectedDy,prev.getZ()+dz*scale,
                cur.getX(),cur.getY(),cur.getZ(),
                Math.max(0,len-expectedHorizontal),d.verticalDelta()-expectedDy);
    }

    /** Blocks/effects that legitimately break the gravity model (cobweb, vines, liquids, honey, slow falling...). */
    public boolean verticalModelBroken(Player p){
        if(p.isInWater()||p.isInLava()||p.isSwimming())return true;
        if(p.hasPotionEffect(PotionEffectType.SLOW_FALLING)||p.hasPotionEffect(PotionEffectType.LEVITATION))return true;
        Material feet=p.getLocation().getBlock().getType();
        Material body=p.getLocation().clone().add(0,1,0).getBlock().getType();
        return slowsFall(feet)||slowsFall(body);
    }

    private boolean slowsFall(Material m){
        return m==Material.COBWEB||m==Material.LADDER||m==Material.VINE||m==Material.SCAFFOLDING
                ||m==Material.TWISTING_VINES||m==Material.TWISTING_VINES_PLANT||m==Material.WEEPING_VINES
                ||m==Material.WEEPING_VINES_PLANT||m==Material.CAVE_VINES||m==Material.CAVE_VINES_PLANT
                ||m==Material.SWEET_BERRY_BUSH||m==Material.POWDER_SNOW||m==Material.BUBBLE_COLUMN
                ||m==Material.HONEY_BLOCK||m==Material.SLIME_BLOCK;
    }
}
