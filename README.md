# Auditoria de IA con McCabe y cobertura (version Java)

Sistema de citas de un medico familiar hecho con Java 21, Spring Boot, SQLite y Alpine.js. El objetivo es auditar el codigo con caja blanca: calcular la complejidad ciclomatica V(G) de cada funcion, escribir un test por cada camino independiente y comprobar con cobertura de ramas y pruebas de mutacion que los tests si detectan fallos.

Esta rama (`feature/migracion-java`) es la migracion de la version Node.js, que sigue en `main` y en la carpeta `legacy-node/`. La decision esta explicada en [docs/adr/0001-migracion-a-java.md](docs/adr/0001-migracion-a-java.md) y las diferencias entre las dos versiones en [docs/paridad.md](docs/paridad.md).

## Reglas de negocio

1. Edad: menores de 14 anios van a pediatria, el resto a medicina general.
2. Horario: no se atiende en fin de semana, antes de las 8:00, desde las 17:00 ni a las 12:00 (almuerzo).
3. Choque de citas: no se puede reservar sobre una cita activa. Las canceladas no cuentan.
4. Penalizacion: cancelar con menos de 24 horas cobra 15, salvo que sea emergencia.

Las horas se leen en la zona horaria del consultorio (`citas.zona-horaria`, por defecto `America/Guayaquil`), no en la del servidor.

## Estructura

| Carpeta o archivo | Para que sirve |
|---|---|
| `src/main/java/.../dominio` | Las reglas como funciones puras (`Reglas`), los tipos y el puerto del repositorio |
| `src/main/java/.../aplicacion` | `ServicioCitas`: valida las reglas y luego guarda |
| `src/main/java/.../infraestructura` | `CitaRepositorioJdbc`: SQLite con Spring JdbcClient |
| `src/main/java/.../web` | API REST, validacion y manejo de errores |
| `src/main/java/.../configuracion` | Zona horaria y reloj |
| `src/main/resources/static` | Frontend con Alpine.js |
| `src/test/java` | Tests con JUnit 5, AssertJ y MockMvc |
| `config/` | Reglas de Checkstyle (limite de V(G) en 10) y exclusiones de SpotBugs |
| `docs/` | Auditoria, diagrama de flujo, prompts, paridad y decision de migrar |
| `legacy-node/` | La version anterior en Node.js, para comparar |

## Preparar el entorno

Se necesita un JDK 21 (por ejemplo Temurin). Maven no hace falta instalarlo: el Wrapper (`./mvnw`) descarga la version exacta, y el proyecto exige Java 21 o mayor.

```bash
java -version
```

Debe decir 21. El proyecto se probo con Temurin 21.0.12 y Maven 3.9.16.

## Comandos

Abrir la app en http://localhost:3000:

```bash
./mvnw spring-boot:run
```

Todo el control de calidad (estilo, tests, cobertura al 100% y SpotBugs):

```bash
./mvnw verify
```

V(G) de cada metodo con alguna decision:

```bash
./mvnw checkstyle:check@complejidad
```

Pruebas de mutacion con PIT (tardan unos minutos, el reporte queda en `target/pit-reports`):

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

El reporte de cobertura queda en `target/site/jacoco/index.html`.

## Notas

1. Alpine.js 3.17.4 va incluido en `src/main/resources/static/vendor/alpine.js` para que la app no dependa de internet.
2. La base se guarda en el archivo `citas.db` de la carpeta donde se arranca el servidor.
3. En GitHub Actions, `.github/workflows/ci.yml` corre la verificacion y las mutaciones en cada push.
