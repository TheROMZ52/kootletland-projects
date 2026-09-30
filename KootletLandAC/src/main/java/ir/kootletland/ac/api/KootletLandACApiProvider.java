package ir.kootletland.ac.api;

import ir.kootletland.ac.engine.AntiCheatEngine;
import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.model.PlayerData;

import java.util.UUID;
import java.util.function.Consumer;

public final class KootletLandACApiProvider implements KootletLandACApi {
    private final AntiCheatEngine engine;

    public KootletLandACApiProvider(AntiCheatEngine engine){this.engine=engine;}

    @Override public PlayerData getPlayerData(UUID uuid){return engine.get(uuid);}
    @Override public double getViolationLevel(UUID uuid){
        PlayerData data=engine.get(uuid);
        return data==null?0:data.violation();
    }
    @Override public double getConfidence(UUID uuid){
        PlayerData data=engine.get(uuid);
        return data==null?0:data.confidence();
    }
    @Override public void submitEvidence(UUID uuid,Evidence evidence){engine.submit(uuid,evidence);}
    @Override public void listenDetection(Consumer<Evidence> listener){engine.listen(listener);}
}
