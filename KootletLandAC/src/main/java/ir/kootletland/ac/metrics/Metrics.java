package ir.kootletland.ac.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class Metrics {
    private final LongAdder checks=new LongAdder();
    private final LongAdder totalNanos=new LongAdder();
    private final AtomicLong maxNanos=new AtomicLong();
    private final LongAdder detections=new LongAdder();
    private final LongAdder warnings=new LongAdder();
    private final LongAdder events=new LongAdder();
    private final LongAdder eventNanos=new LongAdder();
    private final AtomicLong maxEventNanos=new AtomicLong();
    private final Map<String,LongAdder> perCheck=new ConcurrentHashMap<>();

    public void check(long nanos){
        checks.increment();
        totalNanos.add(Math.max(0,nanos));
        maxNanos.accumulateAndGet(Math.max(0,nanos),Math::max);
    }
    public void event(long nanos){
        events.increment();
        eventNanos.add(Math.max(0,nanos));
        maxEventNanos.accumulateAndGet(Math.max(0,nanos),Math::max);
    }
    public void detection(){detections.increment();}
    public void detection(String check){detections.increment();perCheck.computeIfAbsent(check,k->new LongAdder()).increment();}
    public void warning(){warnings.increment();}

    public long checks(){return checks.sum();}
    public long detections(){return detections.sum();}
    public long warnings(){return warnings.sum();}
    public long events(){return events.sum();}
    public double avgMicros(){long c=checks.sum();return c==0?0:totalNanos.sum()/1000.0/c;}
    public double maxMicros(){return maxNanos.get()/1000.0;}
    public double avgEventMicros(){long c=events.sum();return c==0?0:eventNanos.sum()/1000.0/c;}
    public double maxEventMicros(){return maxEventNanos.get()/1000.0;}
    public Map<String,Long> perCheck(){
        Map<String,Long> out=new java.util.TreeMap<>();
        perCheck.forEach((k,v)->out.put(k,v.sum()));
        return out;
    }
    public static long usedMemoryMb(){
        Runtime r=Runtime.getRuntime();
        return (r.totalMemory()-r.freeMemory())/(1024*1024);
    }
}
