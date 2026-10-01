package ir.kootletland.ac;

import ir.kootletland.ac.model.RotationAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotationAnalyzerTest {
    @Test void mouseLikeDeltasFitAGrid(){
        RotationAnalyzer r=new RotationAnalyzer(40);
        double step=.15;
        int[] multiples={1,3,2,5,4,1,7,2,6,3,2,8,1,4,5,2,9,3,1,6,2,4};
        for(int m:multiples)r.add(2.0,m*step);
        assertTrue(r.gridStep()>0);
        assertFalse(r.quantizationBroken());
    }

    @Test void softwareRotationDoesNotFitAnyGrid(){
        RotationAnalyzer r=new RotationAnalyzer(40);
        java.util.Random random=new java.util.Random(7);
        for(int i=0;i<30;i++)r.add(2.0,.2+random.nextDouble()*3.0);
        assertEquals(0.0,r.gridStep(),0.0);
        assertTrue(r.quantizationBroken());
    }

    @Test void notEnoughDataNeverJudges(){
        RotationAnalyzer r=new RotationAnalyzer(40);
        for(int i=0;i<10;i++)r.add(1.0,.31+i*.0137);
        assertFalse(r.quantizationBroken());
    }

    @Test void tinyDeltasCannotBeJudged(){
        RotationAnalyzer r=new RotationAnalyzer(40);
        java.util.Random random=new java.util.Random(3);
        for(int i=0;i<40;i++)r.add(1.0,.001+random.nextDouble()*.02);
        assertFalse(r.quantizationBroken());
    }

    @Test void smoothnessIsLowForConstantRotation(){
        RotationAnalyzer r=new RotationAnalyzer(40);
        for(int i=0;i<30;i++)r.add(3.0,.5);
        assertTrue(r.smoothness()<.2);
    }
}
