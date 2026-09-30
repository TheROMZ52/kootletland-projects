package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CorrelationEngine {
    private static final long WINDOW_MILLIS=4_000L;
    private final Map<UUID,Deque<Evidence>> recent=new HashMap<>();

    public synchronized boolean add(UUID id,Evidence e){
        long now=System.currentTimeMillis();
        Deque<Evidence> q=recent.computeIfAbsent(id,k->new ArrayDeque<>());
        q.addFirst(e);
        while(!q.isEmpty() && now-q.peekLast().timestamp().toEpochMilli()>WINDOW_MILLIS) q.removeLast();
        if(q.size()<3)return false;
        boolean movement=false;
        boolean combat=false;
        boolean velocity=false;
        double confidenceSum=0;
        int count=0;
        for(Evidence x:q){
            if(now-x.timestamp().toEpochMilli()>WINDOW_MILLIS)break;
            confidenceSum+=x.confidence();
            count++;
            if(isMovement(x.check()))movement=true;
            if(isCombat(x.check()))combat=true;
            if("Velocity".equals(x.check()))velocity=true;
        }
        return (movement&&combat&&count>=3||velocity&&movement&&count>=3)&&confidenceSum/count>=0.62;
    }

    private boolean isMovement(String check){
        return switch(check){
            case "Speed","Fly","HighJump","Step","Phase","NoFall","Jesus","NoWeb","LongJump","Timer","Strafe","Motion"->true;
            default->false;
        };
    }

    private boolean isCombat(String check){
        return switch(check){
            case "Reach","KillAura","Aim","AutoClicker"->true;
            default->false;
        };
    }

    public synchronized void clear(UUID id){recent.remove(id);}
}