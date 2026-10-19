# Auditoria de IA con McCabe y cobertura

Sistema de citas de un medico familiar, hecho en Python con SQLite. El objetivo es auditar el codigo con caja blanca: calcular la complejidad ciclomatica V(G) de cada funcion, escribir un test por cada camino independiente y comprobar con cobertura de ramas y mutaciones que los tests si detectan fallos.

## Reglas de negocio

1. Edad: menores de 14 anios van a pediatria, el resto a medicina general.
2. Horario: no se atiende en fin de semana, antes de las 8:00, desde las 17:00 ni a las 12:00 (almuerzo).
3. Choque de citas: no se puede reservar sobre una cita activa. Las canceladas no cuentan.
4. Penalizacion: cancelar con menos de 24 horas cobra 15.0, salvo que sea emergencia.

## Estructura

| Archivo | Para que sirve |
|---|---|
| citas/reglas.py | Las 4 reglas como funciones puras |
| citas/db.py | Acceso a SQLite |
| citas/servicio.py | Une las reglas con la base (agendar y cancelar) |
| tests/ | Tests con pytest |
| docs/auditoria.md | Tabla de V(G), resultados y cuestionario |

## Como correrlo

Todo se instala dentro de un entorno virtual en el propio proyecto (WSL Ubuntu).

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

Tests con cobertura de ramas:

```bash
.venv/bin/pytest
```

Complejidad ciclomatica:

```bash
.venv/bin/radon cc citas -s -a
```

Pruebas de mutacion:

```bash
.venv/bin/mutmut run
.venv/bin/mutmut results
```
