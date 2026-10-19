-- tabla de citas, la multa se guarda como texto para no perder precision
CREATE TABLE IF NOT EXISTS citas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    paciente TEXT NOT NULL,
    tipo TEXT NOT NULL,
    inicio TEXT NOT NULL,
    fin TEXT NOT NULL,
    estado TEXT NOT NULL DEFAULT 'activa',
    penalizacion TEXT NOT NULL DEFAULT '0'
);
