const EDAD_MAXIMA = 120;
const EDAD_LIMITE_PEDIATRICA = 14;
const HORA_APERTURA = 8;
const HORA_CIERRE = 17;
const HORA_ALMUERZO = 12;
const HORAS_LIMITE_CANCELACION = 24;
const PENALIZACION = 15;
const MS_POR_HORA = 60 * 60 * 1000;

// error que se lanza cuando se rompe una regla del negocio
class ReglaNegocioError extends Error {}

// dice si el paciente es menor de 14 anios y rechaza edades imposibles
function esPediatrico(edad) {
  if (!Number.isInteger(edad) || edad < 0 || edad > EDAD_MAXIMA) {
    throw new ReglaNegocioError('Edad fuera de rango');
  }
  return edad < EDAD_LIMITE_PEDIATRICA;
}

// los menores van a pediatria y los adultos a medicina general
function validarTipoConsulta(edad, tipo) {
  const esperado = esPediatrico(edad) ? 'pediatrica' : 'general';
  if (tipo !== esperado) {
    throw new ReglaNegocioError('El tipo de consulta no corresponde a la edad');
  }
}

// bloquea fines de semana, fuera de horario y la hora de almuerzo
function validarHorario(inicio) {
  const dia = inicio.getDay();
  const hora = inicio.getHours();
  if (dia === 0 || dia === 6) {
    throw new ReglaNegocioError('No se atiende en fin de semana');
  }
  if (hora < HORA_APERTURA || hora >= HORA_CIERRE) {
    throw new ReglaNegocioError('Fuera del horario de atencion');
  }
  if (hora === HORA_ALMUERZO) {
    throw new ReglaNegocioError('Hora de almuerzo no disponible');
  }
}

// revisa si el nuevo horario se cruza con alguna cita activa
function hayChoque(citas, inicio, fin) {
  for (const cita of citas) {
    if (cita.estado === 'cancelada') {
      continue;
    }
    if (cita.inicio < fin && inicio < cita.fin) {
      return true;
    }
  }
  return false;
}

// cobra multa si cancelan con menos de 24h y no es emergencia
function calcularPenalizacion(inicio, ahora, esEmergencia) {
  const tardia = inicio - ahora < HORAS_LIMITE_CANCELACION * MS_POR_HORA;
  if (tardia && !esEmergencia) {
    return PENALIZACION;
  }
  return 0;
}

module.exports = {
  PENALIZACION,
  ReglaNegocioError,
  esPediatrico,
  validarTipoConsulta,
  validarHorario,
  hayChoque,
  calcularPenalizacion,
};
