package ir.kootletland.ac.network;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.wire.EvidenceMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends high-confidence evidence to the proxy over a plugin message channel. A backend still decides everything by
 * itself; the proxy only aggregates alerts for staff on other servers. Rate limited per player and check.
 */
public final class PluginMessageDetectionBridge implements DetectionBridge {
    private static final long MIN_INTERVAL_MS=3_000L;
    private final Plugin plugin;
    private final String serverName;
    private final double minConfidence;
    private final Map<String,Long> last=new ConcurrentHashMap<>();

    public PluginMessageDetectionBridge(Plugin plugin,String serverName,double minConfidence){
        this.plugin=plugin;
        this.serverName=serverName;
        this.minConfidence=minConfidence;
    }

    @Override public void publish(Evidence evidence){
        if(evidence.confidence()<minConfidence||!Bukkit.isPrimaryThread())return;
        Player subject=Bukkit.getPlayerExact(evidence.player());
        if(subject==null)return;
        String key=subject.getUniqueId()+":"+evidence.check();
        long now=System.currentTimeMillis();
        Long previous=last.get(key);
        if(previous!=null&&now-previous<MIN_INTERVAL_MS)return;
        // plugin messages need a connected player as carrier
        Player carrier=Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if(carrier==null)return;
        last.put(key,now);
        if(last.size()>2000)last.entrySet().removeIf(e->now-e.getValue()>60_000L);
        try{
            byte[] payload=new EvidenceMessage(serverName,evidence.player(),subject.getUniqueId(),evidence.check(),evidence.confidence(),evidence.violation(),now).encode();
            carrier.sendPluginMessage(plugin,EvidenceMessage.CHANNEL,payload);
        }catch(Exception e){
            plugin.getLogger().fine("Network bridge send failed: "+e.getMessage());
        }
    }
}
