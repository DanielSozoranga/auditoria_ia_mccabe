const path = require('path');
const express = require('express');
const db = require('./db');
const { ReglaNegocioError } = require('./reglas');
const { agendarCita, cancelarCita } = require('./servicio');

// error para datos mal enviados por el cliente
class EntradaInvalidaError extends Error {}

// revisa que lleguen todos los datos y arma la cita
function leerDatosCita(body) {
  const inicio = new Date(body.inicio);
  if (
    !body.paciente ||
    !body.tipo ||
    typeof body.edad !== 'number' ||
    Number.isNaN(inicio.getTime())
  ) {
    throw new EntradaInvalidaError('Faltan datos o la fecha es invalida');
  }
  return { paciente: String(body.paciente), edad: body.edad, tipo: body.tipo, inicio };
}

// crea la app de Express, recibe la base y una funcion que da la hora actual
function crearApp(conexion, reloj) {
  const app = express();
  app.use(express.json());

  // sirve la pagina y la libreria Alpine.js desde el propio proyecto
  app.use(express.static(path.join(__dirname, '..', 'public')));
  app.get('/vendor/alpine.js', (req, res) => {
    res.sendFile(require.resolve('alpinejs/dist/cdn.min.js'));
  });

  // lista todas las citas
  app.get('/api/citas', (req, res) => {
    res.json(db.listarCitas(conexion));
  });

  // agenda una cita nueva
  app.post('/api/citas', (req, res) => {
    const datos = leerDatosCita(req.body || {});
    const id = agendarCita(conexion, datos.paciente, datos.edad, datos.tipo, datos.inicio);
    res.status(201).json({ id });
  });

  // cancela una cita y responde cuanto se cobro
  app.post('/api/citas/:id/cancelar', (req, res) => {
    const esEmergencia = (req.body || {}).esEmergencia === true;
    const penalizacion = cancelarCita(conexion, Number(req.params.id), reloj(), esEmergencia);
    res.json({ penalizacion });
  });

  // convierte los errores en respuestas con el codigo correcto
  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    if (err instanceof EntradaInvalidaError) {
      return res.status(400).json({ error: err.message });
    }
    if (err instanceof ReglaNegocioError) {
      return res.status(422).json({ error: err.message });
    }
    const estado = err.status || 500;
    return res
      .status(estado)
      .json({ error: estado === 500 ? 'Error del servidor' : 'Peticion invalida' });
  });

  return app;
}

module.exports = { crearApp, leerDatosCita, EntradaInvalidaError };
