package com.auditoria.citas.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

// convierte el texto de una fecha en la hora local del consultorio
public final class ConversorFechas {

    static final String MENSAJE_FECHA = "Faltan datos o la fecha es invalida";

    private ConversorFechas() {
    }

    // acepta fecha con zona (se convierte) o sin zona (se toma como hora del consultorio)
    public static LocalDateTime aHoraLocal(String texto, ZoneId zona) {
        try {
            TemporalAccessor fecha = DateTimeFormatter.ISO_DATE_TIME
                    .parseBest(texto, OffsetDateTime::from, LocalDateTime::from);
            if (fecha instanceof OffsetDateTime conZona) {
                return conZona.atZoneSameInstant(zona).toLocalDateTime();
            }
            return (LocalDateTime) fecha;
        } catch (DateTimeParseException error) {
            throw new EntradaInvalidaException(MENSAJE_FECHA);
        }
    }
}
