package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public final class PhysicsEngine {
    public double expectedHorizontal(Player p,PlayerData d){
        if(p.isFlying()||p.getGameMode().isInvulnerable())return 1.0;
        double base=p.isSprinting() ? .285 : .215;
        if(p.isSneaking())base*=.30;
        if(p.isSwimming())base*=.80;
        if(p.isInWater())base*=.72;
        if(p.isInLava())base*=.48;
        PotionEffect speed=p.getPotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        if(speed!=null)base*=1.0+.20*(speed.getAmplifier()+1);
        PotionEffect slow=p.getPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        if(slow!=null)base*=Math.max(.05,1.0-.15*(slow.getAmplifier()+1));
        Material m=p.getLocation().getBlock().getType();
        Material below=p.getLocation().clone().subtract(0,1,0).getBlock().getType();
        if(isIce(m))base*=1.55;
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
        if(now-d.lastTeleportNanos()<900_000_000L)return true;
        if(now-d.lastDamageNanos()<650_000_000L&&d.velocity().lengthSquared()>.025)return true;
        Material m=p.getLocation().getBlock().getType();
        return p.isInWater()||p.isInLava()||m==Material.SLIME_BLOCK||m==Material.HONEY_BLOCK||isIce(m);
    }

    public boolean combatGrace(Player p,PlayerData d){
        long now=System.nanoTime();
        return now-d.lastTeleportNanos()<700_000_000L||now-d.lastDamageNanos()<300_000_000L;
    }

    public double latencyFactor(Player p){
        return Math.min(.28,Math.max(0,p.getPing())/350.0);
    }

    private boolean isIce(Material m){
        return m==Material.ICE||m==Material.PACKED_ICE||m==Material.BLUE_ICE||m==Material.FROSTED_ICE;
    }

    private record VectorLike(double x,double z){
        double length(){return Math.hypot(x,z);}
    }
}
