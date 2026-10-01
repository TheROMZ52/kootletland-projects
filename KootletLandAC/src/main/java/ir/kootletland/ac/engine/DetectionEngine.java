package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import ir.kootletland.ac.model.StatsWindow;
import ir.kootletland.ac.model.TimerBalance;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;

public final class DetectionEngine {
    private final LagCompensationEngine lag;
    private final PhysicsEngine physics;
    private final ContextEngine context=new ContextEngine();
    private final PredictionEngine prediction=new PredictionEngine();
    private final Map<String,Boolean> enabled=new ConcurrentHashMap<>();

    public DetectionEngine(LagCompensationEngine lag){
        this.lag=lag;
        this.physics=new PhysicsEngine(lag);
        for(String check:new String[]{"Speed","Fly","NoFall","Jesus","Step","HighJump","LongJump","Timer","Phase","NoWeb","InvalidMovement","Strafe","Motion","Reach","KillAura","Aim","AutoClicker","Velocity"})enabled.put(check,true);
    }

    public void setEnabled(String check,boolean value){enabled.put(check,value);}
    public boolean enabled(String check){return enabled.getOrDefault(check,true);}

    public void movement(Player p,PlayerData d,Consumer<Evidence> sink){
        jesus(p,d,sink);
        double latency=physics.latencyFactor(p);
        boolean grace=physics.contextualGrace(p,d)||context.movementModifier(p,d)>.86;
        boolean modelBroken=grace||prediction.verticalModelBroken(p)||d.recentlyKnockedBack()||d.recentlyTeleported();
        double deviation=prediction.step(p,d,modelBroken,latency);
        if(grace){d.movementAnomaly(false);d.pushSpeedOver(false);return;}
        Location now=d.current();
        if(!finite(now)){d.movementAnomaly(true);emit(p,"InvalidMovement",.995,2.0,sink,Map.of("reason","non_finite_coordinates"));return;}

        double expected=physics.expectedHorizontal(p,d);
        double actual=d.horizontalSpeed();
        d.lastExpectedSpeed(expected);
        double ratio=actual/Math.max(.05,expected);
        double interval=d.movementIntervalMs();
        double tickRatio=interval>0?50.0/interval:1.0;
        double tps=Bukkit.getTPS()[0];
        double intervalExpected=50.0*Math.min(1.35,Math.max(.55,20.0/Math.max(1.0,tps)));
        double intervalDeviation=interval>0?Math.abs(interval-intervalExpected)/intervalExpected:0.0;

        boolean invalid=Math.abs(now.getY())>2.0E7||Math.abs(now.getX())>3.0E7||Math.abs(now.getZ())>3.0E7;
        double speedStd=d.speedStdDev();
        double speedZ=speedStd>.015&&d.speedCount()>18?(actual-d.speedMean())/speedStd:0;
        boolean over=ratio>1.24+latency&&actual-expected>.035;
        d.pushSpeedOver(over);
        // an isolated tick over the limit is noise; speed needs to be sustained
        boolean speed=over&&d.speedOverCount()>=3&&d.movementSamples()>3;
        boolean speedPattern=speedZ>3.25&&actual>expected*.96&&d.speedCount()>24;
        double jump=PhysicsEngine.jumpBonus(p);
        boolean airborne=!d.serverOnGround()&&!d.wasServerOnGround()&&!d.jumping();
        boolean fly=airborne&&!p.isInWater()&&!p.isInLava()&&d.movementSamples()>8
                &&d.verticalDelta()>.36+jump&&d.velocity().getY()<.08&&d.lastExpectedSpeed()>.05;
        boolean flyPrediction=!modelBroken&&!d.serverOnGround()&&d.movementSamples()>10&&prediction.sustainedUpwardDeviation(d);
        boolean groundSpoof=!modelBroken&&d.groundSpoofTicks()>=4;
        boolean deviationKnown=!Double.isNaN(deviation);
        boolean highJump=deviationKnown&&deviation>.35&&d.verticalDelta()>.72+jump&&d.movementSamples()>8;
        boolean step=d.verticalDelta()>.72+jump&&d.verticalDelta()<1.35+jump&&d.horizontalSpeed()>.07&&p.isOnGround();
        boolean noFall=p.getFallDistance()>4.0&&p.isOnGround()&&d.verticalDelta()<=.03&&d.movementSamples()>12;
        Material block=p.getLocation().getBlock().getType();
        boolean noweb=block==Material.COBWEB&&d.horizontalSpeed()>.12&&!p.isSneaking();
        boolean longJump=!p.isOnGround()&&d.verticalDelta()>.08&&d.verticalDelta()<.65+jump&&ratio>1.42;
        boolean phase=insideSolid(p)&&d.horizontalSpeed()>.08&&!p.isSneaking();
        boolean strafe=!p.isOnGround()&&Math.abs(d.yawDelta())>115&&actual>expected*1.15;
        boolean motion=deviationKnown&&Math.abs(deviation)>.42&&d.movementSamples()>8;
        long velocityAge=System.nanoTime()-d.lastVelocityNanos();
        boolean velocity=velocityAge>120_000_000L&&velocityAge<900_000_000L&&d.velocity().lengthSquared()>.09&&actual<Math.max(.035,d.velocity().clone().setY(0).length()*.18);

        int signals=(invalid?1:0)+(speed?1:0)+(speedPattern?1:0)+(fly?1:0)+(flyPrediction?1:0)+(groundSpoof?1:0)+(highJump?1:0)+(step?1:0)+(noFall?1:0)+(noweb?1:0)+(longJump?1:0)+(phase?1:0)+(strafe?1:0)+(motion?1:0)+(velocity?1:0);
        d.movementAnomaly(signals>0);
        if(signals==0)return;

        double confidence=.30;
        confidence+=speed?Math.min(.25,Math.max(0,ratio-1.0)*.28):0;
        confidence+=speedPattern?.08:0;
        confidence+=fly?.16:0;
        confidence+=flyPrediction?.18:0;
        confidence+=groundSpoof?.14:0;
        confidence+=highJump?.12:0;
        confidence+=step?.08:0;
        confidence+=noFall?.10:0;
        confidence+=noweb?.09:0;
        confidence+=longJump?.10:0;
        confidence+=phase?.10:0;
        confidence+=strafe?.06:0;
        confidence+=motion?.07:0;
        confidence+=velocity?.11:0;
        confidence+=Math.min(.12,d.consecutiveMovementAnomalies()*.006);
        confidence-=latency*.25;
        if(invalid)confidence=.995;
        confidence=Math.max(.25,Math.min(.995,confidence));

        String[] names={"InvalidMovement","Speed","Speed","Fly","Fly","HighJump","Step","NoFall","NoFall","NoWeb","LongJump","Phase","Strafe","Velocity","Motion"};
        boolean[] flags={invalid,speed,speedPattern,fly,flyPrediction,highJump,step,noFall,groundSpoof,noweb,longJump,phase,strafe,velocity,motion};
        String check=null;
        for(int i=0;i<names.length;i++){
            if(flags[i]&&enabled(names[i])){check=names[i];break;}
        }
        if(check==null)return;
        Map<String,Object> data=new HashMap<>();
        data.put("expected",expected);
        data.put("actual",actual);
        data.put("ratio",ratio);
        data.put("speedMean",d.speedMean());
        data.put("speedStdDev",speedStd);
        data.put("speedZScore",speedZ);
        data.put("pingMs",p.getPing());
        data.put("latencyFactor",latency);
        data.put("movementIntervalMs",interval);
        data.put("movementStdDev",d.movementStdDev());
        data.put("tickRatio",tickRatio);
        data.put("intervalExpectedMs",intervalExpected);
        data.put("intervalDeviation",intervalDeviation);
        data.put("dx",now.getX()-d.last().getX());
        data.put("dy",d.verticalDelta());
        data.put("dz",now.getZ()-d.last().getZ());
        data.put("velocityY",d.velocity().getY());
        PredictionEngine.Prediction pred=prediction.predict(d,expected);
        data.put("expectedX",pred.expectedX());
        data.put("expectedY",pred.expectedY());
        data.put("expectedZ",pred.expectedZ());
        data.put("actualX",pred.actualX());
        data.put("actualY",pred.actualY());
        data.put("actualZ",pred.actualZ());
        data.put("horizontalDeviation",pred.horizontalDeviation());
        data.put("verticalDeviation",pred.verticalDeviation());
        data.put("sustainedUpward",d.deviationsAbove(PredictionEngine.DEVIATION_THRESHOLD));
        data.put("serverOnGround",d.serverOnGround());
        data.put("clientOnGround",d.clientOnGround());
        data.put("groundSpoofTicks",d.groundSpoofTicks());
        data.put("avgPingMs",lag.averagePing(p));
        data.put("jitterMs",lag.jitter(p));
        data.put("speedOverTicks",d.speedOverCount());
        data.put("tps",Bukkit.getTPS()[0]);
        data.put("onGround",p.isOnGround());
        data.put("signals",signals);
        data.put("velocityAgeMs",velocityAge/1_000_000.0);
        emit(p,check,confidence,Math.max(.06,(confidence-.48)*2.6),sink,data);
    }

    public void combat(Player attacker,Entity target,PlayerData d,PlayerData victimData,Consumer<Evidence> sink){
        if(!(target instanceof Player victim)||attacker.getGameMode().isInvulnerable()||attacker.getWorld()!=victim.getWorld())return;
        if(physics.combatGrace(attacker,d))return;

        long nowNanos=System.nanoTime();
        d.recordTarget(victim.getUniqueId(),nowNanos);
        Vector eye=attacker.getEyeLocation().toVector();
        BoundingBox box=victim.getBoundingBox();
        double distance=ReachGeometry.distanceToBox(eye.getX(),eye.getY(),eye.getZ(),box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX(),box.getMaxY(),box.getMaxZ());
        double ping=Math.max(0,attacker.getPing());
        double victimSpeed=victimData==null?0:victimData.horizontalSpeed();
        double allowance=lag.reachAllowance(attacker,victimSpeed);
        double interval=d.attackIntervalMs();
        double yaw=Math.abs(d.yawDelta());
        double pitch=Math.abs(d.pitchDelta());
        Vector hitPoint=new Vector(clamp(eye.getX(),box.getMinX(),box.getMaxX()),clamp(eye.getY(),box.getMinY(),box.getMaxY()),clamp(eye.getZ(),box.getMinZ(),box.getMaxZ()));
        Vector line=hitPoint.clone().subtract(eye);
        boolean lineOfSight=line.lengthSquared()<=.0001||attacker.getWorld().rayTraceBlocks(attacker.getEyeLocation(),line.clone().normalize(),line.length(),FluidCollisionMode.NEVER,true)==null;
        if(!lineOfSight&&distance>2.9)return;

        Vector look=attacker.getEyeLocation().getDirection();
        Vector center=box.getCenter().subtract(eye);
        double hitAngle=ReachGeometry.angleDegrees(look.getX(),look.getY(),look.getZ(),center.getX(),center.getY(),center.getZ());

        boolean reach=distance>3.15+allowance&&distance<7.5;
        double rotationAcceleration=Math.abs(d.yawAcceleration())+Math.abs(d.pitchAcceleration());
        boolean aim=yaw>95&&yaw<175&&pitch<25&&d.horizontalSpeed()<.08&&rotationAcceleration<18.0;
        boolean brokenGrid=aim&&d.rotation().quantizationBroken();
        boolean click=interval>0&&interval<85&&d.attackSamples()>10;
        StatsWindow attacks=d.attackWindow();
        double entropy=attacks.size()>=24?attacks.entropy(8.0):Double.MAX_VALUE;
        boolean regular=attacks.size()>=18&&attacks.coefficientOfVariation()<.11&&attacks.stdDev()<7.0;
        boolean lowEntropy=attacks.size()>=24&&entropy<1.4&&attacks.mean()<160&&attacks.coefficientOfVariation()<.2;
        boolean periodic=click&&(regular||lowEntropy);
        boolean switching=d.distinctTargets(600_000_000L,nowNanos)>=3;
        boolean wrongAngle=distance>1.6&&hitAngle>70.0;
        boolean killaura=(aim&&periodic&&(distance<4.5+allowance))||(switching&&(aim||wrongAngle))||(wrongAngle&&periodic);

        int signals=(reach?1:0)+(aim?1:0)+(periodic?1:0)+(killaura?1:0)+(switching?1:0)+(wrongAngle?1:0);
        d.combatAnomaly(signals>0);
        if(signals==0)return;

        double confidence=.30;
        confidence+=reach?.24:0;
        confidence+=aim?.12:0;
        confidence+=brokenGrid?.06:0;
        confidence+=periodic?.18:0;
        confidence+=lowEntropy?.05:0;
        confidence+=killaura?.10:0;
        confidence+=switching?.14:0;
        confidence+=wrongAngle?.16:0;
        confidence+=Math.min(.12,d.consecutiveCombatAnomalies()*.007);
        confidence-=Math.min(.14,lag.averagePing(attacker)/1000.0);
        confidence=Math.max(.25,Math.min(.99,confidence));

        String check=killaura?"KillAura":reach?"Reach":periodic?"AutoClicker":aim?"Aim":"KillAura";
        if(!enabled(check))return;
        Map<String,Object> data=new HashMap<>();
        data.put("distanceToHitbox",distance);
        data.put("lineOfSight",lineOfSight);
        data.put("hitAngleDeg",hitAngle);
        data.put("distinctTargets600ms",d.distinctTargets(600_000_000L,nowNanos));
        data.put("victimSpeedPerTick",victimSpeed);
        data.put("pingMs",ping);
        data.put("avgPingMs",lag.averagePing(attacker));
        data.put("jitterMs",lag.jitter(attacker));
        data.put("reachAllowance",allowance);
        data.put("yawDelta",yaw);
        data.put("pitchDelta",pitch);
        data.put("yawAcceleration",d.yawAcceleration());
        data.put("pitchAcceleration",d.pitchAcceleration());
        data.put("rotationAcceleration",rotationAcceleration);
        data.put("rotationGridStep",d.rotation().gridStep());
        data.put("rotationSmoothness",d.rotation().smoothness());
        data.put("attackIntervalMs",interval);
        data.put("attackMedianMs",attacks.median());
        data.put("attackP90Ms",attacks.percentile(.9));
        data.put("attackEntropyBits",entropy==Double.MAX_VALUE?-1.0:entropy);
        data.put("attackStdDev",attacks.stdDev());
        data.put("attackCoefficientOfVariation",attacks.coefficientOfVariation());
        data.put("attackIntervalSamples",attacks.size());
        data.put("target",victim.getName());
        data.put("signals",signals);
        emit(attacker,check,confidence,Math.max(.05,(confidence-.5)*2.5),sink,data);
    }

    private boolean insideSolid(Player p){
        if(p.isInsideVehicle()||p.isGliding()||p.isRiptiding())return false;
        BoundingBox box=p.getBoundingBox().expand(-.12);
        return p.getWorld().hasCollisionsIn(box);
    }

    /** Timer: movement packets arriving faster than 20 per second, measured as accumulated drift. */
    public void timer(Player p,PlayerData d,Consumer<Evidence> sink){
        TimerBalance timer=d.timer();
        double balance=timer.tick(System.nanoTime());
        double tps=Bukkit.getTPS()[0];
        if(tps<19.0||System.nanoTime()-d.lastTeleportNanos()<2_000_000_000L){timer.reset();return;}
        if(timer.samples()<40||balance<=200.0||!enabled("Timer"))return;
        double confidence=Math.max(.3,Math.min(.9,.45+(balance-200.0)/1500.0)-physics.latencyFactor(p)*.15);
        timer.adjust(-150.0);
        Map<String,Object> data=new HashMap<>();
        data.put("timerBalanceMs",balance);
        data.put("tps",tps);
        data.put("pingMs",p.getPing());
        data.put("samples",timer.samples());
        emit(p,"Timer",confidence,Math.max(.06,(confidence-.48)*2.6),sink,data);
    }

    /** Jesus: walking on top of liquid. Runs before the water grace, which used to make it unreachable. */
    private void jesus(Player p,PlayerData d,Consumer<Evidence> sink){
        boolean candidate=!p.isInsideVehicle()&&!p.isFlying()&&!p.isGliding()&&!p.isSwimming()&&!p.isInWater()
                &&!p.getGameMode().isInvulnerable()
                &&p.getLocation().getBlock().getType().isAir()
                &&p.getLocation().clone().subtract(0,1,0).getBlock().isLiquid()
                &&d.horizontalSpeed()>.10&&Math.abs(d.verticalDelta())<.01
                &&System.nanoTime()-d.lastTeleportNanos()>1_500_000_000L;
        d.liquidSurfaceTicks(candidate?d.liquidSurfaceTicks()+1:0);
        if(d.liquidSurfaceTicks()<8||!enabled("Jesus"))return;
        double confidence=Math.min(.88,.55+(d.liquidSurfaceTicks()-8)*.02);
        Map<String,Object> data=new HashMap<>();
        data.put("ticksOnLiquid",d.liquidSurfaceTicks());
        data.put("horizontal",d.horizontalSpeed());
        data.put("pingMs",p.getPing());
        emit(p,"Jesus",confidence,Math.max(.06,(confidence-.48)*2.6),sink,data);
    }

    private double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private boolean finite(Location l){return l!=null&&Double.isFinite(l.getX())&&Double.isFinite(l.getY())&&Double.isFinite(l.getZ());}

    private void emit(Player p,String check,double confidence,double violation,Consumer<Evidence> sink,Map<String,Object> data){
        sink.accept(new Evidence(p.getName(),check,confidence,violation,Instant.now(),Map.copyOf(data)));
    }
}
