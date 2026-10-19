package com.auditoria.citas.dominio;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// las 4 reglas de negocio como funciones puras, sin base de datos ni reloj
public final class Reglas {

    public static final int EDAD_MAXIMA = 120;
    public static final int EDAD_LIMITE_PEDIATRICA = 14;
    public static final int HORA_APERTURA = 8;
    public static final int HORA_CIERRE = 17;
    public static final int HORA_ALMUERZO = 12;
    public static final int HORAS_LIMITE_CANCELACION = 24;
    public static final BigDecimal PENALIZACION = BigDecimal.valueOf(15);

    static final String MENSAJE_EDAD = "Edad fuera de rango";
    static final String MENSAJE_TIPO = "El tipo de consulta no corresponde a la edad";
    static final String MENSAJE_FIN_DE_SEMANA = "No se atiende en fin de semana";
    static final String MENSAJE_FUERA_DE_HORARIO = "Fuera del horario de atencion";
    static final String MENSAJE_ALMUERZO = "Hora de almuerzo no disponible";

    private Reglas() {
    }

    // convierte el texto de la API en un tipo de consulta, o falla si no existe
    public static TipoConsulta tipoDesdeTexto(String texto) {
        return TipoConsulta.desdeTexto(texto).orElseThrow(() -> new ReglaNegocioException(MENSAJE_TIPO));
    }

    // dice si el paciente es menor de 14 anios y rechaza edades imposibles
    public static boolean esPediatrico(int edad) {
        if (edad < 0 || edad > EDAD_MAXIMA) {
            throw new ReglaNegocioException(MENSAJE_EDAD);
        }
        return edad < EDAD_LIMITE_PEDIATRICA;
    }

    // los menores van a pediatria y los adultos a medicina general
    public static void validarTipoConsulta(int edad, TipoConsulta tipo) {
        TipoConsulta esperado = esPediatrico(edad) ? TipoConsulta.PEDIATRICA : TipoConsulta.GENERAL;
        if (tipo != esperado) {
            throw new ReglaNegocioException(MENSAJE_TIPO);
        }
    }

    // bloquea fines de semana, fuera de horario y la hora de almuerzo
    public static void validarHorario(LocalDateTime inicio) {
        DayOfWeek dia = inicio.getDayOfWeek();
        int hora = inicio.getHour();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new ReglaNegocioException(MENSAJE_FIN_DE_SEMANA);
        }
        if (hora < HORA_APERTURA || hora >= HORA_CIERRE) {
            throw new ReglaNegocioException(MENSAJE_FUERA_DE_HORARIO);
        }
        if (hora == HORA_ALMUERZO) {
            throw new ReglaNegocioException(MENSAJE_ALMUERZO);
        }
    }

    // revisa si el nuevo horario se cruza con alguna cita activa
    public static boolean hayChoque(List<Cita> citas, LocalDateTime inicio, LocalDateTime fin) {
        for (Cita cita : citas) {
            if (cita.estado() == EstadoCita.CANCELADA) {
                continue;
            }
            if (cita.inicio().isBefore(fin) && inicio.isBefore(cita.fin())) {
                return true;
            }
        }
        return false;
    }

    // cobra multa si cancelan con menos de 24h y no es emergencia
    public static BigDecimal calcularPenalizacion(LocalDateTime inicio, LocalDateTime ahora, boolean esEmergencia) {
        boolean tardia = Duration.between(ahora, inicio).compareTo(Duration.ofHours(HORAS_LIMITE_CANCELACION)) < 0;
        if (tardia && !esEmergencia) {
            return PENALIZACION;
        }
        return BigDecimal.ZERO;
    }
}
