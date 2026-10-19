const Database = require('better-sqlite3');

// abre la conexion a SQLite y crea la tabla si falta
function conectar(ruta = ':memory:') {
  const db = new Database(ruta);
  db.exec(`
    CREATE TABLE IF NOT EXISTS citas (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      paciente TEXT NOT NULL,
      tipo TEXT NOT NULL,
      inicio TEXT NOT NULL,
      fin TEXT NOT NULL,
      estado TEXT NOT NULL DEFAULT 'activa',
      penalizacion REAL NOT NULL DEFAULT 0
    )
  `);
  return db;
}

// convierte una fila de la base en una cita con fechas reales
function aCita(fila) {
  return { ...fila, inicio: new Date(fila.inicio), fin: new Date(fila.fin) };
}

// guarda una cita nueva y devuelve su id
function insertarCita(db, paciente, tipo, inicio, fin) {
  const resultado = db
    .prepare('INSERT INTO citas (paciente, tipo, inicio, fin) VALUES (?, ?, ?, ?)')
    .run(paciente, tipo, inicio.toISOString(), fin.toISOString());
  return Number(resultado.lastInsertRowid);
}

// busca una cita por id, devuelve null si no existe
function obtenerCita(db, id) {
  const fila = db.prepare('SELECT * FROM citas WHERE id = ?').get(id);
  return fila ? aCita(fila) : null;
}

// trae todas las citas que empiezan en el dia indicado
function listarCitasDelDia(db, dia) {
  const desde = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate());
  const hasta = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate() + 1);
  return db
    .prepare('SELECT * FROM citas WHERE inicio >= ? AND inicio < ?')
    .all(desde.toISOString(), hasta.toISOString())
    .map(aCita);
}

// trae todas las citas ordenadas por fecha
function listarCitas(db) {
  return db.prepare('SELECT * FROM citas ORDER BY inicio').all().map(aCita);
}

// cambia el estado a cancelada y guarda la multa
function marcarCancelada(db, id, penalizacion) {
  db.prepare("UPDATE citas SET estado = 'cancelada', penalizacion = ? WHERE id = ?").run(
    penalizacion,
    id,
  );
}

module.exports = {
  conectar,
  insertarCita,
  obtenerCita,
  listarCitasDelDia,
  listarCitas,
  marcarCancelada,
};
