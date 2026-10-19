package com.auditoria.citas.aplicacion;

import com.auditoria.citas.dominio.Cita;
import com.auditoria.citas.dominio.CitaRepositorio;
import com.auditoria.citas.dominio.EstadoCita;
import com.auditoria.citas.dominio.ReglaNegocioException;
import com.auditoria.citas.dominio.Reglas;
import com.auditoria.citas.dominio.TipoConsulta;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// junta las reglas con la base: valida y luego guarda
@Service
public class ServicioCitas {

    static final int DURACION_MINUTOS = 30;

    private final CitaRepositorio repositorio;
    private final Clock reloj;

    public ServicioCitas(CitaRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    // aplica las reglas de edad, horario y choque y guarda la cita
    @Transactional
    public long agendar(String paciente, int edad, TipoConsulta tipo, LocalDateTime inicio) {
        Reglas.validarTipoConsulta(edad, tipo);
        Reglas.validarHorario(inicio);
        LocalDateTime fin = inicio.plusMinutes(DURACION_MINUTOS);
        if (Reglas.hayChoque(repositorio.listarDelDia(inicio.toLocalDate()), inicio, fin)) {
            throw new ReglaNegocioException("Ese horario ya esta ocupado");
        }
        return repositorio.guardar(paciente, tipo, inicio, fin);
    }

    // cancela la cita y devuelve la penalizacion cobrada
    @Transactional
    public BigDecimal cancelar(long id, boolean esEmergencia) {
        Cita cita = repositorio.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("La cita no existe"));
        if (cita.estado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("La cita ya estaba cancelada");
        }
        BigDecimal multa = Reglas.calcularPenalizacion(cita.inicio(), LocalDateTime.now(reloj), esEmergencia);
        repositorio.marcarCancelada(id, multa);
        return multa;
    }

    // lista todas las citas ordenadas por fecha
    public List<Cita> listar() {
        return repositorio.listarTodas();
    }
}
