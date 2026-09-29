package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiCheatEngine {
    private final Plugin plugin;
    private final DetectionEngine detection=new DetectionEngine();
    private final CorrelationEngine correlation=new CorrelationEngine();
    private final Map<UUID,PlayerData> players=new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastAlerts=new ConcurrentHashMap<>();
    private long checks;
    private long detections;
    private long alerts;
    private long started;

    public AntiCheatEngine(Plugin plugin){this.plugin=plugin;}

    public void start(){
        started=System.currentTimeMillis();
        for(Player p:Bukkit.getOnlinePlayers())add(p);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            for(Player p:Bukkit.getOnlinePlayers()){
                PlayerData d=add(p);
                d.decay(0.035);
            }
            lastAlerts.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue()>60_000L);
        },20L,20L);
    }

    public void shutdown(){
        players.clear();
        lastAlerts.clear();
    }

    public PlayerData add(Player p){return players.computeIfAbsent(p.getUniqueId(),id->new PlayerData(p));}

    public void remove(UUID id){
        players.remove(id);
        correlation.clear(id);
        lastAlerts.remove(id);
    }

    public PlayerData get(UUID id){return players.get(id);}

    public void move(Player p){
        if(p.hasPermission("kootletlandac.bypass"))return;
        PlayerData d=add(p);
        detection.movement(p,d,e->record(p,e));
        d.update(p.getLocation());
        d.touchMovement();
        checks++;
    }

    public void attack(Player p,org.bukkit.entity.Entity target){
        if(p.hasPermission("kootletlandac.bypass"))return;
        PlayerData d=add(p);
        detection.combat(p,target,d,e->record(p,e));
        d.touchAttack();
        checks++;
    }

    public void velocity(Player p){add(p).velocity(p.getVelocity());}
    public void teleport(Player p){add(p).touchTeleport();}
    public void damage(Player p){add(p).touchDamage();}

    private void record(Player p,Evidence e){
        detections++;
        PlayerData d=add(p);
        boolean corroborated=correlation.add(p.getUniqueId(),e);
        double confidence=e.confidence();
        if(corroborated)confidence=Math.min(.995,confidence+.08);
        d.addViolation(e.violation()*(corroborated?1.15:1.0));
        d.confidence(confidence);
        d.addEvidence(e,plugin.getConfig().getInt("settings.max-evidence-per-player",80));

        double threshold=plugin.getConfig().getDouble("settings.confidence-warning-threshold",.82);
        double vlThreshold=plugin.getConfig().getDouble("settings.violation-warning-threshold",5.0);
        if(confidence>=threshold&&d.violation()>=vlThreshold)alert(p,e,corroborated);
    }

    private void alert(Player p,Evidence e,boolean corroborated){
        if(!plugin.getConfig().getBoolean("settings.alerts",true))return;
        long now=System.currentTimeMillis();
        long cooldown=Math.max(0,plugin.getConfig().getLong("settings.alert-cooldown-ms",3000L));
        Long previous=lastAlerts.get(p.getUniqueId());
        if(previous!=null&&now-previous<cooldown)return;
        lastAlerts.put(p.getUniqueId(),now);
        alerts++;
        String msg="§c[KootletAC] §f"+p.getName()+" §7→ §e"+e.check()+" §7confidence=§f"+String.format("%.1f",e.confidence()*100)+"%% §7VL=§f"+String.format("%.2f",add(p).violation())+(corroborated?" §7correlated":"");
        Bukkit.getOnlinePlayers().stream().filter(x->x.hasPermission("kootletlandac.alerts")).forEach(x->x.sendMessage(msg));
        plugin.getLogger().warning(msg.replaceAll("§[0-9a-fk-or]",""));
    }

    public long checks(){return checks;}
    public long detections(){return detections;}
    public long alerts(){return alerts;}
    public long uptime(){return System.currentTimeMillis()-started;}
    public Map<UUID,PlayerData> players(){return players;}

    public String debug(Player p){
        PlayerData d=get(p.getUniqueId());
        if(d==null)return "No data";
        return "§7Player §f"+p.getName()+" §7ping=§f"+p.getPing()+" §7TPS=§f"+String.format("%.2f",Bukkit.getTPS()[0])+
                " §7VL=§f"+String.format("%.2f",d.violation())+
                " §7confidence=§f"+String.format("%.1f%%",d.confidence()*100)+
                " §7speed=§f"+String.format("%.3f",d.horizontalSpeed())+
                " §7expected=§f"+String.format("%.3f",d.lastExpectedSpeed())+
                " §7moveInterval=§f"+String.format("%.1fms",d.movementIntervalMs())+
                " §7moveStd=§f"+String.format("%.2f",d.movementStdDev())+
                " §7attackInterval=§f"+String.format("%.1fms",d.attackIntervalMs())+
                " §7attackStd=§f"+String.format("%.2f",d.attackStdDev())+
                " §7evidence=§f"+d.evidence().size();
    }
}