package com.auditoria.citas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.TipoConsulta;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// prueba el repositorio contra SQLite real en un archivo temporal, cada test se revierte al terminar
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CitaRepositorioJdbc.class)
class CitaRepositorioJdbcTest {

    private static final LocalDateTime LUNES_9AM = LocalDateTime.of(2026, 10, 19, 9, 0);
    private static final LocalDateTime LUNES_930AM = LocalDateTime.of(2026, 10, 19, 9, 30);

    @Autowired
    private CitaRepositorioJdbc repositorio;

    // apunta la base de prueba a un archivo temporal
    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry registro) throws IOException {
        Path archivo = Files.createTempFile("citas-repositorio-", ".db");
        archivo.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + archivo);
    }

    @Test
    void guardaYRecuperaUnaCita() {
        long id = repositorio.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_930AM);

        Cita cita = repositorio.buscarPorId(id).orElseThrow();
        assertThat(cita.id()).isEqualTo(id);
        assertThat(cita.paciente()).isEqualTo("Ana");
        assertThat(cita.tipo()).isEqualTo(TipoConsulta.GENERAL);
        assertThat(cita.inicio()).isEqualTo(LUNES_9AM);
        assertThat(cita.fin()).isEqualTo(LUNES_930AM);
        assertThat(cita.estado()).isEqualTo(EstadoCita.ACTIVA);
        assertThat(cita.penalizacion()).isEqualByComparingTo("0");
    }

    @Test
    void losIdsSonConsecutivos() {
        long primero = repositorio.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_930AM);
        long segundo = repositorio.guardar("Luis", TipoConsulta.GENERAL, LUNES_930AM, LUNES_930AM.plusMinutes(30));

        assertThat(segundo).isEqualTo(primero + 1);
    }

    @Test
    void buscarUnaCitaInexistenteDevuelveVacio() {
        assertThat(repositorio.buscarPorId(999)).isEmpty();
    }

    @Test
    void listarDelDiaSoloTraeLasDelDiaPedido() {
        repositorio.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_930AM);
        repositorio.guardar("Luis", TipoConsulta.GENERAL, LUNES_9AM.plusDays(1), LUNES_930AM.plusDays(1));
        repositorio.guardar("Eva", TipoConsulta.GENERAL, LUNES_9AM.minusDays(1), LUNES_930AM.minusDays(1));

        assertThat(repositorio.listarDelDia(LocalDate.of(2026, 10, 19)))
                .extracting(Cita::paciente)
                .containsExactly("Ana");
    }

    @Test
    void listarDelDiaIncluyeLaPrimeraYLaUltimaHoraDelDia() {
        LocalDateTime medianoche = LocalDateTime.of(2026, 10, 19, 0, 0);
        LocalDateTime finDelDia = LocalDateTime.of(2026, 10, 19, 23, 59, 59);
        repositorio.guardar("Madrugada", TipoConsulta.GENERAL, medianoche, medianoche.plusMinutes(30));
        repositorio.guardar("Noche", TipoConsulta.GENERAL, finDelDia, finDelDia.plusMinutes(30));
        repositorio.guardar("Siguiente", TipoConsulta.GENERAL, medianoche.plusDays(1), medianoche.plusDays(1));

        assertThat(repositorio.listarDelDia(LocalDate.of(2026, 10, 19)))
                .extracting(Cita::paciente)
                .containsExactlyInAnyOrder("Madrugada", "Noche");
    }

    @Test
    void listarTodasDevuelveLasCitasOrdenadasPorFecha() {
        repositorio.guardar("Luis", TipoConsulta.GENERAL, LUNES_930AM, LUNES_930AM.plusMinutes(30));
        repositorio.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_930AM);

        assertThat(repositorio.listarTodas()).extracting(Cita::paciente).containsExactly("Ana", "Luis");
    }

    @Test
    void marcarCanceladaGuardaElEstadoYLaMulta() {
        long id = repositorio.guardar("Ana", TipoConsulta.PEDIATRICA, LUNES_9AM, LUNES_930AM);

        repositorio.marcarCancelada(id, new BigDecimal("15"));

        Cita cita = repositorio.buscarPorId(id).orElseThrow();
        assertThat(cita.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.penalizacion()).isEqualByComparingTo("15");
        assertThat(cita.tipo()).isEqualTo(TipoConsulta.PEDIATRICA);
    }
}
