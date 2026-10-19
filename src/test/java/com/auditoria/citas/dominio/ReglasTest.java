package com.auditoria.citas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReglasTest {

    // lunes 19 de octubre de 2026, dia habil para las pruebas
    private static final LocalDateTime LUNES_9AM = LocalDateTime.of(2026, 10, 19, 9, 0);
    private static final LocalDateTime DOMINGO_9AM = LocalDateTime.of(2026, 10, 18, 9, 0);
    private static final LocalDateTime SABADO_9AM = LocalDateTime.of(2026, 10, 24, 9, 0);

    // comprueba el tipo de error y su mensaje exacto
    private static void esperarError(Runnable accion, String mensaje) {
        assertThatThrownBy(accion::run)
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje);
    }

    // arma una cita de prueba con los datos minimos
    private static Cita cita(LocalDateTime inicio, LocalDateTime fin, EstadoCita estado) {
        return new Cita(1, "Ana", TipoConsulta.GENERAL, inicio, fin, estado, BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("esPediatrico, V(G) = 3, tres caminos")
    class EsPediatrico {

        @Test
        void edadNegativaFalla() {
            esperarError(() -> Reglas.esPediatrico(-1), "Edad fuera de rango");
        }

        @Test
        void edadMayorAlMaximoFalla() {
            esperarError(() -> Reglas.esPediatrico(121), "Edad fuera de rango");
        }

        @Test
        void edadValidaDeUnNinoResponde() {
            assertThat(Reglas.esPediatrico(5)).isTrue();
        }

        // limites encontrados al medir las mutaciones
        @ParameterizedTest(name = "la edad {0} responde {1}")
        @CsvSource({"0,true", "13,true", "14,false", "120,false"})
        void aceptaLosLimites(int edad, boolean esperado) {
            assertThat(Reglas.esPediatrico(edad)).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("validarTipoConsulta, V(G) = 3, tres caminos")
    class ValidarTipoConsulta {

        @Test
        void menorEnPediatriaPasa() {
            Reglas.validarTipoConsulta(5, TipoConsulta.PEDIATRICA);
        }

        @Test
        void adultoEnGeneralPasa() {
            Reglas.validarTipoConsulta(30, TipoConsulta.GENERAL);
        }

        @Test
        void menorEnGeneralFalla() {
            esperarError(
                    () -> Reglas.validarTipoConsulta(5, TipoConsulta.GENERAL),
                    "El tipo de consulta no corresponde a la edad");
        }
    }

    @Nested
    @DisplayName("tipoDesdeTexto, V(G) = 1, un camino")
    class TipoDesdeTexto {

        @Test
        void textoConocidoDevuelveElTipo() {
            assertThat(Reglas.tipoDesdeTexto("pediatrica")).isEqualTo(TipoConsulta.PEDIATRICA);
        }

        @Test
        void textoDesconocidoFalla() {
            esperarError(() -> Reglas.tipoDesdeTexto("otro"), "El tipo de consulta no corresponde a la edad");
        }
    }

    @Nested
    @DisplayName("validarHorario, V(G) = 6, seis caminos")
    class ValidarHorario {

        @Test
        void domingoFalla() {
            esperarError(() -> Reglas.validarHorario(DOMINGO_9AM), "No se atiende en fin de semana");
        }

        @Test
        void sabadoFalla() {
            esperarError(() -> Reglas.validarHorario(SABADO_9AM), "No se atiende en fin de semana");
        }

        @Test
        void antesDeAbrirFalla() {
            esperarError(
                    () -> Reglas.validarHorario(LUNES_9AM.withHour(7)), "Fuera del horario de atencion");
        }

        @Test
        void despuesDeCerrarFalla() {
            esperarError(
                    () -> Reglas.validarHorario(LUNES_9AM.withHour(17)), "Fuera del horario de atencion");
        }

        @Test
        void horaDeAlmuerzoFalla() {
            esperarError(
                    () -> Reglas.validarHorario(LUNES_9AM.withHour(12)), "Hora de almuerzo no disponible");
        }

        @Test
        void diaYHoraHabilesPasan() {
            Reglas.validarHorario(LUNES_9AM);
        }

        // limite encontrado al medir las mutaciones
        @Test
        void laClinicaAbreALas8EnPunto() {
            Reglas.validarHorario(LUNES_9AM.withHour(8));
        }

        @Test
        void laUltimaHoraDeAtencionEsLas16() {
            Reglas.validarHorario(LUNES_9AM.withHour(16));
        }
    }

    @Nested
    @DisplayName("hayChoque, V(G) = 5, cinco caminos")
    class HayChoque {

        private final LocalDateTime fin9am = LUNES_9AM.plusMinutes(30);

        @Test
        void sinCitasNoHayChoque() {
            assertThat(Reglas.hayChoque(List.of(), LUNES_9AM, fin9am)).isFalse();
        }

        @Test
        void ignoraLasCitasCanceladas() {
            Cita cancelada = cita(LUNES_9AM, fin9am, EstadoCita.CANCELADA);
            assertThat(Reglas.hayChoque(List.of(cancelada), LUNES_9AM, fin9am)).isFalse();
        }

        @Test
        void citaQueEmpiezaDespuesNoChoca() {
            Cita despues = cita(fin9am, fin9am.plusMinutes(30), EstadoCita.ACTIVA);
            assertThat(Reglas.hayChoque(List.of(despues), LUNES_9AM, fin9am)).isFalse();
        }

        @Test
        void citaQueTerminaAntesNoChoca() {
            Cita antes = cita(LUNES_9AM.minusMinutes(30), LUNES_9AM, EstadoCita.ACTIVA);
            assertThat(Reglas.hayChoque(List.of(antes), LUNES_9AM, fin9am)).isFalse();
        }

        @Test
        void citaActivaCruzadaSiChoca() {
            Cita cruzada = cita(LUNES_9AM, fin9am, EstadoCita.ACTIVA);
            assertThat(Reglas.hayChoque(List.of(cruzada), LUNES_9AM, fin9am)).isTrue();
        }

        // limite encontrado al medir las mutaciones
        @Test
        void citaCanceladaNoCortaLaRevisionDeLasDemas() {
            Cita cancelada = cita(LUNES_9AM, fin9am, EstadoCita.CANCELADA);
            Cita activa = cita(LUNES_9AM, fin9am, EstadoCita.ACTIVA);
            assertThat(Reglas.hayChoque(List.of(cancelada, activa), LUNES_9AM, fin9am)).isTrue();
        }
    }

    @Nested
    @DisplayName("calcularPenalizacion, V(G) = 3, tres caminos")
    class CalcularPenalizacion {

        private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 9, 0);

        @Test
        void cancelarConTiempoNoCobra() {
            BigDecimal multa = Reglas.calcularPenalizacion(inicio, inicio.minusHours(48), false);
            assertThat(multa).isEqualByComparingTo("0");
        }

        @Test
        void cancelarTardeSinEmergenciaCobra() {
            BigDecimal multa = Reglas.calcularPenalizacion(inicio, inicio.minusHours(2), false);
            assertThat(multa).isEqualByComparingTo("15");
        }

        @Test
        void cancelarTardeConEmergenciaNoCobra() {
            BigDecimal multa = Reglas.calcularPenalizacion(inicio, inicio.minusHours(2), true);
            assertThat(multa).isEqualByComparingTo("0");
        }

        // limite encontrado al medir las mutaciones
        @Test
        void exactamente24HorasAntesNoCobra() {
            BigDecimal multa = Reglas.calcularPenalizacion(inicio, inicio.minusHours(24), false);
            assertThat(multa).isEqualByComparingTo("0");
        }

        @Test
        void unMinutoAntesDeLas24HorasCobra() {
            BigDecimal multa = Reglas.calcularPenalizacion(inicio, inicio.minusHours(24).plusMinutes(1), false);
            assertThat(multa).isEqualByComparingTo("15");
        }
    }
}
