package ir.kootletland.ac.storage;

import java.util.Collection;
import java.util.Map;

/** Minimal JSON writer for evidence data (no dependency, no reflection). */
public final class JsonMini {
    private JsonMini(){}

    public static String toJson(Object value){
        StringBuilder sb=new StringBuilder();
        write(sb,value,0);
        return sb.toString();
    }

    private static void write(StringBuilder sb,Object v,int depth){
        if(v==null||depth>6){sb.append("null");return;}
        if(v instanceof Boolean b){sb.append(b);return;}
        if(v instanceof Number n){
            double d=n.doubleValue();
            if(Double.isNaN(d)||Double.isInfinite(d))sb.append("null");
            else if(v instanceof Double||v instanceof Float)sb.append(d);
            else sb.append(n.longValue());
            return;
        }
        if(v instanceof Map<?,?> map){
            sb.append('{');
            boolean first=true;
            for(Map.Entry<?,?> e:map.entrySet()){
                if(!first)sb.append(',');
                first=false;
                string(sb,String.valueOf(e.getKey()));
                sb.append(':');
                write(sb,e.getValue(),depth+1);
            }
            sb.append('}');
            return;
        }
        if(v instanceof Collection<?> c){
            sb.append('[');
            boolean first=true;
            for(Object o:c){
                if(!first)sb.append(',');
                first=false;
                write(sb,o,depth+1);
            }
            sb.append(']');
            return;
        }
        string(sb,String.valueOf(v));
    }

    private static void string(StringBuilder sb,String s){
        sb.append('"');
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if(c<0x20)sb.append(String.format("\\u%04x",(int)c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
