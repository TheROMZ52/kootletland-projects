package ir.kootletland.ac;

import ir.kootletland.ac.engine.ReachGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReachGeometryTest {
    @Test void pointInsideTheBoxIsZeroDistance(){
        assertEquals(0.0,ReachGeometry.distanceToBox(.5,1,.5,0,0,0,1,2,1),1e-9);
    }

    @Test void distanceIsMeasuredToTheNearestFace(){
        assertEquals(3.0,ReachGeometry.distanceToBox(4,1,.5,0,0,0,1,2,1),1e-9);
        // corner: 3 along x and 4 along y gives 5
        assertEquals(5.0,ReachGeometry.distanceToBox(4,6,.5,0,0,0,1,2,1),1e-9);
    }

    @Test void anglesBetweenVectors(){
        assertEquals(0.0,ReachGeometry.angleDegrees(1,0,0,2,0,0),1e-6);
        assertEquals(90.0,ReachGeometry.angleDegrees(1,0,0,0,1,0),1e-6);
        assertEquals(180.0,ReachGeometry.angleDegrees(1,0,0,-1,0,0),1e-6);
        assertEquals(0.0,ReachGeometry.angleDegrees(0,0,0,1,0,0),1e-9);
    }
}
