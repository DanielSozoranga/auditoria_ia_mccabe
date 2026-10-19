from dataclasses import dataclass
from datetime import datetime, timedelta

EDAD_MAXIMA = 120
EDAD_LIMITE_PEDIATRICA = 14
HORA_APERTURA = 8
HORA_CIERRE = 17
HORA_ALMUERZO = 12
HORAS_LIMITE_CANCELACION = 24
PENALIZACION = 15.0


class ReglaNegocioError(Exception):
    # error que se lanza cuando se rompe una regla del negocio
    pass


@dataclass
class Cita:
    # datos de una cita guardada en la base
    id: int
    paciente: str
    tipo: str
    inicio: datetime
    fin: datetime
    estado: str
    penalizacion: float


def es_pediatrico(edad):
    # dice si el paciente es menor de 14 anios y rechaza edades imposibles
    if edad < 0 or edad > EDAD_MAXIMA:
        raise ReglaNegocioError("Edad fuera de rango")
    return edad < EDAD_LIMITE_PEDIATRICA


def validar_tipo_consulta(edad, tipo):
    # los menores van a pediatria y los adultos a medicina general
    esperado = "pediatrica" if es_pediatrico(edad) else "general"
    if tipo != esperado:
        raise ReglaNegocioError("El tipo de consulta no corresponde a la edad")


def validar_horario(inicio):
    # bloquea fines de semana, fuera de horario y la hora de almuerzo
    if inicio.weekday() >= 5:
        raise ReglaNegocioError("No se atiende en fin de semana")
    if inicio.hour < HORA_APERTURA or inicio.hour >= HORA_CIERRE:
        raise ReglaNegocioError("Fuera del horario de atencion")
    if inicio.hour == HORA_ALMUERZO:
        raise ReglaNegocioError("Hora de almuerzo no disponible")


def hay_choque(citas, inicio, fin):
    # revisa si el nuevo horario se cruza con alguna cita activa
    for cita in citas:
        if cita.estado == "cancelada":
            continue
        if cita.inicio < fin and inicio < cita.fin:
            return True
    return False


def calcular_penalizacion(inicio, ahora, es_emergencia):
    # cobra multa si cancelan con menos de 24h y no es emergencia
    tardia = inicio - ahora < timedelta(hours=HORAS_LIMITE_CANCELACION)
    if tardia and not es_emergencia:
        return PENALIZACION
    return 0.0
