package com.auditoria.citas.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConversorFechas, V(G) = 3, tres caminos")
class ConversorFechasTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");

    @Test
    void fechaSinZonaSeTomaComoHoraDelConsultorio() {
        assertThat(ConversorFechas.aHoraLocal("2026-10-19T09:00", GUAYAQUIL))
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 0));
    }

    @Test
    void fechaConZonaSeConvierteALaHoraDelConsultorio() {
        assertThat(ConversorFechas.aHoraLocal("2026-10-19T14:00:00Z", GUAYAQUIL))
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 0));
    }

    @Test
    void fechaConOtroDesfaseSeConvierteTambien() {
        assertThat(ConversorFechas.aHoraLocal("2026-10-19T15:00:00+01:00", GUAYAQUIL))
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 0));
    }

    @Test
    void fechaInvalidaFalla() {
        assertThatThrownBy(() -> ConversorFechas.aHoraLocal("manana", GUAYAQUIL))
                .isInstanceOf(EntradaInvalidaException.class)
                .hasMessage("Faltan datos o la fecha es invalida");
    }
}
