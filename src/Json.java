import java.util.*;

public final class Json {
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\r","\\r").replace("\n","\\n")
                .replace("<","\\u003c").replace(">","\\u003e");
    }
    public static String str(String s) { return "\"" + esc(s) + "\""; }

    public static String obj(Map<String,Object> m) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) b.append(",");
            first = false;
            b.append(str(e.getKey())).append(":");
            Object v = e.getValue();
            if (v == null) b.append("null");
            else if (v instanceof Number || v instanceof Boolean) b.append(v);
            else b.append(str(String.valueOf(v)));
        }
        return b.append("}").toString();
    }

    public static String arr(List<String> a) {
        return "[" + String.join(",", a) + "]";
    }
}
