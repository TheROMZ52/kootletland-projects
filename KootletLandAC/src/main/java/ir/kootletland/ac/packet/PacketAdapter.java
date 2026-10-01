package ir.kootletland.ac.packet;

import java.util.function.Consumer;

/** Source of normalized packets. Version- or library-specific code lives behind this interface only. */
public interface PacketAdapter {
    String name();
    boolean isAvailable();
    /** True when real network packets are read (transactions, keep-alives). False for the Bukkit event layer. */
    boolean providesRawPackets();
    void start(Consumer<NormalizedPacket> consumer);
    void stop();
}
