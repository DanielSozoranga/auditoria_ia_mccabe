const request = require('supertest');
const db = require('../src/db');
const { PENALIZACION } = require('../src/reglas');
const { crearApp, leerDatosCita, EntradaInvalidaError } = require('../src/app');

const LUNES_9AM = new Date(2026, 9, 19, 9, 0);
const HORA = 60 * 60 * 1000;

let conexion;
let ahora;
let app;

// app nueva con base en memoria y reloj controlado
beforeEach(() => {
  conexion = db.conectar();
  ahora = new Date(LUNES_9AM.getTime() - 2 * HORA);
  app = crearApp(conexion, () => ahora);
});

afterEach(() => {
  if (conexion.open) {
    conexion.close();
  }
});

// datos validos para agendar
function citaValida() {
  return { paciente: 'Ana', edad: 30, tipo: 'general', inicio: LUNES_9AM.toISOString() };
}

describe('leerDatosCita, V(G) = 5, cinco caminos', () => {
  test('sin paciente falla', () => {
    expect(() => leerDatosCita({ ...citaValida(), paciente: '' })).toThrow(EntradaInvalidaError);
  });

  test('sin tipo falla', () => {
    expect(() => leerDatosCita({ ...citaValida(), tipo: undefined })).toThrow(
      EntradaInvalidaError,
    );
  });

  test('edad que no es numero falla', () => {
    expect(() => leerDatosCita({ ...citaValida(), edad: '30' })).toThrow(EntradaInvalidaError);
  });

  test('fecha invalida falla', () => {
    expect(() => leerDatosCita({ ...citaValida(), inicio: 'manana' })).toThrow(
      new EntradaInvalidaError('Faltan datos o la fecha es invalida'),
    );
  });

  test('datos completos devuelven la cita armada', () => {
    expect(leerDatosCita(citaValida())).toEqual({
      paciente: 'Ana',
      edad: 30,
      tipo: 'general',
      inicio: LUNES_9AM,
    });
  });
});

describe('API de citas', () => {
  test('POST agenda una cita y GET la lista', async () => {
    const creada = await request(app).post('/api/citas').send(citaValida());
    expect(creada.status).toBe(201);
    expect(creada.body).toEqual({ id: 1 });

    const lista = await request(app).get('/api/citas');
    expect(lista.status).toBe(200);
    expect(lista.body).toHaveLength(1);
    expect(lista.body[0]).toMatchObject({ paciente: 'Ana', estado: 'activa' });
  });

  test('POST sin cuerpo responde 400', async () => {
    const respuesta = await request(app).post('/api/citas');
    expect(respuesta.status).toBe(400);
    expect(respuesta.body).toEqual({ error: 'Faltan datos o la fecha es invalida' });
  });

  test('POST que rompe una regla responde 422 con el mensaje', async () => {
    await request(app).post('/api/citas').send(citaValida());
    const respuesta = await request(app).post('/api/citas').send(citaValida());
    expect(respuesta.status).toBe(422);
    expect(respuesta.body).toEqual({ error: 'Ese horario ya esta ocupado' });
  });

  test('POST con JSON roto responde 400', async () => {
    const respuesta = await request(app)
      .post('/api/citas')
      .set('Content-Type', 'application/json')
      .send('{roto');
    expect(respuesta.status).toBe(400);
    expect(respuesta.body).toEqual({ error: 'Peticion invalida' });
  });

  test('cancelar tarde sin emergencia cobra la multa', async () => {
    await request(app).post('/api/citas').send(citaValida());
    const respuesta = await request(app).post('/api/citas/1/cancelar');
    expect(respuesta.status).toBe(200);
    expect(respuesta.body).toEqual({ penalizacion: PENALIZACION });
  });

  test('cancelar tarde con emergencia no cobra', async () => {
    await request(app).post('/api/citas').send(citaValida());
    const respuesta = await request(app).post('/api/citas/1/cancelar').send({ esEmergencia: true });
    expect(respuesta.status).toBe(200);
    expect(respuesta.body).toEqual({ penalizacion: 0 });
  });

  test('cancelar una cita que no existe responde 422', async () => {
    const respuesta = await request(app).post('/api/citas/99/cancelar');
    expect(respuesta.status).toBe(422);
    expect(respuesta.body).toEqual({ error: 'La cita no existe' });
  });

  test('un fallo de la base responde 500', async () => {
    conexion.close();
    const respuesta = await request(app).get('/api/citas');
    expect(respuesta.status).toBe(500);
    expect(respuesta.body).toEqual({ error: 'Error del servidor' });
  });
});

describe('archivos del frontend', () => {
  test('la pagina principal se sirve', async () => {
    const respuesta = await request(app).get('/');
    expect(respuesta.status).toBe(200);
    expect(respuesta.text).toContain('Citas del medico familiar');
  });

  test('Alpine.js se sirve desde el proyecto', async () => {
    const respuesta = await request(app).get('/vendor/alpine.js');
    expect(respuesta.status).toBe(200);
    expect(respuesta.headers['content-type']).toContain('javascript');
  });
});
