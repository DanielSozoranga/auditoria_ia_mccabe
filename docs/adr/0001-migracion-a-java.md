# ADR 0001: Migrar el sistema de citas de Node.js a Java

Estado: aceptada
Fecha: 2026-10-19

## Contexto

El sistema de citas existia en Node.js (Express, SQLite, Jest, Stryker). Se pidio migrarlo completo a Java conservando la auditoria de caja blanca: V(G) de McCabe, cobertura de ramas y pruebas de mutacion.

## Decision

1. La migracion vive en la rama `feature/migracion-java`. `main` queda con la version Node como referencia.
2. La version Node se conserva en `legacy-node/` mientras se demuestra la paridad. Se puede borrar cuando se acepte la migracion.
3. El contrato de la API no cambia (mismas rutas, mismos codigos de estado, mismo formato de error), asi el frontend con Alpine.js funciona sin modificarse.
4. Se usa Java 21 LTS con Spring Boot 3.5, Maven con Wrapper y SQLite.

## Alternativas consideradas

| Alternativa | Por que no |
|---|---|
| Spring Boot 4.1 | Es la linea mas nueva, pero cambia paquetes y anotaciones de pruebas. Se deja como mejora futura. |
| Quarkus o Micronaut | Rinden bien, pero la mayoria de equipos y ofertas piden Spring. Para auditar la logica no aportan. |
| Gradle | Maven tiene plugins maduros para JaCoCo, PIT, Checkstyle y SpotBugs con configuracion declarativa. |
| JPA e Hibernate | Para una tabla y 5 consultas es mas codigo y mas magia. Spring JdbcClient deja el SQL a la vista. |

## Decisiones de diseno

1. **Capas.** `dominio` (reglas puras y el puerto del repositorio), `aplicacion` (servicio), `infraestructura` (SQLite) y `web` (API). El dominio no conoce Spring ni la base.
2. **Reloj inyectado.** El servicio recibe un `Clock`. Las pruebas lo fijan y no dependen de la hora real.
3. **Zona horaria explicita.** Las reglas de horario se leen en la hora del consultorio (`citas.zona-horaria`), no en la del servidor. Esto corrige una limitacion de la version Node.
4. **Dinero como `BigDecimal`.** La multa se guarda como texto en la base para no perder precision.
5. **Errores con un solo formato.** Todo error responde `{"error": "..."}` y el detalle de los fallos inesperados va solo al registro.

## Consecuencias

1. Hay que mantener las cifras de la auditoria al migrar: tests por camino, cobertura de ramas al 100% y 0 mutantes vivos.
2. Las diferencias de comportamiento con Node son pocas y estan listadas en `docs/paridad.md`.
3. Java necesita JDK 21 y Maven instalados. El wrapper (`./mvnw`) descarga la version exacta de Maven.
