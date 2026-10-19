package com.auditoria.citas.aplicacion;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.CitaRepositorio;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.TipoConsulta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// repositorio falso que guarda en memoria, sirve para probar el servicio sin base de datos
class RepositorioEnMemoria implements CitaRepositorio {

    private final Map<Long, Cita> citas = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public long guardar(String paciente, TipoConsulta tipo, LocalDateTime inicio, LocalDateTime fin) {
        long id = siguienteId++;
        citas.put(id, new Cita(id, paciente, tipo, inicio, fin, EstadoCita.ACTIVA, BigDecimal.ZERO));
        return id;
    }

    @Override
    public Optional<Cita> buscarPorId(long id) {
        return Optional.ofNullable(citas.get(id));
    }

    @Override
    public List<Cita> listarDelDia(LocalDate dia) {
        return citas.values().stream().filter(cita -> cita.inicio().toLocalDate().equals(dia)).toList();
    }

    @Override
    public List<Cita> listarTodas() {
        return citas.values().stream().sorted(Comparator.comparing(Cita::inicio)).toList();
    }

    @Override
    public void marcarCancelada(long id, BigDecimal penalizacion) {
        Cita actual = citas.get(id);
        citas.put(id, new Cita(id, actual.paciente(), actual.tipo(), actual.inicio(), actual.fin(),
                EstadoCita.CANCELADA, penalizacion));
    }
}
