package ir.kootletland.ac.packet;

public record NormalizedPacket(Type type,long timestampNanos,double x,double y,double z,float yaw,float pitch,int entityId) {
    public enum Type { MOVEMENT, ROTATION, ATTACK, INTERACT, BLOCK, INVENTORY, TRANSACTION, KEEP_ALIVE, TELEPORT_ACK, CORRECTION }
}
