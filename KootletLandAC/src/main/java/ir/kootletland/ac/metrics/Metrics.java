package ir.kootletland.ac.metrics;

import java.util.concurrent.atomic.AtomicLong;

public final class Metrics {
    private final AtomicLong checks=new AtomicLong();
    private final AtomicLong detections=new AtomicLong();
    private final AtomicLong warnings=new AtomicLong();
    private final AtomicLong nanos=new AtomicLong();
    public void check(long n){checks.incrementAndGet();nanos.addAndGet(Math.max(0,n));}
    public void detection(){detections.incrementAndGet();}
    public void warning(){warnings.incrementAndGet();}
    public long checks(){return checks.get();}
    public long detections(){return detections.get();}
    public long warnings(){return warnings.get();}
    public double avgMicros(){long c=checks.get();return c==0?0:nanos.get()/1000d/c;}
}
