from datetime import datetime, timedelta

import pytest

from citas import db
from citas.reglas import PENALIZACION, ReglaNegocioError
from citas.servicio import agendar_cita, cancelar_cita

LUNES_9AM = datetime(2026, 10, 19, 9, 0)


@pytest.fixture
def conn():
    # base en memoria nueva para cada test
    conexion = db.conectar()
    yield conexion
    conexion.close()


# agendar_cita: V(G) = 2, dos caminos
def test_agendar_guarda_la_cita_con_fin_de_30_minutos(conn):
    cita_id = agendar_cita(conn, "Ana", 30, "general", LUNES_9AM)
    cita = db.obtener_cita(conn, cita_id)
    assert cita.paciente == "Ana"
    assert cita.estado == "activa"
    assert cita.inicio == LUNES_9AM
    assert cita.fin == LUNES_9AM + timedelta(minutes=30)


def test_agendar_horario_ocupado_falla_y_no_guarda(conn):
    agendar_cita(conn, "Ana", 30, "general", LUNES_9AM)
    with pytest.raises(ReglaNegocioError, match="ocupado"):
        agendar_cita(conn, "Luis", 40, "general", LUNES_9AM)
    assert len(db.listar_citas_del_dia(conn, LUNES_9AM.date())) == 1


# cancelar_cita: V(G) = 3, tres caminos
def test_cancelar_cita_inexistente_falla(conn):
    with pytest.raises(ReglaNegocioError, match="no existe"):
        cancelar_cita(conn, 999, LUNES_9AM)


def test_cancelar_cita_ya_cancelada_falla(conn):
    cita_id = agendar_cita(conn, "Ana", 30, "general", LUNES_9AM)
    cancelar_cita(conn, cita_id, LUNES_9AM - timedelta(days=3))
    with pytest.raises(ReglaNegocioError, match="ya estaba cancelada"):
        cancelar_cita(conn, cita_id, LUNES_9AM - timedelta(days=2))


def test_cancelar_tarde_cobra_y_guarda_la_multa(conn):
    cita_id = agendar_cita(conn, "Ana", 30, "general", LUNES_9AM)
    multa = cancelar_cita(conn, cita_id, LUNES_9AM - timedelta(hours=2))
    cita = db.obtener_cita(conn, cita_id)
    assert multa == PENALIZACION
    assert cita.estado == "cancelada"
    assert cita.penalizacion == PENALIZACION
