package com.auditoria.citas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.TipoConsulta;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

// simula apagar y volver a prender el servidor: dos conexiones nuevas al mismo archivo
class PersistenciaTest {

    private static final LocalDateTime LUNES_9AM = LocalDateTime.of(2026, 10, 19, 9, 0);

    @TempDir
    Path carpeta;

    // abre una conexion nueva al archivo y crea la tabla si falta
    private DataSource abrirBase() {
        SQLiteDataSource base = new SQLiteDataSource();
        base.setUrl("jdbc:sqlite:" + carpeta.resolve("citas.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(base);
        return base;
    }

    @Test
    void lasCitasSiguenGuardadasDespuesDeReabrirLaBase() {
        CitaRepositorioJdbc antes = new CitaRepositorioJdbc(JdbcClient.create(abrirBase()));
        long id = antes.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_9AM.plusMinutes(30));
        antes.marcarCancelada(id, new BigDecimal("15"));

        CitaRepositorioJdbc despues = new CitaRepositorioJdbc(JdbcClient.create(abrirBase()));

        Cita cita = despues.buscarPorId(id).orElseThrow();
        assertThat(despues.listarTodas()).hasSize(1);
        assertThat(cita.paciente()).isEqualTo("Ana");
        assertThat(cita.inicio()).isEqualTo(LUNES_9AM);
        assertThat(cita.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.penalizacion()).isEqualByComparingTo("15");
    }

    @Test
    void crearLaTablaDosVecesNoBorraLosDatos() {
        CitaRepositorioJdbc primera = new CitaRepositorioJdbc(JdbcClient.create(abrirBase()));
        primera.guardar("Ana", TipoConsulta.GENERAL, LUNES_9AM, LUNES_9AM.plusMinutes(30));

        abrirBase();

        assertThat(primera.listarTodas()).hasSize(1);
    }
}
