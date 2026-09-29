package ir.kootletland.ac.packet;

import org.bukkit.entity.Player;
import java.util.function.Consumer;

public interface PacketAdapter {
    void register(Player player,Consumer<NormalizedPacket> consumer);
    void unregister(Player player);
    boolean isAvailable();
}
