package ir.kootletland.ac.engine;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferEngineTest {
    @Test void accumulatesAndDecays(){
        BufferEngine buffer=new BufferEngine();
        UUID id=UUID.randomUUID();
        assertEquals(2.5,buffer.add(id,2.5),1e-9);
        assertEquals(1.5,buffer.decay(id,1.0),1e-9);
        buffer.clear(id);
        assertEquals(0,buffer.get(id),1e-9);
    }
}
