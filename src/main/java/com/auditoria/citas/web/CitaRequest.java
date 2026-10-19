package com.auditoria.citas.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// datos que llegan al agendar una cita
public record CitaRequest(
        @NotBlank String paciente,
        @NotNull Integer edad,
        @NotBlank String tipo,
        @NotBlank String inicio) {
}
