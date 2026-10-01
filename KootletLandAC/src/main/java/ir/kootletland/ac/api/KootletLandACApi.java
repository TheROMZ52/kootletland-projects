package ir.kootletland.ac.api;

import ir.kootletland.ac.integration.Integration;
import ir.kootletland.ac.model.CustomCheck;
import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;

import java.util.UUID;
import java.util.function.Consumer;

/** Public API, also published through Bukkit's ServicesManager. Core never depends on who calls it. */
public interface KootletLandACApi {
    PlayerData getPlayerData(UUID uuid);
    double getViolationLevel(UUID uuid);
    double getConfidence(UUID uuid);
    double getBehaviorScore(UUID uuid);
    void submitEvidence(UUID uuid,Evidence evidence);
    void listenDetection(Consumer<Evidence> listener);
    void registerCheck(CustomCheck check);
    void registerIntegration(Integration integration);
}
