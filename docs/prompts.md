# Re-prompting del auditor

La idea del taller: el mismo pedido a una IA da tests muy distintos segun como se escriba el prompt. Aqui se compara con la funcion `hayChoque`, que tiene V(G) = 5.

## Prompt ingenuo (desarrollador copiador)

```text
Escribe tests en JUnit para la funcion hayChoque.
```

Riesgos de este prompt:

1. La IA decide sola cuantos tests hacer y no sabe cuantos caminos hay.
2. Suele recorrer todas las lineas con pocos casos, por ejemplo una lista vacia y un choque.
3. Puede dejar tests sin aserciones o con aserciones debiles.

Ejemplo de test zombi (ilustrativo, no esta en el proyecto):

```java
@Test
void hayChoque() {
    // ejecuta la funcion pero no revisa el resultado
    Reglas.hayChoque(List.of(cita), inicio, fin);
}
```

Este test sube la cobertura y pasa en verde aunque la funcion devuelva siempre lo contrario.

## Prompt del auditor (ingeniero de QA)

```text
Actua como Ingeniero de QA. Calcule que la complejidad ciclomatica V(G)
de hayChoque es 5. Genera exactamente 5 tests unitarios en JUnit 5 con
AssertJ que cubran los 5 caminos independientes:
1. lista vacia
2. cita cancelada que se ignora
3. cita activa que empieza despues del nuevo horario
4. cita activa que termina antes del nuevo horario
5. cita activa que se cruza con el nuevo horario
Usa cobertura de ramas e incluye aserciones estrictas sobre el valor
devuelto (true o false), sin tests sin aserciones.
```

Por que funciona mejor:

1. Da el numero exacto de tests, asi se puede contar y verificar.
2. Lista los caminos, asi no quedan zonas ciegas.
3. Exige aserciones, asi el test falla si la funcion se rompe.

## Plantilla para cualquier funcion

```text
Actua como Ingeniero de QA. Calcule que V(G) de <metodo> es <N>.
Genera exactamente <N> tests en JUnit 5 con AssertJ, uno por cada camino
independiente: <lista de caminos>.
Usa cobertura de ramas y aserciones estrictas sobre el valor devuelto
o el mensaje exacto de la excepcion.
```

## Como verificar lo que entrega la IA

1. Contar los tests: tienen que ser V(G), calculado antes de leerlos.
2. Correr `./mvnw verify` y revisar que la cobertura de ramas sea 100%.
3. Leer las aserciones: que comparen valores o mensajes exactos.
4. Correr PIT y revisar que no sobreviva ningun mutante.
5. Si V(G) pasa de 10, refactorizar (Checkstyle lo hace fallar).

## Lo que se comprobo en este proyecto

Ver la seccion de mutaciones de `docs/auditoria.md`: la cobertura mide que el codigo se ejecuta, no que el test lo verifique, y las pruebas de mutacion son las que muestran la diferencia.

Nota: en este proyecto no se guardo la respuesta de una IA al prompt ingenuo, asi que los riesgos de arriba son los que describe la clase, no una medicion propia.
