package ir.kootletland.ac;

import ir.kootletland.ac.model.TimerBalance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimerBalanceTest {
    private static final long MS=1_000_000L;

    @Test void steadyTwentyTicksPerSecondStaysNearZero(){
        TimerBalance t=new TimerBalance();
        long now=1_000*MS;
        for(int i=0;i<200;i++){t.tick(now);now+=50*MS;}
        assertEquals(0.0,t.balance(),1.0);
    }

    @Test void clientRunningTwentyFivePercentFasterDriftsPastThreshold(){
        TimerBalance t=new TimerBalance();
        long now=1_000*MS;
        for(int i=0;i<60;i++){t.tick(now);now+=40*MS;}
        assertTrue(t.balance()>200.0,"balance was "+t.balance());
    }

    @Test void lagSpikeFollowedByBurstDoesNotFlag(){
        TimerBalance t=new TimerBalance();
        long now=1_000*MS;
        for(int i=0;i<50;i++){t.tick(now);now+=50*MS;}
        now+=500*MS;                       // 500ms stall
        for(int i=0;i<10;i++){t.tick(now);} // queued packets arrive together
        assertTrue(t.balance()<200.0,"balance was "+t.balance());
    }

    @Test void longIdleIsClampedSoCheatersCannotBankUnlimitedTime(){
        TimerBalance t=new TimerBalance();
        t.tick(1_000*MS);
        t.tick(1_000*MS+60_000*MS);
        assertEquals(-1000.0,t.balance(),0.001);
    }

    @Test void resetClearsEverything(){
        TimerBalance t=new TimerBalance();
        t.tick(1_000*MS);t.tick(1_010*MS);
        t.reset();
        assertEquals(0.0,t.balance(),0.0);
        assertEquals(0,t.samples());
    }
}
