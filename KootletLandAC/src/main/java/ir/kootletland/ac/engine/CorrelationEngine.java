package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CorrelationEngine {
    private static final long WINDOW_MILLIS=4_000L;
    private final Map<UUID,Deque<Evidence>> recent=new HashMap<>();

    public synchronized boolean add(UUID id,Evidence e){
        long now=System.currentTimeMillis();
        Deque<Evidence> q=recent.computeIfAbsent(id,k->new ArrayDeque<>());
        q.addFirst(e);
        while(!q.isEmpty()&&now-q.peekLast().timestamp().toEpochMilli()>WINDOW_MILLIS)q.removeLast();
        if(q.size()<3)return false;

        Set<String> distinct=new HashSet<>();
        boolean movement=false;
        boolean combat=false;
        boolean velocity=false;
        boolean grim=false;
        int own=0;
        double confidenceSum=0;
        double weightedSum=0;
        int count=0;

        for(Evidence x:q){
            long age=now-x.timestamp().toEpochMilli();
            if(age>WINDOW_MILLIS)break;
            distinct.add(x.check());
            confidenceSum+=x.confidence();
            weightedSum+=x.confidence()*Math.min(1.0,Math.max(.35,1.0-age/(double)WINDOW_MILLIS));
            count++;
            if(isMovement(x.check()))movement=true;
            if(isCombat(x.check()))combat=true;
            if("Velocity".equals(x.check()))velocity=true;
            if("Grim".equals(x.check()))grim=true;else own++;
        }

        if(count<3||distinct.size()<2)return false;
        double average=confidenceSum/count;
        double weighted=weightedSum/count;
        boolean crossDomain=movement&&combat;
        boolean movementVelocity=movement&&velocity;
        boolean strong=(crossDomain||movementVelocity)&&average>=.62&&weighted>=.58;
        // Grim is one signal among others: it only corroborates when our own checks also fired repeatedly.
        boolean withGrim=grim&&own>=2&&average>=.55&&weighted>=.50;
        return strong||withGrim;
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
