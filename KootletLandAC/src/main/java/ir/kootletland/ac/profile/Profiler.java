package ir.kootletland.ac.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Tiny built-in profiler: named sections with call count, average and maximum cost. */
public final class Profiler {
    public record Section(String name,long calls,double avgMicros,double maxMicros,double totalMillis){}

    private static final class Stat {
        final LongAdder calls=new LongAdder();
        final LongAdder nanos=new LongAdder();
        final AtomicLong max=new AtomicLong();
    }

    private final Map<String,Stat> stats=new ConcurrentHashMap<>();
    private volatile boolean enabled=true;

    public void enabled(boolean value){enabled=value;}
    public boolean enabled(){return enabled;}

    /** Returns 0 when profiling is off so {@link #end} costs nothing. */
    public long begin(){return enabled?System.nanoTime():0L;}

    public void end(String section,long begin){
        if(begin==0L||!enabled)return;
        long n=Math.max(0L,System.nanoTime()-begin);
        Stat s=stats.computeIfAbsent(section,k->new Stat());
        s.calls.increment();
        s.nanos.add(n);
        s.max.accumulateAndGet(n,Math::max);
    }

    public void reset(){stats.clear();}

    /** Sections sorted by total time spent, most expensive first. */
    public List<Section> report(){
        List<Section> out=new ArrayList<>();
        stats.forEach((name,s)->{
            long calls=s.calls.sum();
            double total=s.nanos.sum();
            out.add(new Section(name,calls,calls==0?0:total/calls/1000.0,s.max.get()/1000.0,total/1_000_000.0));
        });
        out.sort(Comparator.comparingDouble(Section::totalMillis).reversed());
        return out;
    }
}
