package ir.kootletland.ac.engine;

/** Pure latency math so it can be unit-tested without a server. */
public final class LagMath {
    private LagMath(){}

    /** Movement tolerance in [0,.35] from average ping, jitter, TPS and milliseconds-per-tick. */
    public static double tolerance(double avgPingMs,double jitterMs,double tps,double msptMs){
        double ping=clamp((avgPingMs-30.0)/300.0,0,.22);
        double jitter=clamp(jitterMs/150.0,0,.12);
        double tick=clamp((20.0-tps)/10.0,0,.10);
        double mspt=clamp((msptMs-45.0)/100.0,0,.05);
        return Math.min(.35,ping+jitter+tick+mspt);
    }

    /** Extra reach allowance (blocks) for interpolation: the victim keeps moving while packets travel. */
    public static double reachAllowance(double avgPingMs,double jitterMs,double victimSpeedPerTick){
        double ping=Math.max(0,avgPingMs);
        double motion=victimSpeedPerTick*(ping/50.0)*.75+victimSpeedPerTick*Math.max(0,jitterMs)/50.0*.5;
        double base=Math.min(.35,ping/1500.0);
        return Math.min(1.1,base+Math.min(.8,motion));
    }

    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
}
