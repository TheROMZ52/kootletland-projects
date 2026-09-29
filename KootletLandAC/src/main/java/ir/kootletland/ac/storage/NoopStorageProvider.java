package ir.kootletland.ac.storage;

import ir.kootletland.ac.model.Evidence;
import java.util.UUID;

public final class NoopStorageProvider implements StorageProvider {
    @Override public void save(UUID player,Evidence evidence){}
}
