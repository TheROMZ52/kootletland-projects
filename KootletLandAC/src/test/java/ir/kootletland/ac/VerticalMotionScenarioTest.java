package ir.kootletland.ac;

import ir.kootletland.ac.model.VerticalMotionTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scenario tests: feed per-tick vertical movement as a player would produce it and check what the model concludes. */
class VerticalMotionScenarioTest {
    private static final double JUMP=VerticalMotionTracker.JUMP_VELOCITY;

    /** Runs a legitimate vanilla jump (up and back down) and returns whether the model ever flagged it. */
    private static boolean flaggedDuringJump(double jumpBonus,double initial){
        VerticalMotionTracker t=new VerticalMotionTracker();
        boolean flagged=false;
        double dy=initial;
        t.step(0,true,true,false,jumpBonus); // standing on ground
        t.step(dy,false,true,false,jumpBonus); // first airborne tick
        for(int tick=0;tick<40;tick++){
            dy=VerticalMotionTracker.nextVelocity(dy);
            boolean landed=dy<-.9&&tick>8; // stop before the model would see ground
            if(landed)break;
            t.step(dy,false,false,false,jumpBonus);
            flagged|=t.sustainedUpward();
        }
        return flagged;
    }

    @Test void normalJumpIsNeverFlagged(){
        assertFalse(flaggedDuringJump(0,JUMP));
    }

    @Test void jumpBoostJumpIsNeverFlagged(){
        assertFalse(flaggedDuringJump(.3,JUMP+.3));
    }

    @Test void walkingOffALedgeIsNeverFlagged(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        double dy=0;
        t.step(dy,false,true,false,0);
        boolean flagged=false;
        for(int i=0;i<30;i++){
            dy=VerticalMotionTracker.nextVelocity(dy);
            t.step(dy,false,false,false,0);
            flagged|=t.sustainedUpward();
        }
        assertFalse(flagged);
    }

    @Test void hoveringInTheAirIsFlagged(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        t.step(0,false,true,false,0);
        boolean flagged=false;
        for(int i=0;i<12;i++){
            t.step(0.0,false,false,false,0);
            flagged|=t.sustainedUpward();
        }
        assertTrue(flagged);
    }

    @Test void steadyAscentIsFlagged(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        t.step(.42,false,true,false,0);
        boolean flagged=false;
        for(int i=0;i<12;i++){
            t.step(.42,false,false,false,0);
            flagged|=t.sustainedUpward();
        }
        assertTrue(flagged);
    }

    @Test void gracedTicksNeverFlagAndResetTheWindow(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        for(int i=0;i<20;i++){
            t.step(0.0,false,false,true,0); // e.g. in a cobweb or lagging
            assertFalse(t.sustainedUpward());
        }
        assertEquals(0,t.samples());
    }

    @Test void knockbackRestartsTheModelWithoutAFalseFlag(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        t.step(0,false,true,false,0);
        for(int i=0;i<5;i++)t.step(VerticalMotionTracker.nextVelocity(-.1*i),false,false,false,0);
        t.applyVelocity(.5); // server applied knockback
        double dy=.5;
        boolean flagged=false;
        for(int i=0;i<10;i++){
            t.step(dy,false,false,false,0);
            dy=VerticalMotionTracker.nextVelocity(dy);
            flagged|=t.sustainedUpward();
        }
        assertFalse(flagged);
    }

    @Test void landingClearsEvidence(){
        VerticalMotionTracker t=new VerticalMotionTracker();
        t.step(0,true,true,false,0);
        t.step(0,false,true,false,0);
        for(int i=0;i<8;i++)t.step(0.0,false,false,false,0);
        assertTrue(t.sustainedUpward());
        t.step(0,true,false,false,0);
        assertFalse(t.sustainedUpward());
    }
}
