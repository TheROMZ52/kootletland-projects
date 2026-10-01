package ir.kootletland.ac.storage;

import ir.kootletland.ac.model.Evidence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite or MySQL evidence log. Writes go through a bounded queue to one worker thread (batched), so the main thread
 * never waits on the database. If the queue is full, new rows are dropped and counted instead of blocking.
 * Uses plain JDBC; Paper ships the SQLite and MySQL drivers.
 */
public final class SqlStorageProvider implements StorageProvider {
    public interface ConnectionFactory { Connection open() throws SQLException; }

    private record Row(UUID id,Evidence evidence){}

    private static final int BATCH=100;
    private final ConnectionFactory factory;
    private final boolean mysql;
    private final String label;
    private final Logger logger;
    private final long retentionMillis;
    private final BlockingQueue<Row> queue;
    private final AtomicLong dropped=new AtomicLong();
    private final Thread worker;
    private volatile boolean running=true;
    private Connection connection;

    public static SqlStorageProvider sqlite(File file,int queueSize,int retentionDays,Logger logger)throws SQLException{
        File parent=file.getAbsoluteFile().getParentFile();
        if(parent!=null&&!parent.exists())parent.mkdirs();
        String url="jdbc:sqlite:"+file.getAbsolutePath();
        return new SqlStorageProvider(()->DriverManager.getConnection(url),false,"sqlite",queueSize,retentionDays,logger);
    }

    public static SqlStorageProvider mysql(String host,int port,String database,String user,String password,int queueSize,int retentionDays,Logger logger)throws SQLException{
        String url="jdbc:mysql://"+host+":"+port+"/"+database+"?useSSL=false&characterEncoding=utf8&connectTimeout=5000";
        return new SqlStorageProvider(()->DriverManager.getConnection(url,user,password),true,"mysql",queueSize,retentionDays,logger);
    }

    public SqlStorageProvider(ConnectionFactory factory,boolean mysql,String label,int queueSize,int retentionDays,Logger logger)throws SQLException{
        this.factory=factory;
        this.mysql=mysql;
        this.label=label;
        this.logger=logger;
        this.retentionMillis=Math.max(0,retentionDays)*86_400_000L;
        this.queue=new ArrayBlockingQueue<>(Math.max(50,queueSize));
        this.connection=factory.open();
        createSchema();
        purgeOld();
        this.worker=new Thread(this::run,"KootletLandAC-storage");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override public String name(){return label;}
    public long dropped(){return dropped.get();}

    @Override public void save(UUID player,Evidence evidence){
        if(!running)return;
        if(!queue.offer(new Row(player,evidence)))dropped.incrementAndGet();
    }

    @Override public synchronized List<StoredEvidence> recent(UUID player,int limit){
        List<StoredEvidence> out=new ArrayList<>();
        try{
            ensureConnection();
            try(PreparedStatement ps=connection.prepareStatement("SELECT player_name,check_name,confidence,violation,created_at,data FROM kac_evidence WHERE player_uuid=? ORDER BY created_at DESC, id DESC LIMIT ?")){
                ps.setString(1,player.toString());
                ps.setInt(2,Math.max(1,Math.min(200,limit)));
                try(ResultSet rs=ps.executeQuery()){
                    while(rs.next())out.add(new StoredEvidence(rs.getString(1),rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getLong(5),rs.getString(6)));
                }
            }
        }catch(SQLException e){
            logger.log(Level.WARNING,"KootletLandAC storage query failed",e);
        }
        return out;
    }

    @Override public void close(){
        running=false;
        worker.interrupt();
        try{worker.join(4000);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        flushRemaining();
        synchronized(this){
            try{if(connection!=null)connection.close();}catch(SQLException ignored){}
        }
    }

    private synchronized void ensureConnection()throws SQLException{
        if(connection==null||connection.isClosed())connection=factory.open();
    }

    private synchronized void createSchema()throws SQLException{
        try(Statement st=connection.createStatement()){
            if(mysql){
                st.executeUpdate("CREATE TABLE IF NOT EXISTS kac_evidence (id BIGINT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, player_name VARCHAR(32) NOT NULL, check_name VARCHAR(32) NOT NULL, confidence DOUBLE NOT NULL, violation DOUBLE NOT NULL, created_at BIGINT NOT NULL, data TEXT, INDEX idx_kac_player (player_uuid, created_at), INDEX idx_kac_time (created_at))");
            }else{
                st.executeUpdate("CREATE TABLE IF NOT EXISTS kac_evidence (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, check_name TEXT NOT NULL, confidence REAL NOT NULL, violation REAL NOT NULL, created_at INTEGER NOT NULL, data TEXT)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kac_player ON kac_evidence (player_uuid, created_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kac_time ON kac_evidence (created_at)");
            }
        }
    }

    private synchronized void purgeOld(){
        if(retentionMillis<=0)return;
        try(PreparedStatement ps=connection.prepareStatement("DELETE FROM kac_evidence WHERE created_at<?")){
            ps.setLong(1,System.currentTimeMillis()-retentionMillis);
            ps.executeUpdate();
        }catch(SQLException e){
            logger.log(Level.WARNING,"KootletLandAC storage purge failed",e);
        }
    }

    private void run(){
        long lastPurge=System.currentTimeMillis();
        List<Row> batch=new ArrayList<>(BATCH);
        while(running){
            try{
                Row first=queue.poll(1,TimeUnit.SECONDS);
                if(first!=null){
                    batch.add(first);
                    queue.drainTo(batch,BATCH-1);
                    write(batch);
                    batch.clear();
                }
                long now=System.currentTimeMillis();
                if(now-lastPurge>3_600_000L){lastPurge=now;purgeOld();}
            }catch(InterruptedException e){
                break;
            }catch(Throwable t){
                logger.log(Level.WARNING,"KootletLandAC storage worker error",t);
                batch.clear();
            }
        }
    }

    private void flushRemaining(){
        List<Row> rest=new ArrayList<>();
        queue.drainTo(rest);
        if(!rest.isEmpty())write(rest);
    }

    private synchronized void write(List<Row> rows){
        try{
            ensureConnection();
            boolean auto=connection.getAutoCommit();
            connection.setAutoCommit(false);
            try(PreparedStatement ps=connection.prepareStatement("INSERT INTO kac_evidence (player_uuid,player_name,check_name,confidence,violation,created_at,data) VALUES (?,?,?,?,?,?,?)")){
                for(Row r:rows){
                    Evidence e=r.evidence();
                    ps.setString(1,r.id().toString());
                    ps.setString(2,cut(e.player(),32));
                    ps.setString(3,cut(e.check(),32));
                    ps.setDouble(4,e.confidence());
                    ps.setDouble(5,e.violation());
                    ps.setLong(6,e.timestamp().toEpochMilli());
                    ps.setString(7,JsonMini.toJson(e.data()));
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
            }catch(SQLException e){
                connection.rollback();
                throw e;
            }finally{
                connection.setAutoCommit(auto);
            }
        }catch(SQLException e){
            logger.log(Level.WARNING,"KootletLandAC storage write failed ("+rows.size()+" rows lost)",e);
            try{if(connection!=null)connection.close();}catch(SQLException ignored){}
            connection=null;
        }
    }

    private static String cut(String s,int max){
        if(s==null)return "";
        return s.length()<=max?s:s.substring(0,max);
    }
}
