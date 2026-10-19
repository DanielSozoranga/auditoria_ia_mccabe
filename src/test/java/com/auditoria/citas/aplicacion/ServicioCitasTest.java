package com.auditoria.citas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.ReglaNegocioException;
import com.auditoria.citas.dominio.TipoConsulta;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServicioCitasTest {

    private static final LocalDateTime LUNES_9AM = LocalDateTime.of(2026, 10, 19, 9, 0);

    private RepositorioEnMemoria repositorio;
    private ServicioCitas servicio;

    // el reloj queda fijo el domingo 18 a las 10:00, 23 horas antes de la cita del lunes
    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneId.of("America/Guayaquil"));
        servicio = new ServicioCitas(repositorio, reloj);
    }

    @Nested
    @DisplayName("agendar, V(G) = 2, dos caminos")
    class Agendar {

        @Test
        void guardaLaCitaConFinA30Minutos() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);

            Cita cita = repositorio.buscarPorId(id).orElseThrow();
            assertThat(cita.paciente()).isEqualTo("Ana");
            assertThat(cita.estado()).isEqualTo(EstadoCita.ACTIVA);
            assertThat(cita.inicio()).isEqualTo(LUNES_9AM);
            assertThat(cita.fin()).isEqualTo(LUNES_9AM.plusMinutes(30));
        }

        @Test
        void horarioOcupadoFallaYNoGuarda() {
            servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);

            assertThatThrownBy(() -> servicio.agendar("Luis", 40, TipoConsulta.GENERAL, LUNES_9AM))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Ese horario ya esta ocupado");
            assertThat(repositorio.listarDelDia(LUNES_9AM.toLocalDate())).hasSize(1);
        }

        // pruebas agregadas despues de medir las mutaciones
        @Test
        void rechazaUnTipoQueNoCorrespondeALaEdad() {
            assertThatThrownBy(() -> servicio.agendar("Ana", 5, TipoConsulta.GENERAL, LUNES_9AM))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El tipo de consulta no corresponde a la edad");
            assertThat(repositorio.listarTodas()).isEmpty();
        }

        @Test
        void rechazaUnHorarioDeFinDeSemana() {
            LocalDateTime domingo = LocalDateTime.of(2026, 10, 18, 9, 0);

            assertThatThrownBy(() -> servicio.agendar("Ana", 30, TipoConsulta.GENERAL, domingo))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("No se atiende en fin de semana");
            assertThat(repositorio.listarTodas()).isEmpty();
        }

        @Test
        void unaCitaCanceladaLiberaElHorario() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);
            servicio.cancelar(id, true);

            long nueva = servicio.agendar("Luis", 40, TipoConsulta.GENERAL, LUNES_9AM);

            assertThat(nueva).isNotEqualTo(id);
        }
    }

    @Nested
    @DisplayName("cancelar, V(G) = 2, dos caminos mas los casos de multa")
    class Cancelar {

        @Test
        void citaInexistenteFalla() {
            assertThatThrownBy(() -> servicio.cancelar(999, false))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cita no existe");
        }

        @Test
        void citaYaCanceladaFalla() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);
            servicio.cancelar(id, true);

            assertThatThrownBy(() -> servicio.cancelar(id, false))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cita ya estaba cancelada");
        }

        @Test
        void cancelarTardeCobraYGuardaLaMulta() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);

            BigDecimal multa = servicio.cancelar(id, false);

            Cita cita = repositorio.buscarPorId(id).orElseThrow();
            assertThat(multa).isEqualByComparingTo("15");
            assertThat(cita.estado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(cita.penalizacion()).isEqualByComparingTo("15");
        }

        @Test
        void cancelarTardeComoEmergenciaNoCobra() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);

            assertThat(servicio.cancelar(id, true)).isEqualByComparingTo("0");
        }

        @Test
        void cancelarConTiempoNoCobra() {
            long id = servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM.plusDays(1));

            assertThat(servicio.cancelar(id, false)).isEqualByComparingTo("0");
        }
    }

    @Test
    void listarDevuelveLasCitasOrdenadasPorFecha() {
        servicio.agendar("Luis", 40, TipoConsulta.GENERAL, LUNES_9AM.plusHours(1));
        servicio.agendar("Ana", 30, TipoConsulta.GENERAL, LUNES_9AM);

        assertThat(servicio.listar()).extracting(Cita::paciente).containsExactly("Ana", "Luis");
    }
}
