package ir.kootletland.ac;

import ir.kootletland.ac.storage.JsonMini;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMiniTest {
    @Test void writesNumbersBooleansAndNestedValues(){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("a",1);
        m.put("b",2.5);
        m.put("c",true);
        m.put("d",List.of(1,2));
        m.put("e",Map.of("k","v"));
        assertEquals("{\"a\":1,\"b\":2.5,\"c\":true,\"d\":[1,2],\"e\":{\"k\":\"v\"}}",JsonMini.toJson(m));
    }

    @Test void escapesStringsAndNeutralisesNonFiniteNumbers(){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("q","say \"hi\"\n\\");
        m.put("nan",Double.NaN);
        m.put("inf",Double.POSITIVE_INFINITY);
        m.put("none",null);
        assertEquals("{\"q\":\"say \\\"hi\\\"\\n\\\\\",\"nan\":null,\"inf\":null,\"none\":null}",JsonMini.toJson(m));
    }
}
