package biblioteca.util;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Leitor/escritor de JSON mínimo (o projeto não usa bibliotecas externas). */
public final class Json {

    private Json() {
    }

    // ---------- escrita ----------

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(valor, sb);
        return sb.toString();
    }

    private static void escrever(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            sb.append(v);
        } else if (v instanceof Number n) {
            sb.append(n.doubleValue());
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                texto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean primeiro = true;
            for (Object o : it) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escrever(o, sb);
            }
            sb.append(']');
        } else if (v instanceof Temporal || v instanceof Enum<?>) {
            texto(v.toString(), sb);
        } else {
            texto(v.toString(), sb);
        }
    }

    private static void texto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ---------- leitura ----------

    /** Converte JSON em Map, List, String, Long, Double, Boolean ou null. */
    public static Object ler(String json) {
        Leitor l = new Leitor(json);
        l.pular();
        Object v = l.valor();
        l.pular();
        if (l.pos != json.length()) {
            throw new IllegalArgumentException("JSON inválido");
        }
        return v;
    }

    private static final class Leitor {
        private final String s;
        private int pos = 0;

        Leitor(String s) {
            this.s = s;
        }

        void pular() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        char atual() {
            if (pos >= s.length()) {
                throw new IllegalArgumentException("JSON incompleto");
            }
            return s.charAt(pos);
        }

        void esperar(char c) {
            if (atual() != c) {
                throw new IllegalArgumentException("JSON inválido na posição " + pos);
            }
            pos++;
        }

        Object valor() {
            char c = atual();
            if (c == '{') {
                return objeto();
            }
            if (c == '[') {
                return lista();
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return numero();
        }

        Map<String, Object> objeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            esperar('{');
            pular();
            if (atual() == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                pular();
                String chave = string();
                pular();
                esperar(':');
                pular();
                mapa.put(chave, valor());
                pular();
                if (atual() == ',') {
                    pos++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        List<Object> lista() {
            List<Object> itens = new ArrayList<>();
            esperar('[');
            pular();
            if (atual() == ']') {
                pos++;
                return itens;
            }
            while (true) {
                pular();
                itens.add(valor());
                pular();
                if (atual() == ',') {
                    pos++;
                } else {
                    esperar(']');
                    return itens;
                }
            }
        }

        String string() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = atual();
                pos++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = atual();
                    pos++;
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object numero() {
            int inicio = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            String t = s.substring(inicio, pos);
            if (t.isEmpty()) {
                throw new IllegalArgumentException("JSON inválido na posição " + pos);
            }
            if (t.contains(".") || t.contains("e") || t.contains("E")) {
                return Double.valueOf(t);
            }
            return Long.valueOf(t);
        }
    }
}
