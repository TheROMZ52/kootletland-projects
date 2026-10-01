package ir.kootletland.ac.storage;

import ir.kootletland.ac.model.Evidence;

import java.util.List;
import java.util.UUID;

/** Persistence seam. The core never touches a database; implementations must never block the main thread. */
public interface StorageProvider {
    void save(UUID player,Evidence evidence);
    /** Newest first. May block, so call it off the main thread. */
    default List<StoredEvidence> recent(UUID player,int limit){return List.of();}
    default String name(){return "none";}
    default void close(){}
}
