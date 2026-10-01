package ir.kootletland.ac;

import ir.kootletland.ac.model.Evidence;
import ir.kootletland.ac.storage.SqlStorageProvider;
import ir.kootletland.ac.storage.StoredEvidence;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs the sqlite JDBC driver (testRuntimeOnly), so it runs in Gradle/CI. */
class SqlStorageProviderTest {
    private static final Logger LOG=Logger.getLogger("test");

    @Test void savedEvidenceIsReadBackNewestFirstAndSurvivesReopen()throws Exception{
        File file=Files.createTempFile("kac",".db").toFile();
        file.deleteOnExit();
        UUID id=UUID.randomUUID();
        SqlStorageProvider s=SqlStorageProvider.sqlite(file,500,30,LOG);
        long now=System.currentTimeMillis();
        s.save(id,new Evidence("Steve","Speed",.9,1.5,Instant.ofEpochMilli(now-2000),Map.of("expected",.28)));
        s.save(id,new Evidence("Steve","Reach",.8,1.0,Instant.ofEpochMilli(now-1000),Map.of("note","a \"quoted\" value")));
        s.close(); // flushes the queue

        SqlStorageProvider again=SqlStorageProvider.sqlite(file,500,30,LOG);
        List<StoredEvidence> rows=again.recent(id,10);
        again.close();
        assertEquals(2,rows.size());
        assertEquals("Reach",rows.get(0).check());
        assertEquals("Speed",rows.get(1).check());
        assertTrue(rows.get(0).data().contains("\\\"quoted\\\""));
    }

    @Test void oldRowsArePurgedOnStartup()throws Exception{
        File file=Files.createTempFile("kac",".db").toFile();
        file.deleteOnExit();
        UUID id=UUID.randomUUID();
        SqlStorageProvider s=SqlStorageProvider.sqlite(file,500,30,LOG);
        s.save(id,new Evidence("Alex","Fly",.9,1.0,Instant.now().minusSeconds(90L*86400),Map.of()));
        s.save(id,new Evidence("Alex","Speed",.9,1.0,Instant.now(),Map.of()));
        s.close();
        SqlStorageProvider again=SqlStorageProvider.sqlite(file,500,30,LOG);
        List<StoredEvidence> rows=again.recent(id,10);
        again.close();
        assertEquals(1,rows.size());
        assertEquals("Speed",rows.get(0).check());
    }

    @Test void fullQueueDropsInsteadOfBlocking()throws Exception{
        File file=Files.createTempFile("kac",".db").toFile();
        file.deleteOnExit();
        SqlStorageProvider s=SqlStorageProvider.sqlite(file,50,30,LOG);
        UUID id=UUID.randomUUID();
        long begin=System.nanoTime();
        for(int i=0;i<5000;i++)s.save(id,new Evidence("X","Speed",.5,.1,Instant.now(),Map.of()));
        long ms=(System.nanoTime()-begin)/1_000_000L;
        s.close();
        assertTrue(ms<2000,"save() must never wait on the database, took "+ms+"ms");
    }
}
