package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PhysicsEngine {
    private final LagCompensationEngine lag;

    public PhysicsEngine(LagCompensationEngine lag){this.lag=lag;}

    public double expectedHorizontal(Player p,PlayerData d){
        if(p.isFlying()||p.getGameMode().isInvulnerable())return 1.0;
        double base=p.isSprinting() ? .285 : .215;
        if(p.isSneaking())base*=.30;
        if(p.isSwimming())base*=.80;
        if(p.isInWater())base*=.72;
        if(p.isInLava())base*=.48;
        PotionEffect speed=p.getPotionEffect(PotionEffectType.SPEED);
        if(speed!=null)base*=1.0+.20*(speed.getAmplifier()+1);
        PotionEffect slow=p.getPotionEffect(PotionEffectType.SLOWNESS);
        if(slow!=null)base*=Math.max(.05,1.0-.15*(slow.getAmplifier()+1));
        Material m=p.getLocation().getBlock().getType();
        Material below=p.getLocation().clone().subtract(0,1,0).getBlock().getType();
        Material support=supportBlock(p);
        if(isIce(m)||isIce(support))base*=1.55;
        if(m==Material.SOUL_SAND||m==Material.SOUL_SOIL)base*=.42;
        if(m==Material.POWDER_SNOW||below==Material.POWDER_SNOW)base*=.72;
        if(m==Material.BUBBLE_COLUMN)base*=.55;
        if(p.isInsideVehicle())base*=1.8;
        if(m==Material.LADDER||m==Material.VINE||m==Material.SCAFFOLDING)base*=.58;
        VectorLike v=new VectorLike(d.velocity().getX(),d.velocity().getZ());
        if(v.length()>.01)base+=Math.min(1.0,v.length()*.75);
        if(!p.isOnGround()&&d.verticalDelta()>0)base*=1.04;
        return Math.max(.05,base);
    }

    public boolean contextualGrace(Player p,PlayerData d){
        if(p.isFlying()||p.getGameMode().isInvulnerable()||p.isInsideVehicle())return true;
        long now=System.nanoTime();
        // Elytra and riptide tridents legitimately exceed every walking limit; keep a short tail
        // because momentum persists for a moment after gliding stops.
        if(p.isGliding()||p.isRiptiding()){d.touchGlide();return true;}
        if(now-d.lastGlideNanos()<1_500_000_000L)return true;
        if(p.hasPotionEffect(PotionEffectType.LEVITATION))return true;
        if(now-d.lastTeleportNanos()<900_000_000L)return true;
        if(now-d.lastDamageNanos()<650_000_000L&&d.velocity().lengthSquared()>.025)return true;
        Material m=p.getLocation().getBlock().getType();
        Material support=supportBlock(p);
        // Ice/slime/honey are usually the block *under* the player, not at the feet.
        if(isSlippery(m)||isSlippery(support)){d.touchSlippery();return true;}
        if(now-d.lastSlipperyNanos()<600_000_000L)return true;
        return p.isInWater()||p.isInLava();
    }

    public boolean combatGrace(Player p,PlayerData d){
        long now=System.nanoTime();
        return now-d.lastTeleportNanos()<700_000_000L||now-d.lastDamageNanos()<300_000_000L;
    }

    public double latencyFactor(Player p){
        return lag.tolerance(p);
    }

    /** Block that actually supports the player (feet block for partial blocks, block below for full ones). */
    public static Material supportBlock(Player p){
        return p.getLocation().clone().subtract(0,.2,0).getBlock().getType();
    }

    /** Extra vertical tolerance granted by the Jump Boost effect. */
    public static double jumpBonus(Player p){
        PotionEffect jump=p.getPotionEffect(PotionEffectType.JUMP_BOOST);
        return jump==null?0:.1*(jump.getAmplifier()+1);
    }

    private static boolean isSlippery(Material m){
        return m==Material.ICE||m==Material.PACKED_ICE||m==Material.BLUE_ICE||m==Material.FROSTED_ICE
                ||m==Material.SLIME_BLOCK||m==Material.HONEY_BLOCK;
    }

    private boolean isIce(Material m){
        return m==Material.ICE||m==Material.PACKED_ICE||m==Material.BLUE_ICE||m==Material.FROSTED_ICE;
    }

    private record VectorLike(double x,double z){
        double length(){return Math.hypot(x,z);}
    }
}
