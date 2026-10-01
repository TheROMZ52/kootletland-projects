package ir.kootletland.ac;

import ir.kootletland.ac.model.StatsWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsWindowTest {
    @Test void meanMedianAndPercentile(){
        StatsWindow w=new StatsWindow(10);
        for(int i=1;i<=5;i++)w.add(i*10.0);
        assertEquals(30.0,w.mean(),1e-9);
        assertEquals(30.0,w.median(),1e-9);
        assertEquals(50.0,w.percentile(1.0),1e-9);
        assertEquals(10.0,w.percentile(0.0),1e-9);
    }

    @Test void oldValuesFallOutOfTheWindow(){
        StatsWindow w=new StatsWindow(4);
        for(int i=0;i<100;i++)w.add(1000.0);
        for(int i=0;i<4;i++)w.add(10.0);
        assertEquals(10.0,w.mean(),1e-9);
        assertEquals(4,w.size());
    }

    @Test void nonFiniteValuesAreIgnored(){
        StatsWindow w=new StatsWindow(4);
        w.add(Double.NaN);
        w.add(Double.POSITIVE_INFINITY);
        assertEquals(0,w.size());
    }

    @Test void constantIntervalsHaveZeroEntropyAndVariedOnesDoNot(){
        StatsWindow regular=new StatsWindow(40);
        StatsWindow varied=new StatsWindow(40);
        for(int i=0;i<40;i++){regular.add(66.0);varied.add(50.0+(i*37)%160);}
        assertEquals(0.0,regular.entropy(8.0),1e-9);
        assertTrue(varied.entropy(8.0)>2.5,"entropy was "+varied.entropy(8.0));
        assertEquals(0.0,regular.coefficientOfVariation(),1e-9);
    }
}
