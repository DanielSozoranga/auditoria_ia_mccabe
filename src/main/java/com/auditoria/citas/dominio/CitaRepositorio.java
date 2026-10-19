package com.auditoria.citas.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// puerto de salida: lo que el sistema necesita de la base de datos
public interface CitaRepositorio {

    // guarda una cita nueva y devuelve su id
    long guardar(String paciente, TipoConsulta tipo, LocalDateTime inicio, LocalDateTime fin);

    // busca una cita por id
    Optional<Cita> buscarPorId(long id);

    // trae las citas que empiezan en el dia indicado
    List<Cita> listarDelDia(LocalDate dia);

    // trae todas las citas ordenadas por fecha
    List<Cita> listarTodas();

    // cambia el estado a cancelada y guarda la multa
    void marcarCancelada(long id, BigDecimal penalizacion);
}
