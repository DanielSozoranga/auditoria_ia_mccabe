# Paridad entre la version Node.js y la version Java

La migracion conserva el contrato de la API para que el frontend con Alpine.js funcione sin cambios. Este documento lista que es igual y que cambio a proposito.

## Herramientas equivalentes

| Node.js | Java |
|---|---|
| Express | Spring Boot (Web MVC) |
| better-sqlite3 | SQLite JDBC con Spring JdbcClient |
| Jest y Supertest | JUnit 5, AssertJ y MockMvc |
| Istanbul y c8 | JaCoCo |
| Stryker | PIT |
| ESLint (regla complexity) | Checkstyle (CyclomaticComplexity) |
| `.nvmrc` y nvm | Java 21 y Maven Wrapper |
| npm | Maven |

## Contrato de la API

| Ruta | Respuesta en las dos versiones |
|---|---|
| `GET /api/citas` | 200 con la lista de citas, mismos campos: id, paciente, tipo, inicio, fin, estado, penalizacion |
| `POST /api/citas` | 201 con `{"id": n}` |
| `POST /api/citas/{id}/cancelar` | 200 con `{"penalizacion": n}` |
| Regla de negocio rota | 422 con `{"error": "..."}` y el mismo mensaje |
| Datos faltantes o fecha invalida | 400 con `{"error": "Faltan datos o la fecha es invalida"}` |
| JSON roto | 400 con `{"error": "Peticion invalida"}` |
| Fallo inesperado | 500 con `{"error": "Error del servidor"}` |

Los mensajes de las 5 reglas son identicos en las dos versiones.

## Diferencias deliberadas

| Caso | Node.js | Java | Por que |
|---|---|---|---|
| Hora de las reglas | La del servidor | La del consultorio (`America/Guayaquil`) | Las reglas dependian de donde corriera el servidor |
| Formato de fechas en la respuesta | UTC, por ejemplo `2026-10-19T14:00:00.000Z` | Con desfase, por ejemplo `2026-10-19T09:00:00-05:00` | Es el mismo instante, y el desfase deja claro en que hora se agenda |
| Fecha enviada sin zona | La zona del servidor | La hora del consultorio | Lo mismo que el punto anterior |
| Fecha enviada con zona | Se acepta | Se acepta y se convierte a la hora del consultorio | Igual comportamiento, ahora con prueba |
| POST sin cuerpo | 400 con "Faltan datos o la fecha es invalida" | 400 con "Peticion invalida" | Lo decide el framework antes de validar los campos |
| Edad con decimales (5.5) | 422 con "Edad fuera de rango" | 400 con "Peticion invalida" | El campo es un entero, y los decimales se rechazan antes de llegar a las reglas |
| Id no numerico al cancelar | No se valida de forma expresa | 400 con "Peticion invalida" | La ruta exige un numero |
| Dinero | Numero de JavaScript | `BigDecimal`, guardado como texto en la base | Sin errores de redondeo |
| Cuerpo del 500 | Mensaje generico | Mensaje generico, y el detalle va solo al registro | Igual de seguro |

## Auditoria: V(G) por funcion

| Funcion | V(G) en Node | V(G) en Java | Motivo de la diferencia |
|---|---|---|---|
| esPediatrico | 4 | 3 | El tipo `int` ya garantiza que es entero, desaparece una condicion |
| validarTipoConsulta | 3 | 3 | |
| validarHorario | 6 | 6 | |
| hayChoque | 5 | 5 | |
| calcularPenalizacion | 3 | 3 | |
| agendarCita | 2 | 2 | |
| cancelarCita | 3 | 2 | `orElseThrow` reemplaza un `if`, y la lambda cuenta aparte |
| Lectura de la fecha | 5 (`leerDatosCita`) | 3 (`ConversorFechas`) | Los campos obligatorios los valida Bean Validation con anotaciones |

La complejidad maxima es 6 en las dos versiones.
