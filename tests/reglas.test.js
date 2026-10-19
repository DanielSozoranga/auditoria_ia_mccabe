const {
  PENALIZACION,
  ReglaNegocioError,
  calcularPenalizacion,
  esPediatrico,
  hayChoque,
  validarHorario,
  validarTipoConsulta,
} = require('../src/reglas');

// lunes 19 de octubre de 2026, dia habil para las pruebas
const LUNES_9AM = new Date(2026, 9, 19, 9, 0);
const DOMINGO_9AM = new Date(2026, 9, 18, 9, 0);
const SABADO_9AM = new Date(2026, 9, 24, 9, 0);
const MINUTO = 60 * 1000;
const HORA = 60 * MINUTO;

// comprueba el tipo de error y su mensaje exacto
function esperarError(funcion, mensaje) {
  expect(funcion).toThrow(ReglaNegocioError);
  expect(funcion).toThrow(new ReglaNegocioError(mensaje));
}

// arma una cita de prueba con los datos minimos
function hacerCita(inicio, fin, estado = 'activa') {
  return { id: 1, paciente: 'Ana', tipo: 'general', inicio, fin, estado, penalizacion: 0 };
}

describe('esPediatrico, V(G) = 4, cuatro caminos', () => {
  test('edad con decimales falla', () => {
    esperarError(() => esPediatrico(5.5), 'Edad fuera de rango');
  });

  test('edad negativa falla', () => {
    esperarError(() => esPediatrico(-1), 'Edad fuera de rango');
  });

  test('edad mayor al maximo falla', () => {
    esperarError(() => esPediatrico(121), 'Edad fuera de rango');
  });

  test('edad valida de un nino responde true', () => {
    expect(esPediatrico(5)).toBe(true);
  });
});

describe('validarTipoConsulta, V(G) = 3, tres caminos', () => {
  test('menor en pediatria pasa', () => {
    expect(validarTipoConsulta(5, 'pediatrica')).toBeUndefined();
  });

  test('adulto en general pasa', () => {
    expect(validarTipoConsulta(30, 'general')).toBeUndefined();
  });

  test('menor en general falla', () => {
    esperarError(
      () => validarTipoConsulta(5, 'general'),
      'El tipo de consulta no corresponde a la edad',
    );
  });
});

describe('validarHorario, V(G) = 6, seis caminos', () => {
  test('domingo falla', () => {
    esperarError(() => validarHorario(DOMINGO_9AM), 'No se atiende en fin de semana');
  });

  test('sabado falla', () => {
    esperarError(() => validarHorario(SABADO_9AM), 'No se atiende en fin de semana');
  });

  test('antes de abrir falla', () => {
    const inicio = new Date(2026, 9, 19, 7, 0);
    esperarError(() => validarHorario(inicio), 'Fuera del horario de atencion');
  });

  test('despues de cerrar falla', () => {
    const inicio = new Date(2026, 9, 19, 17, 0);
    esperarError(() => validarHorario(inicio), 'Fuera del horario de atencion');
  });

  test('hora de almuerzo falla', () => {
    const inicio = new Date(2026, 9, 19, 12, 0);
    esperarError(() => validarHorario(inicio), 'Hora de almuerzo no disponible');
  });

  test('dia y hora habiles pasan', () => {
    expect(validarHorario(LUNES_9AM)).toBeUndefined();
  });
});

describe('hayChoque, V(G) = 5, cinco caminos', () => {
  const FIN_9AM = new Date(LUNES_9AM.getTime() + 30 * MINUTO);

  test('sin citas no hay choque', () => {
    expect(hayChoque([], LUNES_9AM, FIN_9AM)).toBe(false);
  });

  test('ignora las citas canceladas', () => {
    const cancelada = hacerCita(LUNES_9AM, FIN_9AM, 'cancelada');
    expect(hayChoque([cancelada], LUNES_9AM, FIN_9AM)).toBe(false);
  });

  test('cita que empieza despues no choca', () => {
    const despues = hacerCita(FIN_9AM, new Date(FIN_9AM.getTime() + 30 * MINUTO));
    expect(hayChoque([despues], LUNES_9AM, FIN_9AM)).toBe(false);
  });

  test('cita que termina antes no choca', () => {
    const antes = hacerCita(new Date(LUNES_9AM.getTime() - 30 * MINUTO), LUNES_9AM);
    expect(hayChoque([antes], LUNES_9AM, FIN_9AM)).toBe(false);
  });

  test('cita activa cruzada si choca', () => {
    const cruzada = hacerCita(LUNES_9AM, FIN_9AM);
    expect(hayChoque([cruzada], LUNES_9AM, FIN_9AM)).toBe(true);
  });
});

describe('calcularPenalizacion, V(G) = 3, tres caminos', () => {
  const CITA_INICIO = new Date(2026, 9, 20, 9, 0);

  test('cancelar con tiempo no cobra', () => {
    const ahora = new Date(CITA_INICIO.getTime() - 48 * HORA);
    expect(calcularPenalizacion(CITA_INICIO, ahora, false)).toBe(0);
  });

  test('cancelar tarde sin emergencia cobra', () => {
    const ahora = new Date(CITA_INICIO.getTime() - 2 * HORA);
    expect(calcularPenalizacion(CITA_INICIO, ahora, false)).toBe(PENALIZACION);
  });

  test('cancelar tarde con emergencia no cobra', () => {
    const ahora = new Date(CITA_INICIO.getTime() - 2 * HORA);
    expect(calcularPenalizacion(CITA_INICIO, ahora, true)).toBe(0);
  });
});

// pruebas de limites agregadas despues de revisar los mutantes que sobrevivieron
describe('limites encontrados con Stryker', () => {
  test.each([
    [0, true],
    [13, true],
    [14, false],
    [120, false],
  ])('esPediatrico acepta la edad %i y responde %s', (edad, esperado) => {
    expect(esPediatrico(edad)).toBe(esperado);
  });

  test('la clinica abre a las 8 en punto', () => {
    expect(validarHorario(new Date(2026, 9, 19, 8, 0))).toBeUndefined();
  });

  test('cita cancelada no corta la revision de las demas', () => {
    const fin = new Date(LUNES_9AM.getTime() + 30 * MINUTO);
    const cancelada = hacerCita(LUNES_9AM, fin, 'cancelada');
    const activa = hacerCita(LUNES_9AM, fin);
    expect(hayChoque([cancelada, activa], LUNES_9AM, fin)).toBe(true);
  });

  test('exactamente 24 horas antes no cobra', () => {
    const inicio = new Date(2026, 9, 20, 9, 0);
    const ahora = new Date(inicio.getTime() - 24 * HORA);
    expect(calcularPenalizacion(inicio, ahora, false)).toBe(0);
  });
});
