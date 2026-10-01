package ir.kootletland.ac.engine;

/** Maps accumulated evidence to a response level. Nothing here punishes; callers decide what a level means. */
public final class DecisionEngine {
    public enum Level { LOW, MEDIUM, HIGH, VERY_HIGH }

    public record Decision(Level level,String reason){}

    private DecisionEngine(){}

    public static Decision decide(double confidence,double violation,int independentChecks,double behaviorScore,
                                  double warnConfidence,double warnViolation){
        if(confidence>=.95&&violation>=15.0&&independentChecks>=3&&behaviorScore>=.6){
            return new Decision(Level.VERY_HIGH,"confidence>=95%, VL>=15, 3+ independent checks, sustained behaviour");
        }
        if(confidence>=.90&&violation>=10.0&&independentChecks>=2){
            return new Decision(Level.HIGH,"confidence>=90%, VL>=10, 2+ independent checks");
        }
        if(confidence>=warnConfidence&&violation>=warnViolation){
            return new Decision(Level.MEDIUM,"confidence and VL above alert thresholds");
        }
        return new Decision(Level.LOW,"monitoring");
    }
}
