package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.PlayerSnapshot;

public final class ConfidenceEngine {
    public double score(double base,double persistence,double environment,double latency,boolean corroborated){
        double value=base+Math.min(.18,persistence*.025)+environment+latency+(corroborated?.12:0);
        return Math.max(0,Math.min(.999,value));
    }
    public double latencyFactor(PlayerSnapshot s){return s.ping()>180?.14:s.ping()>100?.07:0;}
}
