package ir.kootletland.ac.packet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.function.Consumer;

/** Turns Bukkit events into {@link NormalizedPacket}s. Not raw packets, but the core cannot tell the difference. */
public final class PaperEventPacketAdapter implements PacketAdapter, Listener {
    private final Plugin plugin;
    private volatile Consumer<NormalizedPacket> consumer;

    public PaperEventPacketAdapter(Plugin plugin){this.plugin=plugin;}

    @Override public String name(){return "Paper events";}
    @Override public boolean isAvailable(){return true;}
    @Override public boolean providesRawPackets(){return false;}

    @Override public void start(Consumer<NormalizedPacket> c){
        if(consumer!=null)return; // reload must never register the listener twice
        consumer=c;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    @Override public void stop(){
        consumer=null;
        HandlerList.unregisterAll(this);
    }

    private void emit(NormalizedPacket.Type type,Player p,UUID target,double x,double y,double z,float yaw,float pitch){
        Consumer<NormalizedPacket> c=consumer;
        if(c==null)return;
        try{
            c.accept(new NormalizedPacket(type,p.getUniqueId(),target,System.nanoTime(),x,y,z,yaw,pitch));
        }catch(IllegalArgumentException ignored){
            // malformed values are dropped at the boundary instead of reaching the core
        }
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onMove(PlayerMoveEvent e){
        Location to=e.getTo();
        if(to==null)return;
        Location from=e.getFrom();
        boolean moved=from.getX()!=to.getX()||from.getY()!=to.getY()||from.getZ()!=to.getZ();
        emit(moved?NormalizedPacket.Type.MOVEMENT:NormalizedPacket.Type.ROTATION,e.getPlayer(),null,to.getX(),to.getY(),to.getZ(),to.getYaw(),to.getPitch());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e){
        Location to=e.getTo();
        emit(NormalizedPacket.Type.TELEPORT_ACK,e.getPlayer(),null,to.getX(),to.getY(),to.getZ(),to.getYaw(),to.getPitch());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onVelocity(PlayerVelocityEvent e){
        Vector v=e.getVelocity();
        emit(NormalizedPacket.Type.CORRECTION,e.getPlayer(),null,v.getX(),v.getY(),v.getZ(),0f,0f);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent e){
        if(e.getEntity() instanceof Player p)emit(NormalizedPacket.Type.DAMAGE,p,null,0,0,0,0f,0f);
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onAttack(EntityDamageByEntityEvent e){
        // Only real melee hits. Sweep, thorns and other indirect damage would fake 0ms intervals and long reach.
        if(e.getCause()!=EntityDamageEvent.DamageCause.ENTITY_ATTACK)return;
        if(e.getDamager() instanceof Player p)emit(NormalizedPacket.Type.ATTACK,p,e.getEntity().getUniqueId(),0,0,0,0f,0f);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e){emit(NormalizedPacket.Type.INTERACT,e.getPlayer(),null,0,0,0,0f,0f);}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent e){emit(NormalizedPacket.Type.BLOCK,e.getPlayer(),null,0,0,0,0f,0f);}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent e){emit(NormalizedPacket.Type.BLOCK,e.getPlayer(),null,0,0,0,0f,0f);}

    @EventHandler(priority=EventPriority.MONITOR)
    public void onInventory(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p)emit(NormalizedPacket.Type.INVENTORY,p,null,0,0,0,0f,0f);
    }
}
