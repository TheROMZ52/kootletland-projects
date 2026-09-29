package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class PhysicsEngine {
    public double expectedHorizontal(Player p, PlayerData d) {
        double base = p.isFlying() ? 0.75 : (p.isSprinting() ? 0.30 : 0.215);
        if (p.isSneaking()) base *= 0.3;
        if (p.isSwimming()) base *= 0.8;
        if (p.hasPotionEffect(PotionEffectType.SPEED)) base *= 1.0 + 0.2 * (p.getPotionEffect(PotionEffectType.SPEED).getAmplifier()+1);
        if (p.hasPotionEffect(PotionEffectType.SLOWNESS)) base *= Math.max(0.05, 1.0 - 0.15 * (p.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier()+1));
        Block b=p.getLocation().getBlock(); Material m=b.getType();
        if(m==Material.ICE||m==Material.PACKED_ICE||m==Material.BLUE_ICE||m==Material.FROSTED_ICE) base*=1.65;
        if(m==Material.SOUL_SAND||m==Material.SOUL_SOIL) base*=0.45;
        if(p.isInsideVehicle()) base*=2.2;
        if(d.velocity().lengthSquared()>0.01) base+=Math.min(0.9,d.velocity().clone().setY(0).length()*0.65);
        return base;
    }

    public boolean contextualGrace(Player p, PlayerData d) {
        if(p.isFlying() || p.getGameMode().isInvulnerable()) return true;
        if(System.nanoTime()-d.lastTeleportNanos()<800_000_000L) return true;
        if(System.nanoTime()-d.lastDamageNanos()<500_000_000L && d.velocity().lengthSquared()>0.03) return true;
        Material m=p.getLocation().getBlock().getType();
        return p.isInWater() || p.isInLava() || m==Material.SLIME_BLOCK || m==Material.HONEY_BLOCK;
    }
}
