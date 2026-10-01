package ir.kootletland.ac.engine;

import ir.kootletland.ac.packet.NormalizedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

/** Single entry point from the packet/event layer into the core. Measures its own cost. */
public final class PacketPipeline implements Consumer<NormalizedPacket> {
    private final AntiCheatEngine engine;

    public PacketPipeline(AntiCheatEngine engine){this.engine=engine;}

    @Override public void accept(NormalizedPacket packet){
        long started=System.nanoTime();
        long pb=engine.profiler().begin();
        try{
            Player p=Bukkit.getPlayer(packet.player());
            if(p==null)return;
            switch(packet.type()){
                case MOVEMENT,ROTATION -> engine.move(p);
                case ATTACK -> {
                    Entity target=packet.target()==null?null:Bukkit.getEntity(packet.target());
                    if(target!=null)engine.attack(p,target);
                }
                case TELEPORT_ACK -> engine.teleport(p);
                case CORRECTION -> engine.velocity(p,new Vector(packet.x(),packet.y(),packet.z()));
                case DAMAGE -> engine.damage(p);
                case INTERACT,BLOCK,INVENTORY -> engine.action(p,packet.type().name());
                case TRANSACTION,KEEP_ALIVE -> { /* NOT IMPLEMENTED: needs a raw packet adapter */ }
            }
        }finally{
            engine.metrics().event(System.nanoTime()-started);
            engine.profiler().end("pipeline",pb);
        }
    }
}
