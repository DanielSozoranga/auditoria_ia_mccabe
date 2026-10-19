from datetime import timedelta

from citas import db
from citas.reglas import (
    ReglaNegocioError,
    calcular_penalizacion,
    hay_choque,
    validar_horario,
    validar_tipo_consulta,
)

DURACION_MINUTOS = 30


def agendar_cita(conn, paciente, edad, tipo, inicio):
    # aplica las reglas de edad, horario y choque y guarda la cita
    validar_tipo_consulta(edad, tipo)
    validar_horario(inicio)
    fin = inicio + timedelta(minutes=DURACION_MINUTOS)
    if hay_choque(db.listar_citas_del_dia(conn, inicio.date()), inicio, fin):
        raise ReglaNegocioError("Ese horario ya esta ocupado")
    return db.insertar_cita(conn, paciente, tipo, inicio, fin)


def cancelar_cita(conn, cita_id, ahora, es_emergencia=False):
    # cancela la cita y devuelve la penalizacion cobrada
    cita = db.obtener_cita(conn, cita_id)
    if cita is None:
        raise ReglaNegocioError("La cita no existe")
    if cita.estado == "cancelada":
        raise ReglaNegocioError("La cita ya estaba cancelada")
    multa = calcular_penalizacion(cita.inicio, ahora, es_emergencia)
    db.marcar_cancelada(conn, cita_id, multa)
    return multa
