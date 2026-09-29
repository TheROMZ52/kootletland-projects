package ir.kootletland.ac.engine;

import ir.kootletland.ac.model.Evidence;
import java.util.*;

public final class CorrelationEngine {
    private final Map<UUID,Set<String>> recent=new HashMap<>();
    public synchronized boolean add(UUID id,Evidence e){Set<String> set=recent.computeIfAbsent(id,k->new HashSet<>()); set.add(e.check()); if(set.size()>=2)return true; return false;}
    public synchronized void clear(UUID id){recent.remove(id);}
}
