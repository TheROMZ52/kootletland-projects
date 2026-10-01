package ir.kootletland.ac;

import ir.kootletland.ac.engine.BehaviorEngine;
import ir.kootletland.ac.engine.CorrelationEngine;
import ir.kootletland.ac.engine.DecisionEngine;
import ir.kootletland.ac.engine.DecisionEngine.Level;
import ir.kootletland.ac.model.Evidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end behaviour of correlation + behavior + decision with synthetic evidence streams (no server needed). */
class PvpScenarioTest {
    private static Evidence ev(String check,double confidence){
        return new Evidence("P",check,confidence,1.0,Instant.now(),Map.of());
    }

    @Test void legitPlayerWithOccasionalLowConfidenceNoiseStaysLow(){
        BehaviorEngine behavior=new BehaviorEngine();
        CorrelationEngine correlation=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        double vl=0;
        BehaviorEngine.Assessment last=BehaviorEngine.Assessment.NONE;
        long t=0;
        for(int i=0;i<4;i++){
            t+=25_000L;
            assertFalse(correlation.add(id,ev("Reach",.45)));
            last=behavior.record(id,"Reach",.45,t);
            vl=Math.max(0,vl+.06-25*.035);
        }
        Level level=DecisionEngine.decide(.45,vl,1,last.score(),.82,5.0).level();
        assertEquals(Level.LOW,level);
        assertFalse(last.persistent());
    }

    @Test void cheaterWithSustainedMultiCheckEvidenceEscalates(){
        BehaviorEngine behavior=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        String[] checks={"Speed","Reach","Velocity"};
        BehaviorEngine.Assessment last=BehaviorEngine.Assessment.NONE;
        long t=100_000L;
        for(int i=0;i<30;i++){
            last=behavior.record(id,checks[i%3],.9,t);
            t+=1_000L;
        }
        assertTrue(last.persistent());
        assertTrue(last.score()>=.6);
        double confidence=Math.min(.995,.9+.08+last.confidenceBonus());
        assertEquals(Level.VERY_HIGH,DecisionEngine.decide(confidence,18.0,3,last.score(),.82,5.0).level());
    }

    @Test void oneStrongCheckAloneNeverPunishes(){
        BehaviorEngine behavior=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        BehaviorEngine.Assessment last=BehaviorEngine.Assessment.NONE;
        long t=0;
        for(int i=0;i<40;i++){last=behavior.record(id,"Speed",.97,t);t+=1_000L;}
        Level level=DecisionEngine.decide(.99,60.0,1,last.score(),.82,5.0).level();
        assertTrue(level==Level.MEDIUM||level==Level.LOW);
    }

    @Test void grimAloneDoesNotCorrelateButGrimWithOurChecksDoes(){
        CorrelationEngine c=new CorrelationEngine();
        UUID alone=UUID.randomUUID();
        boolean any=false;
        for(int i=0;i<5;i++)any|=c.add(alone,ev("Grim",.6));
        assertFalse(any);

        UUID both=UUID.randomUUID();
        c.add(both,ev("Grim",.6));
        c.add(both,ev("Speed",.7));
        assertTrue(c.add(both,ev("Speed",.7)));
    }
}
