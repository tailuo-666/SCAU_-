package addressbook.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("Unexpected trailing characters at position " + parser.index());
        }
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String str) {
            writeString(sb, str);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
            return;
        }
        if (value instanceof Object[] array) {
            List<Object> list = new ArrayList<>();
            for (Object item : array) {
                list.add(item);
            }
            writeValue(sb, list);
            return;
        }
        writeString(sb, value.toString());
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int j = hex.length(); j < 4; j++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
            this.index = 0;
        }

        private int index() {
            return index;
        }

        private boolean isEnd() {
            return index >= text.length();
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            char c = text.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect('}');
                break;
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect(']');
                break;
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isEnd()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (isEnd()) {
                    throw new IllegalArgumentException("Invalid escape at position " + index);
                }
                char esc = text.charAt(index++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape at position " + index);
                        }
                        String hex = text.substring(index, index + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("Unknown escape \\" + esc + " at position " + index);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Object parseLiteral(String literal, Object value) {
            if (text.startsWith(literal, index)) {
                index += literal.length();
                return value;
            }
            throw new IllegalArgumentException("Invalid token at position " + index);
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            readDigits();
            boolean floating = false;
            if (peek('.')) {
                floating = true;
                index++;
                readDigits();
            }
            if (peek('e') || peek('E')) {
                floating = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                readDigits();
            }
            String number = text.substring(start, index);
            if (number.isBlank() || number.equals("-")) {
                throw new IllegalArgumentException("Invalid number at position " + start);
            }
            try {
                if (floating) {
                    return Double.parseDouble(number);
                }
                return Long.parseLong(number);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + number, ex);
            }
        }

        private void readDigits() {
            int start = index;
            while (!isEnd() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected digit at position " + index);
            }
        }

        private boolean peek(char c) {
            return !isEnd() && text.charAt(index) == c;
        }

        private void expect(char c) {
            if (isEnd() || text.charAt(index) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at position " + index);
            }
            index++;
        }

        private void skipWhitespace() {
            while (!isEnd()) {
                char c = text.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
