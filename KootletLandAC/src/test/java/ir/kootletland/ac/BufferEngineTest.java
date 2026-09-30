package ir.kootletland.ac;

import ir.kootletland.ac.engine.BufferEngine;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferEngineTest {
    @Test void decayNeverGoesBelowZero(){
        BufferEngine b=new BufferEngine();
        UUID id=UUID.randomUUID();
        b.add(id,1.0);
        assertEquals(0.0,b.decay(id,5.0),0.0);
    }

    @Test void negativeAdditionsAreIgnored(){
        BufferEngine b=new BufferEngine();
        UUID id=UUID.randomUUID();
        b.add(id,2.0);
        b.add(id,-10.0);
        assertEquals(2.0,b.get(id),0.0);
    }

    @Test void clearRemovesPlayer(){
        BufferEngine b=new BufferEngine();
        UUID id=UUID.randomUUID();
        b.add(id,3.0);
        b.clear(id);
        assertEquals(0.0,b.get(id),0.0);
    }
}
