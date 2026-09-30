package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
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
    private final PhysicsEngine physics=new PhysicsEngine();
    private final ContextEngine context=new ContextEngine();
    private final PredictionEngine prediction=new PredictionEngine();
    private final Map<String,Boolean> enabled=new ConcurrentHashMap<>();

    public DetectionEngine(){
        for(String check:new String[]{"Speed","Fly","NoFall","Jesus","Step","HighJump","LongJump","Timer","Phase","NoWeb","InvalidMovement","Strafe","Motion","Reach","KillAura","Aim","AutoClicker","Velocity"})enabled.put(check,true);
    }

    public void setEnabled(String check,boolean value){enabled.put(check,value);}
    public boolean enabled(String check){return enabled.getOrDefault(check,true);}

    public void movement(Player p,PlayerData d,Consumer<Evidence> sink){
        if(physics.contextualGrace(p,d)||context.movementModifier(p,d)>.86){d.movementAnomaly(false);return;}
        Location now=d.current();
        if(!finite(now)){d.movementAnomaly(true);emit(p,"InvalidMovement",.995,2.0,sink,Map.of("reason","non_finite_coordinates"));return;}

        double expected=physics.expectedHorizontal(p,d);
        double actual=d.horizontalSpeed();
        d.lastExpectedSpeed(expected);
        double ratio=actual/Math.max(.05,expected);
        double latency=physics.latencyFactor(p);
        double interval=d.movementIntervalMs();
        double tickRatio=interval>0?50.0/interval:1.0;
        double tps=Bukkit.getTPS()[0];
        double intervalExpected=50.0*Math.min(1.35,Math.max(.55,20.0/Math.max(1.0,tps)));
        double intervalDeviation=interval>0?Math.abs(interval-intervalExpected)/intervalExpected:0.0;

        boolean invalid=Math.abs(now.getY())>2.0E7||Math.abs(now.getX())>3.0E7||Math.abs(now.getZ())>3.0E7;
        double speedStd=d.speedStdDev();
        double speedZ=speedStd>.015&&d.speedCount()>18?(actual-d.speedMean())/speedStd:0;
        boolean speed=ratio>1.24+latency&&actual-expected>.035&&d.movementSamples()>3;
        boolean speedPattern=speedZ>3.25&&actual>expected*.96&&d.speedCount()>24;
        boolean fly=!p.isOnGround()&&!p.isInWater()&&!p.isInLava()&&d.movementSamples()>8
                &&d.verticalDelta()>.30&&d.velocity().getY()<.08&&d.lastExpectedSpeed()>.05;
        boolean highJump=prediction.anomalousVertical(p,d)&&d.verticalDelta()>.72;
        boolean step=d.verticalDelta()>.72&&d.verticalDelta()<1.35&&d.horizontalSpeed()>.07&&p.isOnGround();
        boolean noFall=p.getFallDistance()>4.0&&p.isOnGround()&&d.verticalDelta()<=.03&&d.movementSamples()>12;
        Material block=p.getLocation().getBlock().getType();
        boolean jesus=p.isInWater()&&!p.isSwimming()&&Math.abs(d.verticalDelta())<.01&&d.horizontalSpeed()>.12;
        boolean noweb=block==Material.COBWEB&&d.horizontalSpeed()>.12&&!p.isSneaking();
        boolean longJump=!p.isOnGround()&&d.verticalDelta()>.08&&d.verticalDelta()<.65&&ratio>1.42;
        boolean timer=d.movementSamples()>35&&tickRatio>1.18&&intervalDeviation>.22&&d.movementStdDev()<24.0;
        boolean phase=insideSolid(p)&&d.horizontalSpeed()>.08&&!p.isSneaking();
        boolean strafe=!p.isOnGround()&&Math.abs(d.yawDelta())>115&&actual>expected*1.15;
        boolean motion=!p.isOnGround()&&Math.abs(d.verticalDelta())>.42&&Math.abs(d.velocity().getY())<.04;
        long velocityAge=System.nanoTime()-d.lastVelocityNanos();
        boolean velocity=velocityAge>120_000_000L&&velocityAge<900_000_000L&&d.velocity().lengthSquared()>.09&&actual<Math.max(.035,d.velocity().clone().setY(0).length()*.18);

        int signals=(invalid?1:0)+(speed?1:0)+(speedPattern?1:0)+(fly?1:0)+(highJump?1:0)+(step?1:0)+(noFall?1:0)+(jesus?1:0)+(noweb?1:0)+(longJump?1:0)+(timer?1:0)+(phase?1:0)+(strafe?1:0)+(motion?1:0)+(velocity?1:0);
        d.movementAnomaly(signals>0);
        if(signals==0)return;

        double confidence=.30;
        confidence+=speed?Math.min(.25,Math.max(0,ratio-1.0)*.28):0;
        confidence+=speedPattern?.08:0;
        confidence+=fly?.16:0;
        confidence+=highJump?.12:0;
        confidence+=step?.08:0;
        confidence+=noFall?.10:0;
        confidence+=jesus?.09:0;
        confidence+=noweb?.09:0;
        confidence+=longJump?.10:0;
        confidence+=timer?.08:0;
        confidence+=phase?.10:0;
        confidence+=strafe?.06:0;
        confidence+=motion?.07:0;
        confidence+=velocity?.11:0;
        confidence+=Math.min(.12,d.consecutiveMovementAnomalies()*.006);
        confidence-=latency*.25;
        if(invalid)confidence=.995;
        confidence=Math.max(.25,Math.min(.995,confidence));

        String check=invalid?"InvalidMovement":speed?"Speed":speedPattern?"Speed":fly?"Fly":highJump?"HighJump":step?"Step":noFall?"NoFall":jesus?"Jesus":noweb?"NoWeb":longJump?"LongJump":timer?"Timer":phase?"Phase":strafe?"Strafe":velocity?"Velocity":"Motion";
        if(!enabled(check))return;
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
        data.put("predictedVertical",prediction.expectedVertical(p,d));
        data.put("verticalDeviation",prediction.verticalDeviation(p,d));
        data.put("tps",Bukkit.getTPS()[0]);
        data.put("onGround",p.isOnGround());
        data.put("signals",signals);
        data.put("velocityAgeMs",velocityAge/1_000_000.0);
        emit(p,check,confidence,Math.max(.06,(confidence-.48)*2.6),sink,data);
    }

    public void combat(Player attacker,Entity target,PlayerData d,Consumer<Evidence> sink){
        if(!(target instanceof Player victim)||attacker.getGameMode().isInvulnerable()||attacker.getWorld()!=victim.getWorld())return;
        if(physics.combatGrace(attacker,d))return;

        Vector eye=attacker.getEyeLocation().toVector();
        BoundingBox box=victim.getBoundingBox();
        double distance=distanceToBox(eye,box);
        double ping=Math.max(0,attacker.getPing());
        double allowance=Math.min(.75,ping/450.0);
        double interval=d.attackIntervalMs();
        double yaw=Math.abs(d.yawDelta());
        double pitch=Math.abs(d.pitchDelta());
        Vector hitPoint=new Vector(clamp(eye.getX(),box.getMinX(),box.getMaxX()),clamp(eye.getY(),box.getMinY(),box.getMaxY()),clamp(eye.getZ(),box.getMinZ(),box.getMaxZ()));
        Vector line=hitPoint.clone().subtract(eye);
        boolean lineOfSight=line.lengthSquared()<=.0001||attacker.getWorld().rayTraceBlocks(attacker.getEyeLocation(),line.clone().normalize(),line.length(),FluidCollisionMode.NEVER,true)==null;
        if(!lineOfSight&&distance>2.9)return;

        boolean reach=distance>3.15+allowance&&distance<7.5;
        double rotationAcceleration=Math.abs(d.yawAcceleration())+Math.abs(d.pitchAcceleration());
        boolean aim=yaw>95&&yaw<175&&pitch<25&&d.horizontalSpeed()<.08&&rotationAcceleration<18.0;
        boolean click=interval>0&&interval<85&&d.attackSamples()>10;
        boolean periodic=click&&d.attackCoefficientOfVariation()<.11&&d.attackStdDev()<7.0&&d.attackIntervalSamples()>=18;
        boolean killaura=aim&&periodic&&(distance<4.5+allowance);

        int signals=(reach?1:0)+(aim?1:0)+(periodic?1:0)+(killaura?1:0);
        d.combatAnomaly(signals>0);
        if(signals==0)return;

        double confidence=.30;
        confidence+=reach?.24:0;
        confidence+=aim?.12:0;
        confidence+=periodic?.18:0;
        confidence+=killaura?.10:0;
        confidence+=Math.min(.12,d.consecutiveCombatAnomalies()*.007);
        confidence-=Math.min(.14,ping/1000.0);
        confidence=Math.max(.25,Math.min(.99,confidence));

        String check=killaura?"KillAura":reach?"Reach":periodic?"AutoClicker":"Aim";
        if(!enabled(check))return;
        Map<String,Object> data=new HashMap<>();
        data.put("distanceToHitbox",distance);
        data.put("lineOfSight",lineOfSight);
        data.put("targetVelocity",victim.getVelocity().length());
        data.put("pingMs",ping);
        data.put("reachAllowance",allowance);
        data.put("yawDelta",yaw);
        data.put("pitchDelta",pitch);
        data.put("yawAcceleration",d.yawAcceleration());
        data.put("pitchAcceleration",d.pitchAcceleration());
        data.put("rotationAcceleration",rotationAcceleration);
        data.put("attackIntervalMs",interval);
        data.put("attackStdDev",d.attackStdDev());
        data.put("attackCoefficientOfVariation",d.attackCoefficientOfVariation());
        data.put("attackIntervalSamples",d.attackIntervalSamples());
        data.put("target",victim.getName());
        data.put("signals",signals);
        emit(attacker,check,confidence,Math.max(.05,(confidence-.5)*2.5),sink,data);
    }

    private boolean insideSolid(Player p){
        Location l=p.getLocation();
        Material m=p.getWorld().getBlockAt(l).getType();
        return m.isSolid()&&!m.isAir()&&!p.isInsideVehicle();
    }

    private double distanceToBox(Vector point,BoundingBox box){
        double x=clamp(point.getX(),box.getMinX(),box.getMaxX());
        double y=clamp(point.getY(),box.getMinY(),box.getMaxY());
        double z=clamp(point.getZ(),box.getMinZ(),box.getMaxZ());
        return point.distance(new Vector(x,y,z));
    }

    private double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private boolean finite(Location l){return l!=null&&Double.isFinite(l.getX())&&Double.isFinite(l.getY())&&Double.isFinite(l.getZ());}

    private void emit(Player p,String check,double confidence,double violation,Consumer<Evidence> sink,Map<String,Object> data){
        sink.accept(new Evidence(p.getName(),check,confidence,violation,Instant.now(),Map.copyOf(data)));
    }
}
