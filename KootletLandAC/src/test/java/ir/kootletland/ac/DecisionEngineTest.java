package ir.kootletland.ac;

import ir.kootletland.ac.engine.DecisionEngine;
import ir.kootletland.ac.engine.DecisionEngine.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionEngineTest {
    private static Level level(double confidence,double vl,int checks,double behavior){
        return DecisionEngine.decide(confidence,vl,checks,behavior,.82,5.0).level();
    }

    @Test void belowThresholdsOnlyMonitors(){
        assertEquals(Level.LOW,level(.5,1,1,0));
        assertEquals(Level.LOW,level(.9,2,1,0));
    }

    @Test void alertThresholdsGiveMedium(){
        assertEquals(Level.MEDIUM,level(.85,6,1,0));
    }

    @Test void highNeedsIndependentChecks(){
        assertEquals(Level.MEDIUM,level(.92,11,1,0));
        assertEquals(Level.HIGH,level(.92,11,2,0));
    }

    @Test void veryHighNeedsEverythingIncludingSustainedBehaviour(){
        assertEquals(Level.HIGH,level(.97,16,3,.3));
        assertEquals(Level.VERY_HIGH,level(.97,16,3,.7));
    }

    @Test void oneStrongCheckNeverReachesVeryHigh(){
        assertEquals(Level.MEDIUM,level(.99,50,1,1.0));
    }
}
