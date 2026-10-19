package com.auditoria.citas.web;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auditoria.citas.aplicacion.ServicioCitas;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// solo la capa web con el servicio simulado, para provocar fallos que la base real no da
@WebMvcTest(CitasController.class)
class ErroresInesperadosTest {

    @TestConfiguration
    static class Zona {
        @Bean
        ZoneId zonaHoraria() {
            return ZoneId.of("America/Guayaquil");
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServicioCitas servicio;

    @Test
    void unFalloInesperadoAlListarResponde500SinDetalles() throws Exception {
        when(servicio.listar()).thenThrow(new IllegalStateException("detalle interno secreto"));

        mvc.perform(get("/api/citas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error del servidor"));
    }

    @Test
    void unFalloInesperadoAlCancelarResponde500() throws Exception {
        when(servicio.cancelar(anyLong(), anyBoolean())).thenThrow(new IllegalStateException("fallo"));

        mvc.perform(post("/api/citas/1/cancelar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error del servidor"));
    }

    @Test
    void unaRutaInexistenteResponde404ConElFormatoDeLaApi() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }

    @Test
    void unMetodoNoPermitidoResponde405ConElFormatoDeLaApi() throws Exception {
        mvc.perform(get("/api/citas/1/cancelar"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Peticion invalida"));
    }
}
