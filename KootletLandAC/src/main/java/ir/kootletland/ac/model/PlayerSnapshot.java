package ir.kootletland.ac.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public record PlayerSnapshot(Location position,Location previous,Vector velocity,double horizontalSpeed,double verticalDelta,double yawDelta,double pitchDelta,int ping,double tps,boolean onGround,boolean sprinting,boolean sneaking,boolean swimming,boolean inWater,boolean inLava,boolean inVehicle,boolean flying,GameMode gameMode,long ageTicks) {}
