package storage;

import java.util.*;

/** One line: key: value, key: value. Escapes protect user-entered text. */
final class RecordCodec {
    private RecordCodec() { }

    static String line(String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Key/value pairs required.");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) text.append(", ");
            text.append(pairs[i]).append(": ").append(encode(pairs[i + 1]));
        }
        return text.toString();
    }
    static String encode(String value) {
        if (value == null) return "NONE";
        if (value.equals("NONE")) return "\\0"; // Distinguish literal text from null.
        return value.replace("\\", "\\\\").replace(",", "\\,")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    static String decode(String value) {
        if (value.equals("NONE")) return null;
        if (value.equals("\\0")) return "NONE";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                if (++i == value.length()) throw new IllegalArgumentException("Incomplete escape.");
                c = value.charAt(i);
                if (c == 'n') c = '\n';
                else if (c == 'r') c = '\r';
                else if (c == 't') c = '\t';
                else if (c != '\\' && c != ',') throw new IllegalArgumentException("Unknown escape.");
            }
            result.append(c);
        }
        return result.toString();
    }
    static Map<String, String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean escaped = false;
        for (char c : line.toCharArray()) {
            if (c == ',' && !escaped) {
                fields.add(field.toString()); field.setLength(0);
            } else field.append(c);
            if (c == '\\') escaped = !escaped;
            else escaped = false;
        }
        if (escaped) throw new IllegalArgumentException("Incomplete escape.");
        fields.add(field.toString());
        Map<String, String> result = new LinkedHashMap<>();
        for (String part : fields) {
            int colon = part.indexOf(':'); // Split at FIRST colon; dates contain more.
            if (colon < 1) throw new IllegalArgumentException("Expected key: value.");
            String key = part.substring(0, colon).trim();
            if (!key.matches("[A-Za-z][A-Za-z0-9]*") || result.containsKey(key)) {
                throw new IllegalArgumentException("Invalid or duplicate field: " + key);
            }
            String value = part.substring(colon + 1);
            if (value.startsWith(" ")) value = value.substring(1);
            result.put(key, decode(value));
        }
        return result;
    }
}
