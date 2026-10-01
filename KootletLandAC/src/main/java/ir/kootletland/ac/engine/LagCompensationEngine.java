package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.StatsWindow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks ping history, jitter and server tick health so checks can widen tolerances fairly. */
public final class LagCompensationEngine {
    private final Map<UUID,StatsWindow> pings=new ConcurrentHashMap<>();

    public void sample(Player p){
        pings.computeIfAbsent(p.getUniqueId(),id->new StatsWindow(30)).add(Math.max(0,p.getPing()));
    }

    public void remove(UUID id){pings.remove(id);}

    public double averagePing(Player p){
        StatsWindow w=pings.get(p.getUniqueId());
        return w==null||w.size()==0?Math.max(0,p.getPing()):w.mean();
    }

    public double jitter(Player p){
        StatsWindow w=pings.get(p.getUniqueId());
        return w==null?0:w.stdDev();
    }

    public double tolerance(Player p){
        return LagMath.tolerance(averagePing(p),jitter(p),Bukkit.getTPS()[0],Bukkit.getAverageTickTime());
    }

    public double reachAllowance(Player attacker,double victimSpeedPerTick){
        return LagMath.reachAllowance(averagePing(attacker),jitter(attacker),victimSpeedPerTick);
    }
}
