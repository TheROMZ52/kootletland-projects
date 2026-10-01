package ir.kootletland.ac.engine;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Long-horizon pattern analysis. Individual detections say what happened in one tick;
 * this looks at whether suspicious evidence keeps coming back over 5s / 10s / 20s / 30s windows
 * and across more than one domain (movement, combat, external).
 */
public final class BehaviorEngine {
    public record Assessment(double score,int activeWindows,boolean persistent,int domains){
        public static final Assessment NONE=new Assessment(0,0,false,0);
        /** Extra confidence earned by sustained, repeated behaviour (never more than .10). */
        public double confidenceBonus(){return score>=.6?Math.min(.10,(score-.5)*.2):0;}
    }

    private record Sample(long timeMs,String domain,double confidence){}

    private static final long[] WINDOWS_MS={5_000L,10_000L,20_000L,30_000L};
    private static final int[] EXPECTED={3,5,8,10};
    private static final long RETENTION_MS=30_000L;
    private static final int MAX_SAMPLES=200;
    private final Map<UUID,ArrayDeque<Sample>> samples=new HashMap<>();

    public static String domainOf(String check){
        return switch(check){
            case "Speed","Fly","HighJump","Step","Phase","NoFall","Jesus","NoWeb","LongJump","Timer","Strafe","Motion","InvalidMovement" -> "movement";
            case "Reach","KillAura","Aim","AutoClicker","Velocity" -> "combat";
            default -> "external";
        };
    }

    public synchronized Assessment record(UUID id,String check,double confidence,long nowMs){
        ArrayDeque<Sample> q=samples.computeIfAbsent(id,k->new ArrayDeque<>());
        q.addFirst(new Sample(nowMs,domainOf(check),Math.max(0,Math.min(1,confidence))));
        prune(q,nowMs);
        while(q.size()>MAX_SAMPLES)q.removeLast();
        return evaluate(q,nowMs);
    }

    public synchronized Assessment assess(UUID id,long nowMs){
        ArrayDeque<Sample> q=samples.get(id);
        if(q==null)return Assessment.NONE;
        prune(q,nowMs);
        return evaluate(q,nowMs);
    }

    public synchronized void remove(UUID id){samples.remove(id);}

    private void prune(ArrayDeque<Sample> q,long now){
        while(!q.isEmpty()&&now-q.peekLast().timeMs()>RETENTION_MS)q.removeLast();
    }

    private Assessment evaluate(ArrayDeque<Sample> q,long now){
        if(q.isEmpty())return Assessment.NONE;
        double best=0;
        int active=0;
        for(int w=0;w<WINDOWS_MS.length;w++){
            int count=0;
            double sum=0;
            for(Sample s:q){
                if(now-s.timeMs()>WINDOWS_MS[w])break;
                count++;
                sum+=s.confidence();
            }
            if(count==0)continue;
            double windowScore=Math.min(1.0,count/(double)EXPECTED[w])*(sum/count);
            if(windowScore>=.35)active++;
            best=Math.max(best,windowScore);
        }
        Map<String,Integer> counts=new HashMap<>();
        Map<String,Integer> buckets=new HashMap<>();
        for(Sample s:q){
            int bucket=(int)Math.min(5,Math.max(0,(now-s.timeMs())/5_000L));
            counts.merge(s.domain(),1,Integer::sum);
            buckets.merge(s.domain(),1<<bucket,(a,b)->a|b);
        }
        boolean persistent=false;
        Set<String> domains=new HashSet<>(counts.keySet());
        for(Map.Entry<String,Integer> e:counts.entrySet()){
            if(e.getValue()>=6&&Integer.bitCount(buckets.get(e.getKey()))>=3)persistent=true;
        }
        double score=.45*best+(persistent?.35:0)+(domains.size()>=2?.20:0);
        return new Assessment(Math.min(1.0,score),active,persistent,domains.size());
    }
}
