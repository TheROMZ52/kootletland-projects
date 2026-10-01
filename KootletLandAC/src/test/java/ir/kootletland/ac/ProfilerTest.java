package ir.kootletland.ac;

import ir.kootletland.ac.profile.Profiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilerTest {
    @Test void recordsCallsAndSortsByTotalTime()throws Exception{
        Profiler p=new Profiler();
        long a=p.begin();Thread.sleep(8);p.end("slow",a);
        for(int i=0;i<3;i++){long b=p.begin();p.end("fast",b);}
        var report=p.report();
        assertEquals("slow",report.get(0).name());
        assertEquals(3,(int)report.stream().filter(s->s.name().equals("fast")).findFirst().get().calls());
        assertTrue(report.get(0).maxMicros()>=report.get(0).avgMicros());
    }

    @Test void disabledProfilerRecordsNothing(){
        Profiler p=new Profiler();
        p.enabled(false);
        long t=p.begin();
        p.end("x",t);
        assertTrue(p.report().isEmpty());
    }

    @Test void resetClearsSections(){
        Profiler p=new Profiler();
        p.end("x",p.begin());
        p.reset();
        assertTrue(p.report().isEmpty());
    }
}
