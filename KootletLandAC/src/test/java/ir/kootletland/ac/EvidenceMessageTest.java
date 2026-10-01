package ir.kootletland.ac;

import ir.kootletland.ac.wire.EvidenceMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceMessageTest {
    private static EvidenceMessage sample(double confidence){
        return new EvidenceMessage("lobby","Steve",UUID.randomUUID(),"Speed",confidence,7.5,1_700_000_000_000L);
    }

    @Test void roundTrips()throws IOException{
        EvidenceMessage in=sample(.91);
        assertEquals(in,EvidenceMessage.decode(in.encode()));
    }

    @Test void rejectsGarbage()throws IOException{
        assertThrows(IOException.class,()->EvidenceMessage.decode(null));
        assertThrows(IOException.class,()->EvidenceMessage.decode(new byte[]{1,2,3}));
        byte[] valid=sample(.9).encode();
        valid[0]=9; // unknown version
        assertThrows(IOException.class,()->EvidenceMessage.decode(valid));
    }

    @Test void rejectsOutOfRangeAndTrailingData()throws IOException{
        assertThrows(IOException.class,()->EvidenceMessage.decode(sample(1.5).encode()));
        assertThrows(IOException.class,()->EvidenceMessage.decode(sample(Double.NaN).encode()));
        byte[] valid=sample(.9).encode();
        byte[] longer=java.util.Arrays.copyOf(valid,valid.length+1);
        assertThrows(IOException.class,()->EvidenceMessage.decode(longer));
    }

    @Test void rejectsOversizedText()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeByte(EvidenceMessage.VERSION);
        out.writeUTF("x".repeat(200));
        out.writeUTF("p");
        out.writeLong(1);out.writeLong(2);
        out.writeUTF("c");
        out.writeDouble(.5);out.writeDouble(1);out.writeLong(5);
        assertThrows(IOException.class,()->EvidenceMessage.decode(bytes.toByteArray()));
    }
}
