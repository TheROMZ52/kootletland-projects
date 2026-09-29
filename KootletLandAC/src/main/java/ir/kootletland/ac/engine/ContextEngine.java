package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class ContextEngine {
    public double movementModifier(Player p, PlayerData d) {
        if (p.isFlying() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return 0;
        double grace = 0;
        if (p.isInsideVehicle()) grace += .45;
        if (p.isSwimming() || p.isInWater() || p.isInLava()) grace += .45;
        if (d.velocity().lengthSquared() > .04) grace += Math.min(.35, d.velocity().length() * .12);
        Material m = p.getLocation().getBlock().getType();
        if (m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE || m == Material.FROSTED_ICE) grace += .25;
        if (m == Material.SLIME_BLOCK || m == Material.HONEY_BLOCK) grace += .35;
        return Math.min(.9, grace);
    }

    public boolean invalid(Player p) {
        var l = p.getLocation();
        return !Double.isFinite(l.getX()) || !Double.isFinite(l.getY()) || !Double.isFinite(l.getZ())
                || Math.abs(l.getX()) > 3.0E7 || Math.abs(l.getZ()) > 3.0E7
                || l.getY() < -2048 || l.getY() > 2.0E7;
    }
}
