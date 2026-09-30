package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerData;
import org.bukkit.entity.Player;

public final class PredictionEngine {
    public double expectedVertical(Player player, PlayerData data) {
        double velocityY=data.velocity().getY();
        if(player.isOnGround()) return 0;
        if(player.isInWater()||player.isInLava()||player.isFlying()||player.isInsideVehicle()) return velocityY;
        return velocityY-.08;
    }

    public double verticalDeviation(Player player, PlayerData data) {
        return Math.abs(data.verticalDelta()-expectedVertical(player,data));
    }

    public boolean anomalousVertical(Player player, PlayerData data) {
        if(player.isOnGround()||player.isInWater()||player.isInLava()||player.isFlying()||player.isInsideVehicle()) return false;
        if(data.movementSamples()<8) return false;
        double bonus=PhysicsEngine.jumpBonus(player);
        return verticalDeviation(player,data)>.45+bonus&&Math.abs(data.verticalDelta())>.28+bonus;
    }
}
