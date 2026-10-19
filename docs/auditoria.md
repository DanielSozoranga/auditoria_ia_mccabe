# Auditoria del codigo (version Java)

## 1. Complejidad ciclomatica V(G)

Se calculo a mano con V(G) = P + 1 (P son los puntos de decision, cada `&&` y `||` cuenta como uno) y se confirmo con Checkstyle (`./mvnw checkstyle:check@complejidad`).

| Metodo | P | V(G) a mano | Checkstyle | Tests de camino | Limites y casos extra |
|---|---|---|---|---|---|
| Reglas.esPediatrico | 2 | 3 | 3 | 3 | 4 |
| Reglas.validarTipoConsulta | 2 | 3 | 3 | 3 | 0 |
| Reglas.tipoDesdeTexto | 0 | 1 | 1 | 1 | 1 |
| Reglas.validarHorario | 5 | 6 | 6 | 6 | 2 |
| Reglas.hayChoque | 4 | 5 | 5 | 5 | 1 |
| Reglas.calcularPenalizacion | 2 | 3 | 3 | 3 | 2 |
| ServicioCitas.agendar | 1 | 2 | 2 | 2 | 3 |
| ServicioCitas.cancelar | 1 | 2 | 2 | 2 | 3 |
| ConversorFechas.aHoraLocal | 2 | 3 | 3 | 3 | 1 |

El resto de los metodos (repositorio, controlador, manejo de errores) tiene V(G) 1 o 2, salvo el cancelar del controlador que vale 2.

Complejidad maxima: 6. Ninguna funcion pasa de 10 y Checkstyle hace fallar la compilacion si alguna llega a 11, asi que no hace falta refactorizar.

## 2. Caminos de calcularPenalizacion

El diagrama del grafo de flujo esta en `docs/cfg_penalizacion.md` y la comparacion de prompts en `docs/prompts.md`.

| Camino | tardia | esEmergencia | Resultado |
|---|---|---|---|
| 1 | falso | cualquiera | 0 |
| 2 | verdadero | falso | 15 |
| 3 | verdadero | verdadero | 0 |

## 3. Resultados de las herramientas

| Medida | Resultado |
|---|---|
| Tests | 84 (todos pasan) |
| Cobertura de instrucciones (JaCoCo) | 693 de 693, 100% |
| Cobertura de ramas (JaCoCo) | 44 de 44, 100% |
| Cobertura de lineas y metodos (JaCoCo) | 148 de 148 y 56 de 56, 100% |
| Checkstyle (estilo y V(G) maximo 10) | 0 violaciones |
| SpotBugs | 0 errores |
| Mutantes de PIT con los mutadores por defecto | 69 generados, 67 muertos y 2 sobrevivieron |
| Mutantes de PIT con el grupo STRONGER, despues de agregar un test | 90 generados, 90 muertos, 100% |

Los 84 tests se reparten asi:

1. 28 tests de camino, uno por cada V(G) de cada metodo.
2. 18 tests de limites y casos extra de esos mismos metodos.
3. 38 tests de apoyo: tipos, repositorio SQLite, persistencia al reiniciar, API completa, errores inesperados y configuracion.

La cobertura al 100% es una compuerta: `./mvnw verify` falla si baja de ese valor en instrucciones, ramas, lineas o metodos.

## 4. Mutaciones

PIT cambia el codigo a proposito y corre los tests. Si ninguno falla, el mutante sobrevive y falta un test.

Los tests de limite de las reglas (edad 0, 13, 14 y 120, las 8:00, las 24 horas exactas, la cita cancelada que no corta la revision) se escribieron desde el inicio porque la version Node ya habia mostrado con Stryker que esos huecos existian. Por eso aqui no se repitio el descubrimiento: la primera corrida solo dejo 2 sobrevivientes.

Los 2 sobrevivientes estaban en `ConfiguracionReloj`: PIT cambiaba el valor que devuelven `zonaHoraria` y `reloj` por `null` y ningun test fallaba. La causa probable es que Spring reutiliza el mismo contexto entre las pruebas y lo crea el primer test que corre, asi que PIT no asocia los demas tests a esas lineas. Ademas, las pruebas de la API reemplazan el reloj por uno fijo. No se investigo mas a fondo. Se arreglo con `ConfiguracionRelojTest`, que comprueba que la zona se lee de `application.properties` y llega al reloj real.

Despues se paso al grupo de mutadores STRONGER, que agrega mas cambios (por ejemplo, quitar condiciones). Resultado: 90 de 90 mutantes muertos.

Ademas, SpotBugs encontro un fallo real antes de las mutaciones: `GeneratedKeyHolder.getKey()` puede devolver `null` y el codigo lo usaba sin revisar. Se corrigio con `Objects.requireNonNull` y un mensaje claro.

## 5. Cuestionario de auditoria

1. Conozco V(G) antes de leer los tests? Si. Se calculo a mano antes de escribir los tests y coincide con Checkstyle.
2. El 100% incluye aserciones fuertes? Si. Los tests comparan valores, estados y mensajes exactos. PIT lo comprueba: 90 de 90 mutantes muertos.
3. Estan evaluadas todas las condiciones logicas? Parcial. JaCoCo mide ramas (44 de 44), no MC/DC. Las condiciones con `||` y `&&` se cubrieron con un test por condicion, pero la herramienta no verifica la independencia de cada una.
4. Forcé fallos para ver si los tests los detectan? Si, con PIT. Con los mutadores por defecto sobrevivieron 2, que se corrigieron, y con STRONGER se detectan los 90.
5. La complejidad es tan alta que conviene refactorizar? No. El maximo es 6.

## 6. Limites de este trabajo

1. No se mide MC/DC completo.
2. PIT reporta 140 de 144 lineas cubiertas con su propio medidor, mientras que JaCoCo mide 148 de 148. Ningun mutante quedo sin cobertura.
3. La clase `CitasApplication` (el `main`) queda fuera de JaCoCo y de PIT porque solo arranca Spring.
4. No se valida que la cita termine antes del cierre ni que cruce la hora de almuerzo, solo la hora de inicio.
5. Las citas duran siempre 30 minutos.
6. No hay pruebas de concurrencia. SQLite escribe de a uno y la base usa una sola conexion.

## 7. Como repetir las mediciones

```bash
./mvnw verify
```

```bash
./mvnw checkstyle:check@complejidad
```

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```
