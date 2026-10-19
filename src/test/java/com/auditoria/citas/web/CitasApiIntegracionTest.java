package com.auditoria.citas.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// prueba la API completa con SQLite real, cada test se revierte al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CitasApiIntegracionTest {

    // el reloj queda fijo el domingo 18 a las 10:00, 23 horas antes de la cita del lunes
    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneId.of("America/Guayaquil"));
        }
    }

    @Autowired
    private MockMvc mvc;

    // apunta la base de prueba a un archivo temporal
    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry registro) throws IOException {
        Path archivo = Files.createTempFile("citas-api-", ".db");
        archivo.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + archivo);
    }

    // datos validos para agendar
    private static String citaValida(String paciente, String inicio) {
        return """
                {"paciente":"%s","edad":30,"tipo":"general","inicio":"%s"}
                """.formatted(paciente, inicio);
    }

    private ResultActions enviar(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    @DisplayName("POST agenda una cita y GET la lista con la hora del consultorio")
    void agendaYLista() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T09:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mvc.perform(get("/api/citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paciente").value("Ana"))
                .andExpect(jsonPath("$[0].tipo").value("general"))
                .andExpect(jsonPath("$[0].estado").value("activa"))
                .andExpect(jsonPath("$[0].penalizacion").value(0))
                .andExpect(jsonPath("$[0].inicio").value("2026-10-19T09:00:00-05:00"))
                .andExpect(jsonPath("$[0].fin").value("2026-10-19T09:30:00-05:00"));
    }

    @Test
    void unaFechaConZonaSeConvierteALaHoraDelConsultorio() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T14:00:00Z")).andExpect(status().isCreated());

        mvc.perform(get("/api/citas")).andExpect(jsonPath("$[0].inicio").value("2026-10-19T09:00:00-05:00"));
    }

    @Test
    void sinCuerpoResponde400() throws Exception {
        mvc.perform(post("/api/citas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void faltandoUnDatoResponde400() throws Exception {
        enviar("/api/citas", "{\"edad\":30,\"tipo\":\"general\",\"inicio\":\"2026-10-19T09:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Faltan datos o la fecha es invalida"));
    }

    @Test
    void unaFechaInvalidaResponde400() throws Exception {
        enviar("/api/citas", citaValida("Ana", "manana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Faltan datos o la fecha es invalida"));
    }

    @Test
    void unaEdadEnTextoResponde400() throws Exception {
        enviar("/api/citas", "{\"paciente\":\"Ana\",\"edad\":\"30\",\"tipo\":\"general\",\"inicio\":\"2026-10-19T09:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void unaEdadConDecimalesResponde400() throws Exception {
        enviar("/api/citas", "{\"paciente\":\"Ana\",\"edad\":5.5,\"tipo\":\"pediatrica\",\"inicio\":\"2026-10-19T09:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void unJsonRotoResponde400() throws Exception {
        enviar("/api/citas", "{roto")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void unTipoDesconocidoResponde422() throws Exception {
        enviar("/api/citas", "{\"paciente\":\"Ana\",\"edad\":30,\"tipo\":\"otro\",\"inicio\":\"2026-10-19T09:00\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("El tipo de consulta no corresponde a la edad"));
    }

    @Test
    void unHorarioOcupadoResponde422() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T09:00"));

        enviar("/api/citas", citaValida("Luis", "2026-10-19T09:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Ese horario ya esta ocupado"));
    }

    @Test
    void cancelarTardeSinCuerpoCobraLaMulta() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T09:00"));

        mvc.perform(post("/api/citas/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penalizacion").value(15));
    }

    @Test
    void cancelarTardeComoEmergenciaNoCobra() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T09:00"));

        enviar("/api/citas/1/cancelar", "{\"esEmergencia\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penalizacion").value(0));
    }

    @Test
    void cancelarSinEmergenciaExplicitaCobra() throws Exception {
        enviar("/api/citas", citaValida("Ana", "2026-10-19T09:00"));

        enviar("/api/citas/1/cancelar", "{\"esEmergencia\":false}")
                .andExpect(jsonPath("$.penalizacion").value(15));
    }

    @Test
    void cancelarUnaCitaInexistenteResponde422() throws Exception {
        mvc.perform(post("/api/citas/99/cancelar"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La cita no existe"));
    }

    @Test
    void cancelarConUnIdQueNoEsNumeroResponde400() throws Exception {
        mvc.perform(post("/api/citas/abc/cancelar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void laRaizLlevaALaPaginaPrincipal() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void laPaginaPrincipalSeSirve() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Citas del medico familiar")));
    }

    @Test
    void alpineSeSirveDesdeElProyecto() throws Exception {
        mvc.perform(get("/vendor/alpine.js"))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getContentType())
                        .contains("javascript"));
    }
}
