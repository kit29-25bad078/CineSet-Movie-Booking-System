import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HttpUtil {
    public static String body(HttpExchange e) throws IOException {
        return new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static Map<String,String> query(String raw) {
        Map<String,String> m = new HashMap<>();
        if (raw == null || raw.isBlank()) return m;
        for (String p : raw.split("&")) {
            String[] a = p.split("=",2);
            if (a.length == 2)
                m.put(URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                      URLDecoder.decode(a[1], StandardCharsets.UTF_8));
        }
        return m;
    }

    public static String field(String json, String key) {
        String n = "\"" + key + "\"";
        int i = json.indexOf(n);
        if (i < 0) return "";
        i = json.indexOf(':', i + n.length());
        if (i < 0) return "";
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i < json.length() && json.charAt(i) == '"') {
            i++;
            StringBuilder b = new StringBuilder();
            boolean esc = false;
            while (i < json.length()) {
                char c = json.charAt(i++);
                if (esc) { b.append(c); esc = false; }
                else if (c == '\\') esc = true;
                else if (c == '"') break;
                else b.append(c);
            }
            return b.toString();
        }
        int j = i;
        while (j < json.length() && ",}".indexOf(json.charAt(j)) < 0) j++;
        return json.substring(i,j).trim();
    }

    public static String[] arrayField(String json, String key) {
        String n = "\"" + key + "\"";
        int i = json.indexOf(n);
        if (i < 0) return new String[0];
        i = json.indexOf('[', i+n.length());
        int j = json.indexOf(']', i);
        if (i < 0 || j < 0) return new String[0];
        String x = json.substring(i+1,j).trim();
        if (x.isBlank()) return new String[0];
        return Arrays.stream(x.split(","))
                .map(v -> v.trim().replace("\"",""))
                .toArray(String[]::new);
    }

    public static void send(HttpExchange e, int code, String content, String type) throws IOException {
        byte[] d = content.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        e.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        e.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
        e.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        e.sendResponseHeaders(code,d.length);
        try(OutputStream o=e.getResponseBody()){o.write(d);}
    }

    public static void json(HttpExchange e,int code,String content)throws IOException {
        send(e,code,content,"application/json");
    }
}
