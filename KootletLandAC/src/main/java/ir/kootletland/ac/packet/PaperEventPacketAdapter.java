package ir.kootletland.ac.packet;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PaperEventPacketAdapter implements PacketAdapter {
    private final Map<UUID,Consumer<NormalizedPacket>> listeners=new ConcurrentHashMap<>();

    @Override public void register(Player p,Consumer<NormalizedPacket> c){listeners.put(p.getUniqueId(),c);}
    @Override public void unregister(Player p){listeners.remove(p.getUniqueId());}
    @Override public boolean isAvailable(){return false;}
    public void publish(Player p,NormalizedPacket packet){
        Consumer<NormalizedPacket> listener=listeners.get(p.getUniqueId());
        if(listener!=null)listener.accept(packet);
    }
}
