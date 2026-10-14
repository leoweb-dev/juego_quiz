# Preguntas y Respuestas (Java Swing)

Juego de preguntas y respuestas con interfaz gráfica. Sin librerías externas.

## Requisitos
- Java JDK 11 o superior (comprueba con `java -version` y `javac -version`).

## Compilar y ejecutar
En la carpeta del proyecto:

```
javac *.java
java Main
```

La base de datos es el archivo `datos.json`, que se crea solo en la carpeta desde donde ejecutas el programa.

## Cómo se usa
1. **Docente**: pantalla de inicio → *Soy docente* → pestaña *Registrarse* (nombre, apellido, usuario, contraseña) → *Iniciar sesión*.
   - Pestaña *Preguntas*: elige o escribe un grupo, escribe la pregunta, 2 a 4 opciones y marca la correcta. Al hacer clic en una pregunta de la tabla puedes editarla o eliminarla.
   - Pestaña *Estudiantes*: lista de estudiantes registrados por grupo (aparecen automáticamente).
   - Pestaña *Resultados*: intentos de los estudiantes.
2. **Estudiante**: *Soy estudiante* → *Registrarse* (nombre, apellido, usuario, contraseña y grupo) → *Iniciar sesión*. Juega con las preguntas que el docente asignó a su grupo; el resultado queda guardado.

## Notas
- Las preguntas las crea siempre el docente; el programa no genera ninguna.
- Las contraseñas se guardan con hash SHA-256 y sal, no en texto plano.
- El nombre de usuario es único entre docentes y estudiantes.
- Los grupos no distinguen mayúsculas/minúsculas ("3A" y "3a" son el mismo grupo).
