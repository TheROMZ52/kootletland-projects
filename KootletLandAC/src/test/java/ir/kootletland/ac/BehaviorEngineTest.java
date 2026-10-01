package ir.kootletland.ac;

import ir.kootletland.ac.engine.BehaviorEngine;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorEngineTest {
    @Test void oneBurstIsNotSustainedBehaviour(){
        BehaviorEngine b=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        BehaviorEngine.Assessment last=BehaviorEngine.Assessment.NONE;
        for(int i=0;i<8;i++)last=b.record(id,"Speed",.9,1_000L+i*100L);
        assertFalse(last.persistent());
        assertEquals(0.0,last.confidenceBonus(),1e-9);
    }

    @Test void repeatedEvidenceSpreadOverTimeAcrossDomainsIsSustained(){
        BehaviorEngine b=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        BehaviorEngine.Assessment last=BehaviorEngine.Assessment.NONE;
        long t=100_000L;
        for(int i=0;i<12;i++){
            last=b.record(id,i%3==0?"Reach":"Speed",.9,t);
            t+=2_000L;
        }
        assertTrue(last.persistent());
        assertTrue(last.domains()>=2);
        assertTrue(last.score()>=.6,"score was "+last.score());
        assertTrue(last.confidenceBonus()>0&&last.confidenceBonus()<=.10);
    }

    @Test void oldEvidenceExpires(){
        BehaviorEngine b=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        b.record(id,"Speed",.9,1_000L);
        assertEquals(0.0,b.assess(id,1_000L+60_000L).score(),1e-9);
    }

    @Test void removingAPlayerForgetsThem(){
        BehaviorEngine b=new BehaviorEngine();
        UUID id=UUID.randomUUID();
        b.record(id,"Speed",.9,1_000L);
        b.remove(id);
        assertEquals(0.0,b.assess(id,1_100L).score(),1e-9);
    }

    @Test void checksMapToDomains(){
        assertEquals("movement",BehaviorEngine.domainOf("Fly"));
        assertEquals("combat",BehaviorEngine.domainOf("KillAura"));
        assertEquals("external",BehaviorEngine.domainOf("Grim"));
    }
}
