package com.auditoria.citas.web;

// datos opcionales al cancelar una cita
public record CancelarRequest(Boolean esEmergencia) {
}
