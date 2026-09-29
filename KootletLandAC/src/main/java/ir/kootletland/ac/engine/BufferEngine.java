package ir.kootletland.ac.engine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BufferEngine {
    private final Map<UUID,Double> values=new ConcurrentHashMap<>();
    public double add(UUID id,double amount){return values.merge(id,Math.max(0,amount),Double::sum);}
    public double decay(UUID id,double amount){return values.compute(id,(k,v)->v==null?0:Math.max(0,v-amount));}
    public double get(UUID id){return values.getOrDefault(id,0d);}
    public void clear(UUID id){values.remove(id);}
    public void clearAll(){values.clear();}
}
