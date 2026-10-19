import sqlite3
from datetime import datetime, timedelta

from citas.reglas import Cita


def conectar(ruta=":memory:"):
    # abre la conexion a SQLite y crea las tablas si faltan
    conn = sqlite3.connect(ruta)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS citas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            paciente TEXT NOT NULL,
            tipo TEXT NOT NULL,
            inicio TEXT NOT NULL,
            fin TEXT NOT NULL,
            estado TEXT NOT NULL DEFAULT 'activa',
            penalizacion REAL NOT NULL DEFAULT 0
        )
        """
    )
    return conn


def _a_cita(fila):
    # convierte una fila de la base en un objeto Cita
    return Cita(
        id=fila[0],
        paciente=fila[1],
        tipo=fila[2],
        inicio=datetime.fromisoformat(fila[3]),
        fin=datetime.fromisoformat(fila[4]),
        estado=fila[5],
        penalizacion=fila[6],
    )


def insertar_cita(conn, paciente, tipo, inicio, fin):
    # guarda una cita nueva y devuelve su id
    cursor = conn.execute(
        "INSERT INTO citas (paciente, tipo, inicio, fin) VALUES (?, ?, ?, ?)",
        (paciente, tipo, inicio.isoformat(), fin.isoformat()),
    )
    conn.commit()
    return cursor.lastrowid


def obtener_cita(conn, cita_id):
    # busca una cita por id, devuelve None si no existe
    fila = conn.execute("SELECT * FROM citas WHERE id = ?", (cita_id,)).fetchone()
    return _a_cita(fila) if fila else None


def listar_citas_del_dia(conn, dia):
    # trae todas las citas que empiezan en el dia indicado
    desde = datetime.combine(dia, datetime.min.time())
    hasta = desde + timedelta(days=1)
    filas = conn.execute(
        "SELECT * FROM citas WHERE inicio >= ? AND inicio < ?",
        (desde.isoformat(), hasta.isoformat()),
    ).fetchall()
    return [_a_cita(fila) for fila in filas]


def marcar_cancelada(conn, cita_id, penalizacion):
    # cambia el estado a cancelada y guarda la multa
    conn.execute(
        "UPDATE citas SET estado = 'cancelada', penalizacion = ? WHERE id = ?",
        (penalizacion, cita_id),
    )
    conn.commit()
