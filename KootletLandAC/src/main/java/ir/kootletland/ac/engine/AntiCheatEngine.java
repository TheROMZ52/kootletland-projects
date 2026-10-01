package ir.kootletland.ac.engine;

import ir.kootletland.ac.integration.Integration;
import ir.kootletland.ac.metrics.Metrics;
import ir.kootletland.ac.model.CustomCheck;
import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import ir.kootletland.ac.network.DetectionBridge;
import ir.kootletland.ac.network.NoopDetectionBridge;
import ir.kootletland.ac.profile.Profiler;
import ir.kootletland.ac.storage.NoopStorageProvider;
import ir.kootletland.ac.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class AntiCheatEngine {
    private static final int MAX_CHECK_ERRORS=25;
    private static final long BYPASS_CACHE_MS=5_000L;

    private record BypassEntry(boolean bypassed,long expiresAt){}

    private final Plugin plugin;
    private final LagCompensationEngine lag=new LagCompensationEngine();
    private final DetectionEngine detection;
    private final CorrelationEngine correlation=new CorrelationEngine();
    private final BehaviorEngine behavior=new BehaviorEngine();
    private final BufferEngine buffer=new BufferEngine();
    private final Metrics metrics=new Metrics();
    private final Profiler profiler=new Profiler();
    private final CopyOnWriteArrayList<Consumer<Evidence>> listeners=new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<CustomCheck> customChecks=new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Integration> integrations=new CopyOnWriteArrayList<>();
    private final Map<UUID,PlayerData> players=new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastAlerts=new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastPunishments=new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastBypassLog=new ConcurrentHashMap<>();
    private final Map<UUID,BypassEntry> bypassCache=new ConcurrentHashMap<>();
    private final Set<UUID> pendingMoves=ConcurrentHashMap.newKeySet();
    private final Map<String,Integer> checkErrors=new ConcurrentHashMap<>();
    private final Set<String> faulted=ConcurrentHashMap.newKeySet();
    private volatile StorageProvider storage=new NoopStorageProvider();
    private volatile DetectionBridge bridge=new NoopDetectionBridge();
    private long checks;
    private long detections;
    private long alerts;
    private long punishments;
    private long started;
    private long serverTick;
    private BukkitTask movementTask;
    private BukkitTask maintenanceTask;

    public AntiCheatEngine(Plugin plugin){
        this.plugin=plugin;
        this.detection=new DetectionEngine(lag);
        reloadChecks();
    }

    // ---------------------------------------------------------------- lifecycle

    public void start(){
        started=System.currentTimeMillis();
        for(Player p:Bukkit.getOnlinePlayers())add(p);
        movementTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            serverTick++;
            // Snapshot first: one failing player must never leave the queue stuck and re-fail every tick.
            List<UUID> ids=new ArrayList<>(pendingMoves);
            pendingMoves.clear();
            for(UUID id:ids){
                Player p=Bukkit.getPlayer(id);
                if(p==null)continue;
                try{processMove(p);}catch(Throwable t){onCheckError("movement",t);}
            }
        },1L,1L);
        maintenanceTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            long now=System.currentTimeMillis();
            for(Player p:Bukkit.getOnlinePlayers()){
                PlayerData d=add(p);
                lag.sample(p);
                d.decay(0.035);
                buffer.decay(p.getUniqueId(),0.06);
                d.behaviorScore(behavior.assess(p.getUniqueId(),now).score());
            }
            lastAlerts.entrySet().removeIf(e->now-e.getValue()>60_000L);
            lastBypassLog.entrySet().removeIf(e->now-e.getValue()>60_000L);
            bypassCache.entrySet().removeIf(e->e.getValue().expiresAt()<now);
        },20L,20L);
    }

    public void shutdown(){
        if(movementTask!=null)movementTask.cancel();
        if(maintenanceTask!=null)maintenanceTask.cancel();
        stopIntegrations();
        try{storage.close();}catch(Throwable ignored){}
        try{bridge.close();}catch(Throwable ignored){}
        pendingMoves.clear();
        players.clear();
        lastAlerts.clear();
        bypassCache.clear();
    }

    /** Safe to call repeatedly: only re-reads settings, never re-registers tasks or listeners. */
    public void reloadChecks(){
        String[][] groups={{"speed","Speed"},{"fly","Fly"},{"nofall","NoFall"},{"jesus","Jesus"},{"step","Step"},{"highjump","HighJump"},{"longjump","LongJump"},{"timer","Timer"},{"phase","Phase"},{"noweb","NoWeb"},{"invalidmovement","InvalidMovement"},{"strafe","Strafe"},{"motion","Motion"},{"reach","Reach"},{"killaura","KillAura"},{"aim","Aim"},{"autoclicker","AutoClicker"},{"velocity","Velocity"}};
        for(String[] x:groups){
            boolean movement=plugin.getConfig().getBoolean("checks.movement."+x[0],false);
            boolean combat=plugin.getConfig().getBoolean("checks.combat."+x[0],false);
            detection.setEnabled(x[1],movement||combat);
        }
        profiler.enabled(plugin.getConfig().getBoolean("settings.profiling",true));
        checkErrors.clear();
        faulted.clear();
    }

    // ---------------------------------------------------------------- extension points

    public void registerCheck(CustomCheck check){if(check!=null)customChecks.add(check);}
    public void registerIntegration(Integration integration){if(integration!=null)integrations.add(integration);}
    public void setStorage(StorageProvider provider){if(provider!=null)storage=provider;}
    public void setBridge(DetectionBridge detectionBridge){if(detectionBridge!=null)bridge=detectionBridge;}
    public List<Integration> integrations(){return List.copyOf(integrations);}
    public List<CustomCheck> customChecks(){return List.copyOf(customChecks);}

    public void startIntegrations(){
        for(Integration i:integrations){
            try{if(i.available())i.start();}catch(Throwable t){plugin.getLogger().log(Level.WARNING,"Integration "+i.name()+" failed to start",t);}
        }
    }

    public void stopIntegrations(){
        for(Integration i:integrations){
            try{i.stop();}catch(Throwable ignored){}
        }
    }

    // ---------------------------------------------------------------- players

    public PlayerData add(Player p){return players.computeIfAbsent(p.getUniqueId(),id->new PlayerData(p));}

    public void remove(UUID id){
        players.remove(id);
        correlation.clear(id);
        behavior.remove(id);
        buffer.clear(id);
        lag.remove(id);
        lastAlerts.remove(id);
        lastBypassLog.remove(id);
        lastPunishments.remove(id);
        bypassCache.remove(id);
        pendingMoves.remove(id);
    }

    public PlayerData get(UUID id){return players.get(id);}

    // ---------------------------------------------------------------- bypass

    private boolean bypassed(Player p){
        long now=System.currentTimeMillis();
        BypassEntry cached=bypassCache.get(p.getUniqueId());
        if(cached!=null&&cached.expiresAt()>now)return cached.bypassed();
        boolean value=p.hasPermission("kootletlandac.bypass");
        bypassCache.put(p.getUniqueId(),new BypassEntry(value,now+BYPASS_CACHE_MS));
        return value;
    }

    /** bypass.mode: "skip" (default) does not run checks; "log-only" runs them but only writes to the console. */
    private boolean skipBypass(Player p){
        return bypassed(p)&&!"log-only".equalsIgnoreCase(plugin.getConfig().getString("bypass.mode","skip"));
    }

    // ---------------------------------------------------------------- inputs (called by the pipeline)

    public void move(Player p){
        if(skipBypass(p))return;
        pendingMoves.add(p.getUniqueId());
        if(faulted.contains("timer"))return;
        long tb=profiler.begin();
        try{detection.timer(p,add(p),e->record(p,e));}catch(Throwable t){onCheckError("timer",t);}
        profiler.end("timer",tb);
    }

    private void processMove(Player p){
        if(faulted.contains("movement"))return;
        if(!p.isOnline()||skipBypass(p))return;
        PlayerData d=add(p);
        long begin=System.nanoTime();
        long sb=profiler.begin();
        d.update(p.getLocation());
        d.movementTick(serverTick);
        d.refreshState(p);
        profiler.end("movement.state",sb);
        long cb=profiler.begin();
        detection.movement(p,d,e->record(p,e));
        profiler.end("movement.checks",cb);
        for(CustomCheck c:customChecks){
            String key="custom:"+c.name();
            if(faulted.contains(key))continue;
            try{c.onMove(p,d,e->record(p,e));}catch(Throwable t){onCheckError(key,t);}
        }
        d.touchMovement();
        metrics.check(System.nanoTime()-begin);
        checks++;
    }

    public void attack(Player p,Entity target){
        if(skipBypass(p))return;
        if(faulted.contains("combat"))return;
        PlayerData d=add(p);
        PlayerData victimData=target instanceof Player v?players.get(v.getUniqueId()):null;
        long begin=System.nanoTime();
        long ab=profiler.begin();
        try{detection.combat(p,target,d,victimData,e->record(p,e));}catch(Throwable t){onCheckError("combat",t);}
        profiler.end("combat",ab);
        for(CustomCheck c:customChecks){
            String key="custom:"+c.name();
            if(faulted.contains(key))continue;
            try{c.onAttack(p,target,d,e->record(p,e));}catch(Throwable t){onCheckError(key,t);}
        }
        d.attackTick(serverTick);
        d.touchAttack();
        metrics.check(System.nanoTime()-begin);
        checks++;
    }

    public void velocity(Player p,Vector applied){add(p).velocity(applied==null?p.getVelocity():applied);}
    public void teleport(Player p){add(p).touchTeleport();}
    public void damage(Player p){add(p).touchDamage();}
    public void action(Player p,String type){add(p).recordAction(type);}

    // ---------------------------------------------------------------- evidence -> decision

    private void onCheckError(String group,Throwable t){
        int n=checkErrors.merge(group,1,Integer::sum);
        if(n==1)plugin.getLogger().log(Level.SEVERE,"KootletLandAC "+group+" check error (isolated, other checks continue)",t);
        if(n>=MAX_CHECK_ERRORS&&faulted.add(group))plugin.getLogger().severe("KootletLandAC disabled the "+group+" checks after "+n+" errors. Fix the cause and run /kac reload.");
    }

    private void record(Player p,Evidence e){
        long rb=profiler.begin();
        try{recordInternal(p,e);}finally{profiler.end("record",rb);}
    }

    private void recordInternal(Player p,Evidence e){
        detections++;
        metrics.detection(e.check());
        listeners.forEach(listener->{try{listener.accept(e);}catch(Throwable ignored){}});
        try{storage.save(p.getUniqueId(),e);}catch(Throwable ignored){}
        try{bridge.publish(e);}catch(Throwable ignored){}
        PlayerData d=add(p);

        if(bypassed(p)){
            // log-only bypass: admins are analysed but never alerted on, never accumulate VL, never punished
            long now=System.currentTimeMillis();
            Long previous=lastBypassLog.get(p.getUniqueId());
            if(previous==null||now-previous>10_000L){
                lastBypassLog.put(p.getUniqueId(),now);
                plugin.getLogger().info("[bypass log-only] "+p.getName()+" "+e.check()+" confidence="+String.format("%.2f",e.confidence()));
            }
            return;
        }

        boolean corroborated=correlation.add(p.getUniqueId(),e);
        BehaviorEngine.Assessment assessment=behavior.record(p.getUniqueId(),e.check(),e.confidence(),System.currentTimeMillis());
        d.behaviorScore(assessment.score());
        double confidence=e.confidence();
        if(corroborated)confidence+=.08;
        confidence+=assessment.confidenceBonus();
        confidence=Math.min(.995,confidence);
        double weight=corroborated?1.15:1.0;
        double suspicion=buffer.add(p.getUniqueId(),e.violation()*weight);
        d.addViolation(Math.min(1.5,e.violation()*weight));
        d.confidence(confidence);
        d.addEvidence(e,plugin.getConfig().getInt("settings.max-evidence-per-player",80));

        double warnConfidence=plugin.getConfig().getDouble("settings.confidence-warning-threshold",.82);
        double warnViolation=plugin.getConfig().getDouble("settings.violation-warning-threshold",5.0);
        DecisionEngine.Decision decision=DecisionEngine.decide(confidence,d.violation(),independentChecks(d),assessment.score(),warnConfidence,warnViolation);
        if(decision.level()!=DecisionEngine.Level.LOW&&suspicion>=1.5)alert(p,e,corroborated,decision);
        if(decision.level()==DecisionEngine.Level.VERY_HIGH)punish(p,e,decision,confidence);
    }

    private int independentChecks(PlayerData d){
        long cutoff=System.currentTimeMillis()-30_000L;
        Set<String> distinct=new HashSet<>();
        int seen=0;
        for(Evidence x:d.evidence()){
            if(seen++>=40||x.timestamp().toEpochMilli()<cutoff)break;
            distinct.add(x.check());
        }
        return distinct.size();
    }

    private void alert(Player p,Evidence e,boolean corroborated,DecisionEngine.Decision decision){
        if(!plugin.getConfig().getBoolean("settings.alerts",true))return;
        long now=System.currentTimeMillis();
        boolean raised=decision.level()==DecisionEngine.Level.HIGH||decision.level()==DecisionEngine.Level.VERY_HIGH;
        long cooldown=raised
                ?Math.max(0,plugin.getConfig().getLong("decision.high-alert-cooldown-ms",1500L))
                :Math.max(0,plugin.getConfig().getLong("settings.alert-cooldown-ms",3000L));
        Long previous=lastAlerts.get(p.getUniqueId());
        if(previous!=null&&now-previous<cooldown)return;
        lastAlerts.put(p.getUniqueId(),now);
        alerts++;
        metrics.warning();
        PlayerData d=add(p);
        String msg="§c[KootletAC] §f"+p.getName()+" §7→ §e"+e.check()+" §7["+decision.level()+"] confidence=§f"+String.format("%.1f",d.confidence()*100)+"% §7VL=§f"+String.format("%.2f",d.violation())+(corroborated?" §7correlated":"")+(d.behaviorScore()>=.6?" §7sustained":"");
        Bukkit.getOnlinePlayers().stream().filter(x->x.hasPermission("kootletlandac.alerts")).forEach(x->x.sendMessage(msg));
        plugin.getLogger().warning(msg.replaceAll("§[0-9a-fk-or]",""));
    }

    /** Runs the configured console commands. Off unless settings.auto-punishment is true and commands are listed. */
    private void punish(Player p,Evidence e,DecisionEngine.Decision decision,double confidence){
        if(!plugin.getConfig().getBoolean("settings.auto-punishment",false))return;
        List<String> commands=plugin.getConfig().getStringList("decision.very-high.commands");
        if(commands.isEmpty())return;
        long now=System.currentTimeMillis();
        long cooldown=Math.max(10L,plugin.getConfig().getLong("decision.very-high.cooldown-seconds",300L))*1000L;
        Long previous=lastPunishments.get(p.getUniqueId());
        if(previous!=null&&now-previous<cooldown)return;
        lastPunishments.put(p.getUniqueId(),now);
        punishments++;
        PlayerData d=add(p);
        for(String raw:commands){
            String command=raw.replace("{player}",p.getName())
                    .replace("{check}",e.check())
                    .replace("{confidence}",String.format("%.0f",confidence*100))
                    .replace("{vl}",String.format("%.1f",d.violation()));
            plugin.getLogger().warning("[punishment] "+p.getName()+" "+e.check()+" ("+decision.reason()+") -> /"+command);
            try{Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);}catch(Throwable t){plugin.getLogger().log(Level.WARNING,"Punishment command failed",t);}
        }
    }

    // ---------------------------------------------------------------- reporting

    public long checks(){return checks;}
    public long detections(){return detections;}
    public long alerts(){return alerts;}
    public long punishments(){return punishments;}
    public Metrics metrics(){return metrics;}
    public Profiler profiler(){return profiler;}
    public StorageProvider storage(){return storage;}
    public LagCompensationEngine lag(){return lag;}
    public BehaviorEngine behavior(){return behavior;}
    public long uptime(){return System.currentTimeMillis()-started;}
    public Map<UUID,PlayerData> players(){return players;}
    public Set<String> faultedChecks(){return Set.copyOf(faulted);}
    public void listen(Consumer<Evidence> listener){if(listener!=null)listeners.add(listener);}

    /** May be called from any thread (Grim fires flags asynchronously); work always runs on the main thread. */
    public void submit(UUID uuid,Evidence evidence){
        if(evidence==null)return;
        Runnable work=()->{Player p=Bukkit.getPlayer(uuid);if(p!=null)record(p,evidence);};
        if(Bukkit.isPrimaryThread())work.run();else Bukkit.getScheduler().runTask(plugin,work);
    }

    public List<String> debug(Player p){
        PlayerData d=get(p.getUniqueId());
        List<String> out=new ArrayList<>();
        if(d==null){out.add("No data");return out;}
        double tps=Bukkit.getTPS()[0];
        out.add("§e--- KootletAC debug: §f"+p.getName()+" §e---");
        out.add("§7Movement: speed=§f"+String.format("%.3f",d.horizontalSpeed())+" §7expected=§f"+String.format("%.3f",d.lastExpectedSpeed())
                +" §7dy=§f"+String.format("%.3f",d.verticalDelta())+" §7predictedDy=§f"+String.format("%.3f",d.predictedDy())
                +" §7speedOver=§f"+d.speedOverCount()+"/6 §7upwardDev=§f"+d.deviationsAbove(PredictionEngine.DEVIATION_THRESHOLD)+"/"+d.deviationSamples());
        out.add("§7State: clientGround=§f"+d.clientOnGround()+" §7serverGround=§f"+d.serverOnGround()+" §7jumping=§f"+d.jumping()
                +" §7water=§f"+d.inWater()+" §7ice=§f"+d.onIce()+" §7slime=§f"+d.onSlime()+" §7climbable=§f"+d.onClimbable()
                +" §7spoofTicks=§f"+d.groundSpoofTicks()+" §7effects=§f"+d.effects());
        out.add("§7Lag: ping=§f"+p.getPing()+" §7avg=§f"+String.format("%.0f",lag.averagePing(p))+" §7jitter=§f"+String.format("%.1f",lag.jitter(p))
                +" §7tolerance=§f"+String.format("%.2f",lag.tolerance(p))+" §7TPS=§f"+String.format("%.2f",tps)+" §7MSPT=§f"+String.format("%.1f",Bukkit.getAverageTickTime()));
        out.add("§7Velocity: §f"+d.velocity()+" §7knockback=§f"+d.recentlyKnockedBack()+" §7timerBalance=§f"+String.format("%.0fms",d.timer().balance()));
        out.add("§7Combat: attackInterval=§f"+String.format("%.0fms",d.attackIntervalMs())+" §7median=§f"+String.format("%.0f",d.attackWindow().median())
                +" §7cv=§f"+String.format("%.3f",d.attackCoefficientOfVariation())+" §7gridStep=§f"+String.format("%.3f",d.rotation().gridStep())
                +" §7actions=§f"+d.actionCounts());
        out.add("§7VL=§f"+String.format("%.2f",d.violation())+" §7confidence=§f"+String.format("%.1f%%",d.confidence()*100)
                +" §7behavior=§f"+String.format("%.2f",d.behaviorScore())+" §7checks=§f"+enabledChecks());
        d.evidence().stream().limit(3).forEach(x->out.add("§8- §e"+x.check()+" §7conf="+String.format("%.1f%%",x.confidence()*100)+" §7vl="+String.format("%.2f",x.violation())));
        return out;
    }

    public String enabledChecks(){
        StringBuilder sb=new StringBuilder();
        for(String name:new String[]{"Speed","Fly","NoFall","Jesus","Step","HighJump","LongJump","Timer","Phase","NoWeb","InvalidMovement","Strafe","Motion","Reach","KillAura","Aim","AutoClicker","Velocity"}){
            if(detection.enabled(name)){if(sb.length()>0)sb.append(',');sb.append(name);}
        }
        return sb.toString();
    }

    public List<String> stats(){
        List<String> out=new ArrayList<>();
        out.add("§eKootletAC §7checks="+checks+" detections="+detections+" alerts="+alerts+" punishments="+punishments+" players="+players.size()+" uptime="+uptime()/1000+"s");
        out.add("§7check avg="+String.format("%.2fµs",metrics.avgMicros())+" max="+String.format("%.0fµs",metrics.maxMicros())
                +" §7| events="+metrics.events()+" avg="+String.format("%.2fµs",metrics.avgEventMicros())+" max="+String.format("%.0fµs",metrics.maxEventMicros())
                +" §7| memory="+Metrics.usedMemoryMb()+"MB");
        if(!metrics.perCheck().isEmpty())out.add("§7per check: §f"+metrics.perCheck());
        if(!faulted.isEmpty())out.add("§cDisabled after errors: "+faulted);
        return out;
    }
}
