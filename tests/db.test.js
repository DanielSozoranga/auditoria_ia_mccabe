const db = require('../src/db');

const LUNES_9AM = new Date(2026, 9, 19, 9, 0);
const LUNES_930AM = new Date(2026, 9, 19, 9, 30);

describe('base de datos', () => {
  test('conectar acepta una ruta explicita', () => {
    const conexion = db.conectar(':memory:');
    expect(db.listarCitas(conexion)).toEqual([]);
    conexion.close();
  });

  test('obtenerCita devuelve null si no existe', () => {
    const conexion = db.conectar();
    expect(db.obtenerCita(conexion, 1)).toBeNull();
    conexion.close();
  });

  test('listarCitasDelDia solo trae las del dia pedido', () => {
    const conexion = db.conectar();
    db.insertarCita(conexion, 'Ana', 'general', LUNES_9AM, LUNES_930AM);
    const martes = new Date(2026, 9, 20, 9, 0);
    db.insertarCita(conexion, 'Luis', 'general', martes, new Date(2026, 9, 20, 9, 30));
    const citas = db.listarCitasDelDia(conexion, LUNES_9AM);
    expect(citas).toHaveLength(1);
    expect(citas[0].paciente).toBe('Ana');
    conexion.close();
  });

  test('listarCitas devuelve todas ordenadas por fecha', () => {
    const conexion = db.conectar();
    db.insertarCita(conexion, 'Luis', 'general', LUNES_930AM, new Date(2026, 9, 19, 10, 0));
    db.insertarCita(conexion, 'Ana', 'general', LUNES_9AM, LUNES_930AM);
    expect(db.listarCitas(conexion).map((c) => c.paciente)).toEqual(['Ana', 'Luis']);
    conexion.close();
  });
});
