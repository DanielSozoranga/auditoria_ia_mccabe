# Auditoria de IA con McCabe y cobertura (version Node.js, anterior)

Esta es la version original, conservada para comparar con la migracion a Java. Todos los comandos de este documento se corren desde esta carpeta (`legacy-node`). La version vigente esta en la raiz del repositorio.

Sistema de citas de un medico familiar hecho con Node.js (Express), SQLite y Alpine.js. El objetivo es auditar el codigo con caja blanca: calcular la complejidad ciclomatica V(G) de cada funcion, escribir un test por cada camino independiente y comprobar con cobertura de ramas y pruebas de mutacion que los tests si detectan fallos.

## Reglas de negocio

1. Edad: menores de 14 anios van a pediatria, el resto a medicina general.
2. Horario: no se atiende en fin de semana, antes de las 8:00, desde las 17:00 ni a las 12:00 (almuerzo).
3. Choque de citas: no se puede reservar sobre una cita activa. Las canceladas no cuentan.
4. Penalizacion: cancelar con menos de 24 horas cobra 15, salvo que sea emergencia.

## Estructura

| Archivo | Para que sirve |
|---|---|
| src/reglas.js | Las 4 reglas como funciones puras |
| src/db.js | Acceso a SQLite con better-sqlite3 |
| src/servicio.js | Une las reglas con la base (agendar y cancelar) |
| src/app.js | API con Express |
| src/server.js | Arranca el servidor |
| public/index.html | Frontend con Alpine.js |
| tests/ | Tests con Jest y Supertest |
| eslint.config.js | Limite de V(G) en 10 |
| eslint.complejidad.config.js | Imprime el V(G) de cada funcion |
| stryker.config.json | Pruebas de mutacion |
| docs/auditoria.md | Tabla de V(G), resultados y cuestionario |
| docs/cfg_penalizacion.md | Diagrama del grafo de flujo de la penalizacion |
| docs/prompts.md | Prompt ingenuo contra prompt del auditor |

## Preparar el entorno

La version de Node queda fijada en `.nvmrc` y las librerias se instalan solo dentro del proyecto (`node_modules`).

```bash
nvm use
```

```bash
npm install
```

## Comandos

Abrir la app en http://localhost:3000:

```bash
npm start
```

Tests con cobertura de ramas (falla si baja de 100%):

```bash
npm test
```

Cobertura de ramas con c8:

```bash
npm run cobertura:c8
```

V(G) de cada funcion:

```bash
npm run complejidad
```

Revisar que ninguna funcion pase de V(G) 10:

```bash
npm run lint
```

Pruebas de mutacion con Stryker (tarda unos 2 minutos):

```bash
npm run mutaciones
```
