package ir.kootletland.ac;

import ir.kootletland.ac.packet.NormalizedPacket;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NormalizedPacketTest {
    @Test void rejectsNonFiniteValues(){
        UUID id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new NormalizedPacket(NormalizedPacket.Type.MOVEMENT,id,null,0,Double.NaN,0,0,0f,0f));
        assertThrows(IllegalArgumentException.class,()->new NormalizedPacket(NormalizedPacket.Type.MOVEMENT,id,null,0,0,Double.POSITIVE_INFINITY,0,0f,0f));
        assertThrows(IllegalArgumentException.class,()->new NormalizedPacket(NormalizedPacket.Type.ROTATION,id,null,0,0,0,0,Float.NaN,0f));
    }

    @Test void rejectsMissingPlayer(){
        assertThrows(NullPointerException.class,()->new NormalizedPacket(NormalizedPacket.Type.MOVEMENT,null,null,0,0,0,0,0f,0f));
    }
}
