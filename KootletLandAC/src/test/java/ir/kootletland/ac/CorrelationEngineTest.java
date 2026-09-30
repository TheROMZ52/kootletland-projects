package ir.kootletland.ac;

import ir.kootletland.ac.engine.CorrelationEngine;
import ir.kootletland.ac.model.Evidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationEngineTest {
    private static Evidence ev(String check,double confidence){
        return new Evidence("Test",check,confidence,1.0,Instant.now(),Map.of());
    }

    @Test void singleCheckRepeatedNeverCorrelates(){
        CorrelationEngine c=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        boolean any=false;
        for(int i=0;i<10;i++)any|=c.add(id,ev("Speed",.9));
        assertFalse(any);
    }

    @Test void movementOnlyEvidenceDoesNotCorrelate(){
        CorrelationEngine c=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        boolean any=false;
        any|=c.add(id,ev("Speed",.9));
        any|=c.add(id,ev("Fly",.9));
        any|=c.add(id,ev("Speed",.9));
        assertFalse(any);
    }

    @Test void movementPlusVelocityCorrelates(){
        CorrelationEngine c=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        c.add(id,ev("Speed",.85));
        c.add(id,ev("Velocity",.85));
        assertTrue(c.add(id,ev("Speed",.85)));
    }

    @Test void lowConfidenceEvidenceDoesNotCorrelate(){
        CorrelationEngine c=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        c.add(id,ev("Speed",.4));
        c.add(id,ev("Velocity",.4));
        assertFalse(c.add(id,ev("Speed",.4)));
    }

    @Test void playersAreIsolatedFromEachOther(){
        CorrelationEngine c=new CorrelationEngine();
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        c.add(a,ev("Speed",.9));
        c.add(b,ev("Velocity",.9));
        assertFalse(c.add(a,ev("Speed",.9)));
    }
}
