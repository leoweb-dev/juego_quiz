import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * "Base de datos" en un archivo JSON (datos.json).
 * Guarda docentes, estudiantes, preguntas y resultados.
 * Las contraseñas se guardan como hash SHA-256 con sal, nunca en texto plano.
 */
public class Database {

    // ================================================================ modelos

    public static class Usuario {
        public String nombre, apellido, usuario, salt, hash;
        public String grupo; // solo para estudiantes

        public String nombreCompleto() {
            return nombre + " " + apellido;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", nombre);
            m.put("apellido", apellido);
            m.put("usuario", usuario);
            if (grupo != null) m.put("grupo", grupo);
            m.put("salt", salt);
            m.put("hash", hash);
            return m;
        }

        static Usuario fromMap(Map<String, Object> m) {
            Usuario u = new Usuario();
            u.nombre = str(m, "nombre");
            u.apellido = str(m, "apellido");
            u.usuario = str(m, "usuario");
            u.salt = str(m, "salt");
            u.hash = str(m, "hash");
            u.grupo = m.get("grupo") == null ? null : str(m, "grupo");
            return u;
        }
    }

    public static class Pregunta {
        public long id;
        public String enunciado;
        public List<String> opciones = new ArrayList<>();
        public int correcta; // índice dentro de "opciones"
        public String grupo;

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("grupo", grupo);
            m.put("enunciado", enunciado);
            m.put("opciones", new ArrayList<Object>(opciones));
            m.put("correcta", correcta);
            return m;
        }

        @SuppressWarnings("unchecked")
        static Pregunta fromMap(Map<String, Object> m) {
            Pregunta p = new Pregunta();
            p.id = ((Number) m.get("id")).longValue();
            p.grupo = str(m, "grupo");
            p.enunciado = str(m, "enunciado");
            for (Object o : (List<Object>) m.get("opciones")) p.opciones.add(String.valueOf(o));
            p.correcta = ((Number) m.get("correcta")).intValue();
            return p;
        }
    }

    public static class Resultado {
        public String usuario, nombre, grupo, fecha;
        public int aciertos, total;

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("usuario", usuario);
            m.put("nombre", nombre);
            m.put("grupo", grupo);
            m.put("fecha", fecha);
            m.put("aciertos", aciertos);
            m.put("total", total);
            return m;
        }

        static Resultado fromMap(Map<String, Object> m) {
            Resultado r = new Resultado();
            r.usuario = str(m, "usuario");
            r.nombre = str(m, "nombre");
            r.grupo = str(m, "grupo");
            r.fecha = str(m, "fecha");
            r.aciertos = ((Number) m.get("aciertos")).intValue();
            r.total = ((Number) m.get("total")).intValue();
            return r;
        }
    }

    // ================================================================ estado

    private final Path archivo;
    private final List<Usuario> docentes = new ArrayList<>();
    private final List<Usuario> estudiantes = new ArrayList<>();
    private final List<Pregunta> preguntas = new ArrayList<>();
    private final List<Resultado> resultados = new ArrayList<>();
    private long siguienteId = 1;

    public Database(Path archivo) throws IOException {
        this.archivo = archivo;
        if (Files.exists(archivo)) cargar();
        else guardar();
    }

    /** Vuelve a leer el archivo (útil si otra instancia del programa lo modificó). */
    public synchronized void recargar() throws IOException {
        if (Files.exists(archivo)) cargar();
    }

    // ============================================================ persistencia

    @SuppressWarnings("unchecked")
    private void cargar() throws IOException {
        String texto = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        try {
            Object raiz = texto.trim().isEmpty() ? new LinkedHashMap<String, Object>() : JsonUtil.parse(texto);
            if (!(raiz instanceof Map)) throw new IllegalArgumentException("se esperaba un objeto JSON en la raíz");
            Map<String, Object> r = (Map<String, Object>) raiz;

            List<Usuario> d = new ArrayList<>();
            List<Usuario> e = new ArrayList<>();
            List<Pregunta> p = new ArrayList<>();
            List<Resultado> res = new ArrayList<>();
            for (Object o : lista(r, "docentes")) d.add(Usuario.fromMap((Map<String, Object>) o));
            for (Object o : lista(r, "estudiantes")) e.add(Usuario.fromMap((Map<String, Object>) o));
            for (Object o : lista(r, "preguntas")) p.add(Pregunta.fromMap((Map<String, Object>) o));
            for (Object o : lista(r, "resultados")) res.add(Resultado.fromMap((Map<String, Object>) o));

            long max = 0;
            for (Pregunta q : p) max = Math.max(max, q.id);
            Object sig = r.get("siguienteId");
            long siguiente = Math.max(max + 1, sig instanceof Number ? ((Number) sig).longValue() : 1);

            docentes.clear();
            docentes.addAll(d);
            estudiantes.clear();
            estudiantes.addAll(e);
            preguntas.clear();
            preguntas.addAll(p);
            resultados.clear();
            resultados.addAll(res);
            siguienteId = siguiente;
        } catch (RuntimeException ex) {
            throw new IOException("No se pudo leer " + archivo.toAbsolutePath() + ": " + ex.getMessage(), ex);
        }
    }

    private void guardar() {
        Map<String, Object> r = new LinkedHashMap<>();
        List<Object> d = new ArrayList<>();
        for (Usuario u : docentes) d.add(u.toMap());
        List<Object> e = new ArrayList<>();
        for (Usuario u : estudiantes) e.add(u.toMap());
        List<Object> p = new ArrayList<>();
        for (Pregunta q : preguntas) p.add(q.toMap());
        List<Object> res = new ArrayList<>();
        for (Resultado x : resultados) res.add(x.toMap());
        r.put("docentes", d);
        r.put("estudiantes", e);
        r.put("preguntas", p);
        r.put("resultados", res);
        r.put("siguienteId", siguienteId);

        try {
            Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(tmp, JsonUtil.stringify(r).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar " + archivo.toAbsolutePath() + ": " + ex.getMessage(), ex);
        }
    }

    // ============================================================ usuarios

    public synchronized Usuario registrarDocente(String nombre, String apellido, String usuario, String clave) {
        return registrar(docentes, nombre, apellido, usuario, clave, null);
    }

    public synchronized Usuario registrarEstudiante(String nombre, String apellido, String usuario,
                                                    String clave, String grupo) {
        if (limpio(grupo).isEmpty()) throw new IllegalArgumentException("Indica el grupo al que perteneces.");
        return registrar(estudiantes, nombre, apellido, usuario, clave, grupoCanonico(grupo));
    }

    private Usuario registrar(List<Usuario> destino, String nombre, String apellido, String usuario,
                              String clave, String grupo) {
        nombre = limpio(nombre);
        apellido = limpio(apellido);
        usuario = limpio(usuario);
        if (nombre.isEmpty() || apellido.isEmpty() || usuario.isEmpty() || clave == null || clave.isEmpty())
            throw new IllegalArgumentException("Completa todos los campos.");
        if (usuario.contains(" "))
            throw new IllegalArgumentException("El usuario no puede contener espacios.");
        if (clave.length() < 4)
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        if (existeUsuario(usuario))
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");

        Usuario u = new Usuario();
        u.nombre = nombre;
        u.apellido = apellido;
        u.usuario = usuario;
        u.grupo = grupo;
        u.salt = nuevaSal();
        u.hash = hash(u.salt, clave);
        destino.add(u);
        guardar();
        return u;
    }

    private boolean existeUsuario(String usuario) {
        for (Usuario u : docentes) if (u.usuario.equalsIgnoreCase(usuario)) return true;
        for (Usuario u : estudiantes) if (u.usuario.equalsIgnoreCase(usuario)) return true;
        return false;
    }

    /** Devuelve el usuario si las credenciales son correctas; si no, null. */
    public synchronized Usuario autenticar(boolean docente, String usuario, String clave) {
        usuario = limpio(usuario);
        for (Usuario u : docente ? docentes : estudiantes) {
            if (u.usuario.equalsIgnoreCase(usuario)) {
                byte[] a = hash(u.salt, clave == null ? "" : clave).getBytes(StandardCharsets.UTF_8);
                byte[] b = u.hash.getBytes(StandardCharsets.UTF_8);
                return MessageDigest.isEqual(a, b) ? u : null;
            }
        }
        return null;
    }

    public synchronized List<Usuario> estudiantes() {
        List<Usuario> copia = new ArrayList<>(estudiantes);
        copia.sort(Comparator.comparing((Usuario u) -> u.grupo, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(u -> u.apellido, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(u -> u.nombre, String.CASE_INSENSITIVE_ORDER));
        return copia;
    }

    // ============================================================== grupos

    /** Todos los grupos conocidos (de estudiantes registrados y de preguntas creadas). */
    public synchronized List<String> grupos() {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Usuario u : estudiantes) if (u.grupo != null) set.add(u.grupo);
        for (Pregunta p : preguntas) set.add(p.grupo);
        return new ArrayList<>(set);
    }

    private String grupoCanonico(String grupo) {
        grupo = limpio(grupo);
        for (String g : grupos()) if (g.equalsIgnoreCase(grupo)) return g;
        return grupo;
    }

    // ============================================================ preguntas

    /** Crea una pregunta (id == null) o actualiza la existente. */
    public synchronized Pregunta guardarPregunta(Long id, String enunciado, List<String> opciones,
                                                 int correcta, String grupo) {
        enunciado = enunciado == null ? "" : enunciado.trim();
        grupo = limpio(grupo);
        if (enunciado.isEmpty()) throw new IllegalArgumentException("Escribe el enunciado de la pregunta.");
        if (grupo.isEmpty()) throw new IllegalArgumentException("Indica el grupo al que se asigna la pregunta.");
        if (opciones == null || opciones.size() < 2)
            throw new IllegalArgumentException("Escribe al menos dos opciones de respuesta.");
        List<String> ops = new ArrayList<>();
        for (String o : opciones) {
            if (o == null || o.trim().isEmpty())
                throw new IllegalArgumentException("Las opciones no pueden estar vacías.");
            ops.add(o.trim());
        }
        if (correcta < 0 || correcta >= ops.size())
            throw new IllegalArgumentException("Marca cuál es la respuesta correcta.");

        String grupoFinal = grupoCanonico(grupo); // antes de agregar la pregunta nueva a la lista

        Pregunta p = null;
        if (id != null) {
            for (Pregunta q : preguntas) if (q.id == id) p = q;
            if (p == null) throw new IllegalArgumentException("La pregunta ya no existe.");
        } else {
            p = new Pregunta();
            p.id = siguienteId++;
            preguntas.add(p);
        }
        p.enunciado = enunciado;
        p.opciones = ops;
        p.correcta = correcta;
        p.grupo = grupoFinal;
        guardar();
        return p;
    }

    public synchronized void eliminarPregunta(long id) {
        preguntas.removeIf(p -> p.id == id);
        guardar();
    }

    public synchronized List<Pregunta> preguntas() {
        List<Pregunta> copia = new ArrayList<>(preguntas);
        copia.sort(Comparator.comparing((Pregunta p) -> p.grupo, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(p -> p.id));
        return copia;
    }

    public synchronized List<Pregunta> preguntasDeGrupo(String grupo) {
        List<Pregunta> r = new ArrayList<>();
        for (Pregunta p : preguntas()) if (p.grupo.equalsIgnoreCase(limpio(grupo))) r.add(p);
        return r;
    }

    // =========================================================== resultados

    public synchronized void guardarResultado(Usuario estudiante, int aciertos, int total) {
        Resultado r = new Resultado();
        r.usuario = estudiante.usuario;
        r.nombre = estudiante.nombreCompleto();
        r.grupo = estudiante.grupo;
        r.fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        r.aciertos = aciertos;
        r.total = total;
        resultados.add(r);
        guardar();
    }

    /** Resultados, del más reciente al más antiguo. */
    public synchronized List<Resultado> resultados() {
        List<Resultado> copia = new ArrayList<>(resultados);
        copia.sort(Comparator.comparing((Resultado r) -> r.fecha).reversed());
        return copia;
    }

    // ============================================================== helpers

    private static String str(Map<String, Object> m, String clave) {
        Object o = m.get(clave);
        if (o == null) throw new IllegalArgumentException("falta el campo \"" + clave + "\"");
        return o.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(Map<String, Object> m, String clave) {
        Object o = m.get(clave);
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String nuevaSal() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static String hash(String sal, String clave) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString((md.digest((sal + clave).getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
