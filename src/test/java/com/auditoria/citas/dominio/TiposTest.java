package com.auditoria.citas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// los textos de los enums son parte del contrato de la API y de la base
class TiposTest {

    @Test
    void tipoConsultaTieneSuTexto() {
        assertThat(TipoConsulta.PEDIATRICA.texto()).isEqualTo("pediatrica");
        assertThat(TipoConsulta.GENERAL.texto()).isEqualTo("general");
    }

    @Test
    void tipoConsultaSeBuscaPorTexto() {
        assertThat(TipoConsulta.desdeTexto("general")).contains(TipoConsulta.GENERAL);
        assertThat(TipoConsulta.desdeTexto("otro")).isEmpty();
        assertThat(TipoConsulta.desdeTexto(null)).isEmpty();
    }

    @Test
    void estadoCitaTieneSuTexto() {
        assertThat(EstadoCita.ACTIVA.texto()).isEqualTo("activa");
        assertThat(EstadoCita.CANCELADA.texto()).isEqualTo("cancelada");
    }

    @Test
    void estadoCitaSeBuscaPorTexto() {
        assertThat(EstadoCita.desdeTexto("cancelada")).contains(EstadoCita.CANCELADA);
        assertThat(EstadoCita.desdeTexto("otro")).isEmpty();
    }
}
