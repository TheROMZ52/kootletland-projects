package ir.kootletland.ac.network;

import ir.kootletland.ac.model.Evidence;

/**
 * Seam for multi-server setups. Each backend still runs its own detections; a future Velocity
 * integration can implement this to publish evidence to a central service. No network code lives in the core.
 */
public interface DetectionBridge {
    void publish(Evidence evidence);
    default void close(){}
}
