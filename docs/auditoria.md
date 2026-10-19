# Auditoria del codigo

## 1. Complejidad ciclomatica V(G)

Se calculo a mano con V(G) = P + 1 (P son los puntos de decision, cada `and` y `or` cuenta como uno) y se confirmo con radon.

| Funcion | P | V(G) a mano | radon | Tests de camino | Tests de limite |
|---|---|---|---|---|---|
| es_pediatrico | 2 | 3 | 3 | 3 | 4 |
| validar_tipo_consulta | 2 | 3 | 3 | 3 | 0 |
| validar_horario | 4 | 5 | 5 | 5 | 1 |
| hay_choque | 4 | 5 | 5 | 5 | 1 |
| calcular_penalizacion | 2 | 3 | 3 | 3 | 1 |
| agendar_cita | 1 | 2 | 2 | 2 | 0 |
| cancelar_cita | 2 | 3 | 3 | 3 | 0 |

Complejidad maxima: 5. Ninguna funcion pasa de 10, no hace falta refactorizar.

## 2. Caminos de calcular_penalizacion

| Camino | tardia | es_emergencia | Resultado |
|---|---|---|---|
| 1 | falso | cualquiera | 0.0 |
| 2 | verdadero | falso | 15.0 |
| 3 | verdadero | verdadero | 0.0 |

## 3. Resultados de las herramientas

| Medida | Resultado |
|---|---|
| Tests | 31 (24 de camino y 7 de limite) |
| Cobertura de sentencias | 100% |
| Cobertura de ramas | 100% (24 de 24 ramas) |
| Mutantes con solo los tests de camino | 41 muertos y 15 sobrevivieron de 56 |
| Mutantes despues de agregar limites y mensajes exactos | 56 muertos de 56 |

Los 15 mutantes que sobrevivieron al inicio mostraron que el 100% de cobertura era enganoso:

1. Cambiar `<` por `<=` en las edades (0, 14, 120) y en la hora de apertura no fallaba ningun test.
2. Cambiar `continue` por `break` en hay_choque no fallaba ninguno.
3. Cambiar 24h por "24h o menos" en la penalizacion no fallaba ninguno.
4. Alterar el texto de los errores no fallaba ninguno, porque `match` busca solo un pedazo del mensaje.

Se agregaron 7 tests de limite y se pasaron los mensajes a comparacion exacta.

## 4. Cuestionario de auditoria

1. Conozco V(G) antes de leer los tests? Si. Se calculo a mano antes de escribir cualquier test y coincide con radon.
2. El 100% incluye aserciones fuertes? Ahora si. Al principio no: mutmut mostro 15 mutantes vivos con 100% de cobertura. Cada test compara el valor o el mensaje exacto.
3. Estan evaluadas todas las condiciones logicas? Parcial. coverage.py mide ramas, no MC/DC. Las condiciones con `or` y `and` se cubrieron a mano con un test por condicion (por ejemplo edad negativa y edad mayor a 120 por separado), pero la herramienta no lo verifica.
4. Forcé fallos para ver si los tests los detectan? Si, con mutmut: 56 mutantes y los 56 detectados.
5. La complejidad es tan alta que conviene refactorizar? No. El maximo es 5.

## 5. Limites de este trabajo

1. El PDF propone Node.js, Express y Alpine.js. Esta version usa Python y SQLite, y no incluye frontend ni API web.
2. No se valida que la cita termine antes del cierre ni que cruce la hora de almuerzo, solo la hora de inicio.
3. Las citas se agendan siempre de 30 minutos.
