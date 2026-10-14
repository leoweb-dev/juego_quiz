import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector/escritor JSON mínimo (sin dependencias externas).
 * Tipos: Map (objeto), List (arreglo), String, Long/Double, Boolean y null.
 */
public final class JsonUtil {

    private JsonUtil() {}

    // ------------------------------------------------------------------ escribir

    public static String stringify(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, valor, 0);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object o, int nivel) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String) {
            comillas(sb, (String) o);
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sangria(sb, nivel + 1);
                comillas(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                escribir(sb, e.getValue(), nivel + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            sangria(sb, nivel);
            sb.append('}');
        } else if (o instanceof List) {
            List<?> l = (List<?>) o;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                sangria(sb, nivel + 1);
                escribir(sb, l.get(i), nivel + 1);
                if (i < l.size() - 1) sb.append(',');
                sb.append('\n');
            }
            sangria(sb, nivel);
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Tipo no soportado en JSON: " + o.getClass());
        }
    }

    private static void sangria(StringBuilder sb, int nivel) {
        for (int i = 0; i < nivel; i++) sb.append("  ");
    }

    private static void comillas(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------- leer

    public static Object parse(String texto) {
        Lector p = new Lector(texto);
        p.espacios();
        Object v = p.valor();
        p.espacios();
        if (p.i != texto.length()) throw p.error("contenido inesperado después del valor");
        return v;
    }

    private static final class Lector {
        final String s;
        int i = 0;

        Lector(String s) {
            this.s = s;
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON inválido (posición " + i + "): " + msg);
        }

        void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        char ver() {
            if (i >= s.length()) throw error("fin de texto inesperado");
            return s.charAt(i);
        }

        void esperar(String palabra) {
            if (!s.startsWith(palabra, i)) throw error("se esperaba " + palabra);
            i += palabra.length();
        }

        Object valor() {
            char c = ver();
            switch (c) {
                case '{': return objeto();
                case '[': return arreglo();
                case '"': return cadena();
                case 't': esperar("true");  return Boolean.TRUE;
                case 'f': esperar("false"); return Boolean.FALSE;
                case 'n': esperar("null");  return null;
                default:  return numero();
            }
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            espacios();
            if (ver() == '}') {
                i++;
                return m;
            }
            while (true) {
                espacios();
                if (ver() != '"') throw error("se esperaba una clave entre comillas");
                String clave = cadena();
                espacios();
                if (ver() != ':') throw error("se esperaba ':'");
                i++;
                espacios();
                m.put(clave, valor());
                espacios();
                char c = ver();
                i++;
                if (c == '}') return m;
                if (c != ',') throw error("se esperaba ',' o '}'");
            }
        }

        List<Object> arreglo() {
            List<Object> l = new ArrayList<>();
            i++; // [
            espacios();
            if (ver() == ']') {
                i++;
                return l;
            }
            while (true) {
                espacios();
                l.add(valor());
                espacios();
                char c = ver();
                i++;
                if (c == ']') return l;
                if (c != ',') throw error("se esperaba ',' o ']'");
            }
        }

        String cadena() {
            StringBuilder sb = new StringBuilder();
            i++; // comilla de apertura
            while (true) {
                char c = ver();
                i++;
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = ver();
                i++;
                switch (e) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw error("secuencia \\u incompleta");
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("secuencia \\u inválida");
                        }
                        i += 4;
                        break;
                    default:
                        throw error("escape inválido");
                }
            }
        }

        Number numero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (inicio == i) throw error("valor inesperado");
            String t = s.substring(inicio, i);
            try {
                if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.valueOf(t);
                return Long.valueOf(t);
            } catch (NumberFormatException ex) {
                throw error("número inválido");
            }
        }
    }
}
