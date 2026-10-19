const db = require('./db');
const {
  ReglaNegocioError,
  calcularPenalizacion,
  hayChoque,
  validarHorario,
  validarTipoConsulta,
} = require('./reglas');

const DURACION_MINUTOS = 30;

// aplica las reglas de edad, horario y choque y guarda la cita
function agendarCita(conexion, paciente, edad, tipo, inicio) {
  validarTipoConsulta(edad, tipo);
  validarHorario(inicio);
  const fin = new Date(inicio.getTime() + DURACION_MINUTOS * 60 * 1000);
  if (hayChoque(db.listarCitasDelDia(conexion, inicio), inicio, fin)) {
    throw new ReglaNegocioError('Ese horario ya esta ocupado');
  }
  return db.insertarCita(conexion, paciente, tipo, inicio, fin);
}

// cancela la cita y devuelve la penalizacion cobrada
function cancelarCita(conexion, id, ahora, esEmergencia) {
  const cita = db.obtenerCita(conexion, id);
  if (cita === null) {
    throw new ReglaNegocioError('La cita no existe');
  }
  if (cita.estado === 'cancelada') {
    throw new ReglaNegocioError('La cita ya estaba cancelada');
  }
  const multa = calcularPenalizacion(cita.inicio, ahora, esEmergencia);
  db.marcarCancelada(conexion, id, multa);
  return multa;
}

module.exports = { agendarCita, cancelarCita };
