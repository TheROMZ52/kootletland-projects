package ir.kootletland.ac.storage;

import ir.kootletland.ac.model.Evidence;
import java.util.UUID;

public interface StorageProvider {
    void save(UUID player,Evidence evidence);
    default void close(){}
}
