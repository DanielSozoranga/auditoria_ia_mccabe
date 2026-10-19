const db = require('../src/db');
const { PENALIZACION, ReglaNegocioError } = require('../src/reglas');
const { agendarCita, cancelarCita } = require('../src/servicio');

const LUNES_9AM = new Date(2026, 9, 19, 9, 0);
const HORA = 60 * 60 * 1000;

let conexion;

// base en memoria nueva para cada test
beforeEach(() => {
  conexion = db.conectar();
});

afterEach(() => {
  conexion.close();
});

describe('agendarCita, V(G) = 2, dos caminos', () => {
  test('guarda la cita con fin a los 30 minutos', () => {
    const id = agendarCita(conexion, 'Ana', 30, 'general', LUNES_9AM);
    const cita = db.obtenerCita(conexion, id);
    expect(cita.paciente).toBe('Ana');
    expect(cita.estado).toBe('activa');
    expect(cita.inicio).toEqual(LUNES_9AM);
    expect(cita.fin).toEqual(new Date(2026, 9, 19, 9, 30));
  });

  test('horario ocupado falla y no guarda', () => {
    agendarCita(conexion, 'Ana', 30, 'general', LUNES_9AM);
    expect(() => agendarCita(conexion, 'Luis', 40, 'general', LUNES_9AM)).toThrow(
      new ReglaNegocioError('Ese horario ya esta ocupado'),
    );
    expect(db.listarCitasDelDia(conexion, LUNES_9AM)).toHaveLength(1);
  });
});

describe('cancelarCita, V(G) = 3, tres caminos', () => {
  test('cita inexistente falla', () => {
    expect(() => cancelarCita(conexion, 999, LUNES_9AM, false)).toThrow(
      new ReglaNegocioError('La cita no existe'),
    );
  });

  test('cita ya cancelada falla', () => {
    const id = agendarCita(conexion, 'Ana', 30, 'general', LUNES_9AM);
    cancelarCita(conexion, id, new Date(LUNES_9AM.getTime() - 72 * HORA), false);
    expect(() =>
      cancelarCita(conexion, id, new Date(LUNES_9AM.getTime() - 48 * HORA), false),
    ).toThrow(new ReglaNegocioError('La cita ya estaba cancelada'));
  });

  test('cancelar tarde cobra y guarda la multa', () => {
    const id = agendarCita(conexion, 'Ana', 30, 'general', LUNES_9AM);
    const multa = cancelarCita(conexion, id, new Date(LUNES_9AM.getTime() - 2 * HORA), false);
    const cita = db.obtenerCita(conexion, id);
    expect(multa).toBe(PENALIZACION);
    expect(cita.estado).toBe('cancelada');
    expect(cita.penalizacion).toBe(PENALIZACION);
  });
});

// pruebas agregadas despues de revisar los mutantes que sobrevivieron
describe('agendarCita aplica las reglas antes de guardar', () => {
  test('rechaza un tipo de consulta que no corresponde a la edad', () => {
    expect(() => agendarCita(conexion, 'Ana', 5, 'general', LUNES_9AM)).toThrow(
      new ReglaNegocioError('El tipo de consulta no corresponde a la edad'),
    );
    expect(db.listarCitas(conexion)).toHaveLength(0);
  });

  test('rechaza un horario en fin de semana', () => {
    const domingo = new Date(2026, 9, 18, 9, 0);
    expect(() => agendarCita(conexion, 'Ana', 30, 'general', domingo)).toThrow(
      new ReglaNegocioError('No se atiende en fin de semana'),
    );
    expect(db.listarCitas(conexion)).toHaveLength(0);
  });
});
