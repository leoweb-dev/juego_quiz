# 📘 Documentación Técnica  
## Juego de Quiz en Java

---

## 1. Introducción

Este documento describe el diseño, la arquitectura y los fundamentos técnicos del desarrollo de un **juego de preguntas y respuestas (Quiz)** implementado en Java.  
El objetivo del proyecto es aplicar principios de **Programación Orientada a Objetos (POO)** para construir un sistema modular, extensible y fácil de mantener.

El juego permite presentar preguntas de opción múltiple, registrar respuestas del usuario, calcular un puntaje final y mostrar estadísticas de desempeño.

---

## 2. Requerimientos del Sistema

### 2.1 Requerimientos funcionales
- El sistema debe mostrar preguntas con opciones múltiples.
- El jugador debe seleccionar una respuesta para cada pregunta.
- El sistema debe validar las respuestas y actualizar el puntaje.
- Debe generarse un resultado final con puntaje y porcentaje.
- El sistema debe permitir agregar nuevas preguntas sin modificar la lógica principal.

### 2.2 Requerimientos no funcionales
- **Modularidad:** el código debe estar organizado en clases con responsabilidades claras.
- **Extensibilidad:** debe ser posible agregar nuevas preguntas o modos de juego.
- **Mantenibilidad:** el diseño debe facilitar modificaciones futuras.
- **Portabilidad:** el programa debe ejecutarse en cualquier entorno compatible con Java.

---

## 3. Diseño del Sistema

### 3.1 Arquitectura general

El sistema se organiza en tres capas:

- **Modelo:** contiene las clases principales del juego (Pregunta, Opcion, Quiz, Jugador, Resultado).
- **Controlador:** gestiona el flujo del juego (QuizController).
- **Vista:** maneja la interacción con el usuario (consola en esta versión).

Esta separación permite mantener un diseño limpio y escalable.

---

## 3.2 Descripción de Clases

### **Clase Pregunta**
Representa una pregunta del quiz.

**Atributos:**
- `String enunciado`
- `List<Opcion> opciones`
- `int respuestaCorrecta`

**Métodos:**
- `validarRespuesta(int opcionElegida)`
- `obtenerOpciones()`

---

### **Clase Opcion**
Representa una opción de respuesta.

**Atributos:**
- `String texto`
- `boolean esCorrecta`

**Métodos:**
- `esCorrecta()`

---

### **Clase Quiz**
Gestiona el conjunto de preguntas y el puntaje.

**Atributos:**
- `List<Pregunta> preguntas`
- `int puntaje`

**Métodos:**
- `iniciar()`
- `agregarPregunta(Pregunta p)`
- `evaluarRespuesta(int indicePregunta, int opcionElegida)`

---

### **Clase Jugador**
Representa al usuario que participa del quiz.

**Atributos:**
- `String nombre`
- `int puntajeFinal`

**Métodos:**
- `actualizarPuntaje(int puntos)`

---

### **Clase Resultado**
Encapsula el resultado final del quiz.

**Atributos:**
- `Jugador jugador`
- `int puntaje`
- `double porcentaje`

**Métodos:**
- `calcularPorcentaje(int totalPreguntas)`

---

## 3.3 Diagrama de Clases (UML)

```mermaid
classDiagram
    class Quiz {
        - List<Pregunta> preguntas
        - int puntaje
        + iniciar()
        + agregarPregunta(Pregunta p)
        + evaluarRespuesta(int indicePregunta, int opcionElegida)
    }

    class Pregunta {
        - String enunciado
        - List<Opcion> opciones
        - int respuestaCorrecta
        + validarRespuesta(int opcionElegida)
        + obtenerOpciones()
    }

    class Opcion {
        - String texto
        - boolean esCorrecta
        + esCorrecta()
    }

    class Jugador {
        - String nombre
        - int puntajeFinal
        + actualizarPuntaje(int puntos)
    }

    class Resultado {
        - Jugador jugador
        - int puntaje
        - double porcentaje
        + calcularPorcentaje(int totalPreguntas)
    }

    Quiz "1" --> "*" Pregunta
    Pregunta "1" --> "*" Opcion
    Resultado "1" --> "1" Jugador
