package ir.kootletland.ac;

import ir.kootletland.ac.engine.LagMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LagMathTest {
    @Test void healthyConnectionNeedsNoExtraTolerance(){
        assertEquals(0.0,LagMath.tolerance(20,0,20.0,40.0),1e-9);
    }

    @Test void tolerancePerSignalGrowsAndIsCapped(){
        double highPing=LagMath.tolerance(250,0,20,40);
        double jittery=LagMath.tolerance(20,120,20,40);
        double lowTps=LagMath.tolerance(20,0,14,40);
        assertTrue(highPing>.1);
        assertTrue(jittery>.05);
        assertTrue(lowTps>.05);
        assertEquals(.35,LagMath.tolerance(2000,2000,1,500),1e-9);
    }

    @Test void reachAllowanceScalesWithVictimSpeed(){
        double standing=LagMath.reachAllowance(100,10,0.0);
        double sprinting=LagMath.reachAllowance(100,10,.28);
        assertTrue(sprinting>standing);
        assertTrue(LagMath.reachAllowance(5000,5000,1.0)<=1.1);
    }
}
