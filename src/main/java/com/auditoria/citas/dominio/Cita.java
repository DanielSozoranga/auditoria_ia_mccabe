package com.auditoria.citas.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// datos de una cita, las fechas estan en la hora local del consultorio
public record Cita(
        long id,
        String paciente,
        TipoConsulta tipo,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoCita estado,
        BigDecimal penalizacion) {
}
