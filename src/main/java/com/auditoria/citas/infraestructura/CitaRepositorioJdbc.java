package com.auditoria.citas.infraestructura;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.CitaRepositorio;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.TipoConsulta;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

// guarda y lee las citas en SQLite con consultas parametrizadas
@Repository
public class CitaRepositorioJdbc implements CitaRepositorio {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JdbcClient jdbc;

    public CitaRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // convierte una fila de la base en una cita con fechas reales
    private static Cita aCita(ResultSet fila, int numeroDeFila) throws SQLException {
        return new Cita(
                fila.getLong("id"),
                fila.getString("paciente"),
                TipoConsulta.desdeTexto(fila.getString("tipo")).orElseThrow(),
                LocalDateTime.parse(fila.getString("inicio"), FORMATO),
                LocalDateTime.parse(fila.getString("fin"), FORMATO),
                EstadoCita.desdeTexto(fila.getString("estado")).orElseThrow(),
                new BigDecimal(fila.getString("penalizacion")));
    }

    // guarda una cita nueva y devuelve su id
    @Override
    public long guardar(String paciente, TipoConsulta tipo, LocalDateTime inicio, LocalDateTime fin) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO citas (paciente, tipo, inicio, fin) VALUES (:paciente, :tipo, :inicio, :fin)")
                .param("paciente", paciente)
                .param("tipo", tipo.texto())
                .param("inicio", FORMATO.format(inicio))
                .param("fin", FORMATO.format(fin))
                .update(claves);
        return Objects.requireNonNull(claves.getKey(), "SQLite no devolvio el id de la cita").longValue();
    }

    // busca una cita por id, vacio si no existe
    @Override
    public Optional<Cita> buscarPorId(long id) {
        return jdbc.sql("SELECT * FROM citas WHERE id = :id")
                .param("id", id)
                .query(CitaRepositorioJdbc::aCita)
                .optional();
    }

    // trae las citas que empiezan en el dia indicado
    @Override
    public List<Cita> listarDelDia(LocalDate dia) {
        LocalDateTime desde = dia.atStartOfDay();
        return jdbc.sql("SELECT * FROM citas WHERE inicio >= :desde AND inicio < :hasta")
                .param("desde", FORMATO.format(desde))
                .param("hasta", FORMATO.format(desde.plusDays(1)))
                .query(CitaRepositorioJdbc::aCita)
                .list();
    }

    // trae todas las citas ordenadas por fecha
    @Override
    public List<Cita> listarTodas() {
        return jdbc.sql("SELECT * FROM citas ORDER BY inicio")
                .query(CitaRepositorioJdbc::aCita)
                .list();
    }

    // cambia el estado a cancelada y guarda la multa
    @Override
    public void marcarCancelada(long id, BigDecimal penalizacion) {
        jdbc.sql("UPDATE citas SET estado = 'cancelada', penalizacion = :penalizacion WHERE id = :id")
                .param("penalizacion", penalizacion.toPlainString())
                .param("id", id)
                .update();
    }
}
