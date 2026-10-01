package ir.kootletland.ac.network;

import ir.kootletland.ac.model.Evidence;

public final class NoopDetectionBridge implements DetectionBridge {
    @Override public void publish(Evidence evidence){}
}
