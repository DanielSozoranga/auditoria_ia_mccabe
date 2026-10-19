# Auditoria del codigo

## 1. Complejidad ciclomatica V(G)

Se calculo a mano con V(G) = P + 1 (P son los puntos de decision, cada `&&` y `||` cuenta como uno) y se confirmo con ESLint (`npm run complejidad`).

| Funcion | P | V(G) a mano | ESLint | Tests de camino | Tests de limite |
|---|---|---|---|---|---|
| esPediatrico | 3 | 4 | 4 | 4 | 4 |
| validarTipoConsulta | 2 | 3 | 3 | 3 | 0 |
| validarHorario | 5 | 6 | 6 | 6 | 1 |
| hayChoque | 4 | 5 | 5 | 5 | 1 |
| calcularPenalizacion | 2 | 3 | 3 | 3 | 1 |
| agendarCita | 1 | 2 | 2 | 2 | 2 |
| cancelarCita | 2 | 3 | 3 | 3 | 0 |
| leerDatosCita | 4 | 5 | 5 | 5 | 0 |

Las demas funciones (base de datos, rutas de la API) tienen V(G) entre 1 y 5.

Complejidad maxima: 6. Ninguna funcion pasa de 10, no hace falta refactorizar. `npm run lint` falla si alguna llega a 11.

## 2. Caminos de calcularPenalizacion

El diagrama del grafo de flujo esta en docs/cfg_penalizacion.md y la comparacion de prompts en docs/prompts.md.

| Camino | tardia | esEmergencia | Resultado |
|---|---|---|---|
| 1 | falso | cualquiera | 0 |
| 2 | verdadero | falso | 15 |
| 3 | verdadero | verdadero | 0 |

## 3. Resultados de las herramientas

| Medida | Resultado |
|---|---|
| Tests | 54 en total |
| Cobertura de sentencias, ramas, funciones y lineas (Jest) | 100% |
| Cobertura de ramas con c8 | 100% |
| Mutantes con solo los tests por camino | 104 muertos y 7 sobrevivieron de 111 (93.69%) |
| Mutantes despues de agregar 9 tests | 111 muertos de 111 (100%) |

Los 54 tests se reparten asi:

1. 31 tests de camino, exactamente V(G) por funcion (reglas, servicio y leerDatosCita).
2. 14 tests de la base de datos, la API y los archivos del frontend.
3. 9 tests agregados despues de Stryker: 7 de limites en las reglas y 2 de que agendarCita aplica las reglas.

Los 7 mutantes que sobrevivieron al inicio mostraron que el 100% de cobertura era enganoso:

1. Cambiar `<` por `<=` en la edad minima, la edad maxima y el corte de 14 anios no hacia fallar ningun test.
2. Cambiar `<` por `<=` en la hora de apertura (8:00) no hacia fallar ninguno.
3. Cambiar `<` por `<=` en las 24 horas de la penalizacion no hacia fallar ninguno.
4. Borrar las llamadas a validarTipoConsulta y validarHorario dentro de agendarCita no hacia fallar ninguno, porque los tests del servicio solo probaban el choque de horarios.

Los mensajes de error se comparan de forma exacta desde el inicio, por eso no aparecieron mutantes de texto.

## 4. Cuestionario de auditoria

1. Conozco V(G) antes de leer los tests? Si. Se calculo a mano antes de escribir cualquier test y coincide con ESLint.
2. El 100% incluye aserciones fuertes? Ahora si. Al principio no: Stryker mostro 7 mutantes vivos con 100% de cobertura. Cada test compara el valor o el mensaje exacto.
3. Estan evaluadas todas las condiciones logicas? Parcial. Istanbul cuenta cada operando de `&&` y `||` como una rama, asi que se mide que cada condicion se evalue. Pero eso no es MC/DC, que exige probar que cada condicion cambia el resultado por si sola. Las condiciones se cubrieron a mano con un test por condicion.
4. Forcé fallos para ver si los tests los detectan? Si, con Stryker: 111 mutantes y los 111 detectados.
5. La complejidad es tan alta que conviene refactorizar? No. El maximo es 6.

## 5. Limites de este trabajo

1. Solo se aplican mutaciones a src/reglas.js y src/servicio.js. La API (src/app.js) y la base (src/db.js) tienen 100% de cobertura pero no se les corrieron mutaciones.
2. No se valida que la cita termine antes del cierre ni que cruce la hora de almuerzo, solo la hora de inicio.
3. Las citas son siempre de 30 minutos.
4. La zona horaria es la del servidor: las reglas de horario usan la hora local de la maquina donde corre.
