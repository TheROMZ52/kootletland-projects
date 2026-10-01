package ir.kootletland.ac.wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Plugin-message payload sent by a backend and decoded by the proxy. Plain Java on purpose: the Velocity module
 * compiles this exact source file, so both sides can never drift apart.
 */
public record EvidenceMessage(String server,String player,UUID uuid,String check,double confidence,double violation,long timestampMillis) {
    public static final String CHANNEL="kootletlandac:evidence";
    public static final int VERSION=1;
    private static final int MAX_TEXT=64;

    public byte[] encode()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(96);
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeByte(VERSION);
        out.writeUTF(limit(server));
        out.writeUTF(limit(player));
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeUTF(limit(check));
        out.writeDouble(confidence);
        out.writeDouble(violation);
        out.writeLong(timestampMillis);
        out.flush();
        return bytes.toByteArray();
    }

    /** Rejects anything malformed: wrong version, oversized text, non-finite or out-of-range numbers, trailing bytes. */
    public static EvidenceMessage decode(byte[] data)throws IOException{
        if(data==null||data.length<10||data.length>512)throw new IOException("bad length");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(data));
        int version=in.readUnsignedByte();
        if(version!=VERSION)throw new IOException("unsupported version "+version);
        String server=text(in.readUTF());
        String player=text(in.readUTF());
        UUID uuid=new UUID(in.readLong(),in.readLong());
        String check=text(in.readUTF());
        double confidence=in.readDouble();
        double violation=in.readDouble();
        long time=in.readLong();
        if(!Double.isFinite(confidence)||confidence<0||confidence>1)throw new IOException("confidence out of range");
        if(!Double.isFinite(violation)||violation<0||violation>1000)throw new IOException("violation out of range");
        if(in.available()>0)throw new IOException("trailing bytes");
        return new EvidenceMessage(server,player,uuid,check,confidence,violation,time);
    }

    private static String text(String s)throws IOException{
        if(s.length()>MAX_TEXT)throw new IOException("text too long");
        return s;
    }

    private static String limit(String s){
        if(s==null)return "";
        return s.length()<=MAX_TEXT?s:s.substring(0,MAX_TEXT);
    }
}
