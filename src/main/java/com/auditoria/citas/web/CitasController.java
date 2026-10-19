package com.auditoria.citas.web;

import com.auditoria.citas.aplicacion.ServicioCitas;
import com.auditoria.citas.dominio.Reglas;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API de citas: lista, agenda y cancela
@RestController
@RequestMapping("/api/citas")
public class CitasController {

    private final ServicioCitas servicio;
    private final ZoneId zona;

    public CitasController(ServicioCitas servicio, ZoneId zona) {
        this.servicio = servicio;
        this.zona = zona;
    }

    // lista todas las citas
    @GetMapping
    public List<CitaRespuesta> listar() {
        return servicio.listar().stream().map(cita -> CitaRespuesta.desde(cita, zona)).toList();
    }

    // agenda una cita nueva y responde su id
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> agendar(@Valid @RequestBody CitaRequest peticion) {
        LocalDateTime inicio = ConversorFechas.aHoraLocal(peticion.inicio(), zona);
        long id = servicio.agendar(
                peticion.paciente(), peticion.edad(), Reglas.tipoDesdeTexto(peticion.tipo()), inicio);
        return Map.of("id", id);
    }

    // cancela una cita y responde cuanto se cobro
    @PostMapping("/{id}/cancelar")
    public Map<String, BigDecimal> cancelar(
            @PathVariable long id, @RequestBody(required = false) CancelarRequest peticion) {
        boolean esEmergencia = peticion != null && Boolean.TRUE.equals(peticion.esEmergencia());
        return Map.of("penalizacion", servicio.cancelar(id, esEmergencia));
    }
}
