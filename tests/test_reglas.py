from datetime import datetime, timedelta

import pytest

from citas.reglas import (
    PENALIZACION,
    Cita,
    ReglaNegocioError,
    calcular_penalizacion,
    es_pediatrico,
    hay_choque,
    validar_horario,
    validar_tipo_consulta,
)

# lunes 19 de octubre de 2026, dia habil para las pruebas
LUNES_9AM = datetime(2026, 10, 19, 9, 0)


def hacer_cita(inicio, fin, estado="activa"):
    # arma una cita de prueba con los datos minimos
    return Cita(1, "Ana", "general", inicio, fin, estado, 0.0)


# es_pediatrico: V(G) = 3, tres caminos
def test_es_pediatrico_edad_negativa_falla():
    with pytest.raises(ReglaNegocioError, match=r"^Edad fuera de rango$"):
        es_pediatrico(-1)


def test_es_pediatrico_edad_mayor_al_maximo_falla():
    with pytest.raises(ReglaNegocioError, match=r"^Edad fuera de rango$"):
        es_pediatrico(121)


def test_es_pediatrico_edad_valida_responde():
    assert es_pediatrico(5) is True


# validar_tipo_consulta: V(G) = 3, tres caminos
def test_tipo_consulta_menor_en_pediatria_pasa():
    assert validar_tipo_consulta(5, "pediatrica") is None


def test_tipo_consulta_adulto_en_general_pasa():
    assert validar_tipo_consulta(30, "general") is None


def test_tipo_consulta_menor_en_general_falla():
    mensaje = r"^El tipo de consulta no corresponde a la edad$"
    with pytest.raises(ReglaNegocioError, match=mensaje):
        validar_tipo_consulta(5, "general")


# validar_horario: V(G) = 5, cinco caminos
def test_horario_fin_de_semana_falla():
    sabado = LUNES_9AM + timedelta(days=5)
    with pytest.raises(ReglaNegocioError, match=r"^No se atiende en fin de semana$"):
        validar_horario(sabado)


def test_horario_antes_de_abrir_falla():
    with pytest.raises(ReglaNegocioError, match=r"^Fuera del horario de atencion$"):
        validar_horario(LUNES_9AM.replace(hour=7))


def test_horario_despues_de_cerrar_falla():
    with pytest.raises(ReglaNegocioError, match=r"^Fuera del horario de atencion$"):
        validar_horario(LUNES_9AM.replace(hour=17))


def test_horario_de_almuerzo_falla():
    with pytest.raises(ReglaNegocioError, match=r"^Hora de almuerzo no disponible$"):
        validar_horario(LUNES_9AM.replace(hour=12))


def test_horario_habil_pasa():
    assert validar_horario(LUNES_9AM) is None


# hay_choque: V(G) = 5, cinco caminos
FIN_9AM = LUNES_9AM + timedelta(minutes=30)


def test_choque_sin_citas_no_hay():
    assert hay_choque([], LUNES_9AM, FIN_9AM) is False


def test_choque_ignora_citas_canceladas():
    cancelada = hacer_cita(LUNES_9AM, FIN_9AM, estado="cancelada")
    assert hay_choque([cancelada], LUNES_9AM, FIN_9AM) is False


def test_choque_cita_que_empieza_despues_no_choca():
    despues = hacer_cita(FIN_9AM, FIN_9AM + timedelta(minutes=30))
    assert hay_choque([despues], LUNES_9AM, FIN_9AM) is False


def test_choque_cita_que_termina_antes_no_choca():
    antes = hacer_cita(LUNES_9AM - timedelta(minutes=30), LUNES_9AM)
    assert hay_choque([antes], LUNES_9AM, FIN_9AM) is False


def test_choque_cita_activa_cruzada_si_choca():
    cruzada = hacer_cita(LUNES_9AM, FIN_9AM)
    assert hay_choque([cruzada], LUNES_9AM, FIN_9AM) is True


# calcular_penalizacion: V(G) = 3, tres caminos
CITA_INICIO = datetime(2026, 10, 20, 9, 0)


def test_penalizacion_cancelacion_con_tiempo_es_cero():
    ahora = CITA_INICIO - timedelta(hours=48)
    assert calcular_penalizacion(CITA_INICIO, ahora, False) == 0.0


def test_penalizacion_tardia_sin_emergencia_cobra():
    ahora = CITA_INICIO - timedelta(hours=2)
    assert calcular_penalizacion(CITA_INICIO, ahora, False) == PENALIZACION


def test_penalizacion_tardia_con_emergencia_es_cero():
    ahora = CITA_INICIO - timedelta(hours=2)
    assert calcular_penalizacion(CITA_INICIO, ahora, True) == 0.0


# pruebas de limites agregadas despues de revisar los mutantes que sobrevivieron
@pytest.mark.parametrize("edad, esperado", [(0, True), (120, False)])
def test_es_pediatrico_acepta_los_extremos_validos(edad, esperado):
    assert es_pediatrico(edad) is esperado


@pytest.mark.parametrize("edad, esperado", [(13, True), (14, False)])
def test_es_pediatrico_corte_en_14_anios(edad, esperado):
    assert es_pediatrico(edad) is esperado


def test_horario_abre_a_las_8_en_punto():
    assert validar_horario(LUNES_9AM.replace(hour=8)) is None


def test_choque_cita_cancelada_no_corta_la_revision():
    cancelada = hacer_cita(LUNES_9AM, FIN_9AM, estado="cancelada")
    activa = hacer_cita(LUNES_9AM, FIN_9AM)
    assert hay_choque([cancelada, activa], LUNES_9AM, FIN_9AM) is True


def test_penalizacion_exactamente_24_horas_no_cobra():
    ahora = CITA_INICIO - timedelta(hours=24)
    assert calcular_penalizacion(CITA_INICIO, ahora, False) == 0.0
