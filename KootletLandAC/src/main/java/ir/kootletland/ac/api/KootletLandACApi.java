package ir.kootletland.ac.api;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;
import java.util.UUID;
import java.util.function.Consumer;

public interface KootletLandACApi {
    PlayerData getPlayerData(UUID uuid);
    double getViolationLevel(UUID uuid);
    double getConfidence(UUID uuid);
    void submitEvidence(UUID uuid,Evidence evidence);
    void listenDetection(Consumer<Evidence> listener);
}
