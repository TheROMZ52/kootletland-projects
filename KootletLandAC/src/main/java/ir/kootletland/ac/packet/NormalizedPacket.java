package ir.kootletland.ac.packet;

import java.util.Objects;
import java.util.UUID;

/** Version-independent view of something a player did. The core only ever sees this type. */
public record NormalizedPacket(Type type,UUID player,UUID target,long timestampNanos,
                               double x,double y,double z,float yaw,float pitch) {
    /** TRANSACTION and KEEP_ALIVE exist for a future raw-packet adapter; the Bukkit event adapter never emits them. */
    public enum Type { MOVEMENT, ROTATION, ATTACK, INTERACT, BLOCK, INVENTORY, TELEPORT_ACK, CORRECTION, DAMAGE, TRANSACTION, KEEP_ALIVE }

    public NormalizedPacket {
        Objects.requireNonNull(type,"type");
        Objects.requireNonNull(player,"player");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Float.isFinite(yaw)||!Float.isFinite(pitch)){
            throw new IllegalArgumentException("non-finite packet values");
        }
    }
}
