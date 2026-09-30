package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationEngineTest {
    @Test void correlatesMovementAndCombatEvidence(){
        CorrelationEngine engine=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        Instant now=Instant.now();
        assertFalse(engine.add(id,new Evidence("p","Speed",.8,.4,now,Map.of())));
        assertFalse(engine.add(id,new Evidence("p","Fly",.8,.4,now,Map.of())));
        assertTrue(engine.add(id,new Evidence("p","Reach",.8,.4,now,Map.of())));
    }

    @Test void doesNotCorrelateMovementOnly(){
        CorrelationEngine engine=new CorrelationEngine();
        UUID id=UUID.randomUUID();
        Instant now=Instant.now();
        engine.add(id,new Evidence("p","Speed",.9,.4,now,Map.of()));
        engine.add(id,new Evidence("p","Fly",.9,.4,now,Map.of()));
        assertFalse(engine.add(id,new Evidence("p","Timer",.9,.4,now,Map.of())));
    }
}
