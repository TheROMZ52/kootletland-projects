package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import ir.kootletland.ac.metrics.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiCheatEngine {
    private final Plugin plugin;
    private final DetectionEngine detection=new DetectionEngine();
    private final CorrelationEngine correlation=new CorrelationEngine();
    private final BufferEngine buffer=new BufferEngine();
    private final Metrics metrics=new Metrics();
    private final java.util.concurrent.CopyOnWriteArrayList<java.util.function.Consumer<Evidence>> listeners=new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<UUID,PlayerData> players=new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastAlerts=new ConcurrentHashMap<>();
    private final java.util.Set<UUID> pendingMoves=ConcurrentHashMap.newKeySet();
    private long checks;
    private long detections;
    private long alerts;
    private long started;
    private BukkitTask movementTask;
    private BukkitTask maintenanceTask;

    public AntiCheatEngine(Plugin plugin){this.plugin=plugin;reloadChecks();}

    public void start(){
        started=System.currentTimeMillis();
        for(Player p:Bukkit.getOnlinePlayers())add(p);
        movementTask=maintenanceTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            for(UUID id:pendingMoves){
                Player p=Bukkit.getPlayer(id);
                if(p!=null) processMove(p);
            }
            pendingMoves.clear();
        },1L,1L);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            for(Player p:Bukkit.getOnlinePlayers()){
                PlayerData d=add(p);
                d.decay(0.035);
                buffer.decay(p.getUniqueId(),0.06);
            }
            lastAlerts.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue()>60_000L);
        },20L,20L);
    }

    public void reloadChecks(){
        String[][] groups={{"speed","Speed"},{"fly","Fly"},{"nofall","NoFall"},{"jesus","Jesus"},{"step","Step"},{"highjump","HighJump"},{"longjump","LongJump"},{"timer","Timer"},{"phase","Phase"},{"noweb","NoWeb"},{"invalidmovement","InvalidMovement"},{"strafe","Strafe"},{"motion","Motion"},{"reach","Reach"},{"killaura","KillAura"},{"aim","Aim"},{"autoclicker","AutoClicker"},{"velocity","Velocity"}};
        for(String[] x:groups){
            boolean movement=plugin.getConfig().getBoolean("checks.movement."+x[0],false);
            boolean combat=plugin.getConfig().getBoolean("checks.combat."+x[0],false);
            boolean enabled=movement||combat||x[1].equals("InvalidMovement");
            detection.setEnabled(x[1],enabled);
        }
    }

    public void shutdown(){
        if(movementTask!=null)movementTask.cancel();
        if(maintenanceTask!=null)maintenanceTask.cancel();
        pendingMoves.clear();
        players.clear();        lastAlerts.clear();
    }

    public PlayerData add(Player p){return players.computeIfAbsent(p.getUniqueId(),id->new PlayerData(p));}

    public void remove(UUID id){
        players.remove(id);
        correlation.clear(id);
        buffer.clear(id);
        lastAlerts.remove(id);
    }

    public PlayerData get(UUID id){return players.get(id);}

    public void move(Player p){
        if(p.hasPermission("kootletlandac.bypass"))return;
        pendingMoves.add(p.getUniqueId());
    }

    private void processMove(Player p){
        if(!p.isOnline()||p.hasPermission("kootletlandac.bypass"))return;
        PlayerData d=add(p);
        long started=System.nanoTime();
        d.update(p.getLocation());
        detection.movement(p,d,e->record(p,e));
        d.touchMovement();
        metrics.check(System.nanoTime()-started);
        checks++;
    }

    public void attack(Player p,org.bukkit.entity.Entity target){
        if(p.hasPermission("kootletlandac.bypass"))return;
        PlayerData d=add(p);
        long started=System.nanoTime();
        detection.combat(p,target,d,e->record(p,e));
        d.touchAttack();
        metrics.check(System.nanoTime()-started);
        checks++;
    }

    public void velocity(Player p){add(p).velocity(p.getVelocity());}
    public void teleport(Player p){add(p).touchTeleport();}
    public void damage(Player p){add(p).touchDamage();}

    private void record(Player p,Evidence e){
        detections++;
        metrics.detection();
        listeners.forEach(listener->{try{listener.accept(e);}catch(Throwable ignored){}});
        PlayerData d=add(p);
        boolean corroborated=correlation.add(p.getUniqueId(),e);
        double confidence=e.confidence();
        if(corroborated)confidence=Math.min(.995,confidence+.08);
        double suspicion=buffer.add(p.getUniqueId(),e.violation()*(corroborated?1.15:1.0));
        d.addViolation(Math.min(1.5,e.violation()*(corroborated?1.15:1.0)));
        d.confidence(confidence);
        d.addEvidence(e,plugin.getConfig().getInt("settings.max-evidence-per-player",80));

        double threshold=plugin.getConfig().getDouble("settings.confidence-warning-threshold",.82);
        double vlThreshold=plugin.getConfig().getDouble("settings.violation-warning-threshold",5.0);
        if(confidence>=threshold&&d.violation()>=vlThreshold&&suspicion>=1.5)alert(p,e,corroborated);
    }

    private void alert(Player p,Evidence e,boolean corroborated){
        if(!plugin.getConfig().getBoolean("settings.alerts",true))return;
        long now=System.currentTimeMillis();
        long cooldown=Math.max(0,plugin.getConfig().getLong("settings.alert-cooldown-ms",3000L));
        Long previous=lastAlerts.get(p.getUniqueId());
        if(previous!=null&&now-previous<cooldown)return;
        lastAlerts.put(p.getUniqueId(),now);
        alerts++;
        metrics.warning();
        String msg="§c[KootletAC] §f"+p.getName()+" §7→ §e"+e.check()+" §7confidence=§f"+String.format("%.1f",e.confidence()*100)+"%% §7VL=§f"+String.format("%.2f",add(p).violation())+(corroborated?" §7correlated":"");
        Bukkit.getOnlinePlayers().stream().filter(x->x.hasPermission("kootletlandac.alerts")).forEach(x->x.sendMessage(msg));
        plugin.getLogger().warning(msg.replaceAll("§[0-9a-fk-or]",""));
    }

    public long checks(){return checks;}
    public long detections(){return detections;}
    public long alerts(){return alerts;}
    public Metrics metrics(){return metrics;}
    public long uptime(){return System.currentTimeMillis()-started;}
    public Map<UUID,PlayerData> players(){return players;}
    public void listen(java.util.function.Consumer<Evidence> listener){if(listener!=null)listeners.add(listener);}
    public void submit(UUID uuid,Evidence evidence){Player p=Bukkit.getPlayer(uuid);if(p!=null&&evidence!=null)record(p,evidence);}

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
                " §7speedStd=§f"+String.format("%.4f",d.speedStdDev())+
                " §7evidence=§f"+d.evidence().size();
    }
}