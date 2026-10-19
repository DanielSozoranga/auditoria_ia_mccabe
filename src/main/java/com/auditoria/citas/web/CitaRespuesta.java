package com.auditoria.citas.web;

import com.auditoria.citas.dominio.Cita;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

// cita tal como la ve el cliente, con las fechas y el estado en texto
public record CitaRespuesta(
        long id,
        String paciente,
        String tipo,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        String estado,
        BigDecimal penalizacion) {

    // arma la respuesta con las fechas en la zona del consultorio
    public static CitaRespuesta desde(Cita cita, ZoneId zona) {
        return new CitaRespuesta(
                cita.id(),
                cita.paciente(),
                cita.tipo().texto(),
                cita.inicio().atZone(zona).toOffsetDateTime(),
                cita.fin().atZone(zona).toOffsetDateTime(),
                cita.estado().texto(),
                cita.penalizacion());
    }
}
