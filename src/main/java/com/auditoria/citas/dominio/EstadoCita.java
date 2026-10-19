package com.auditoria.citas.dominio;

import java.util.Arrays;
import java.util.Optional;

// estados posibles de una cita
public enum EstadoCita {
    ACTIVA("activa"),
    CANCELADA("cancelada");

    private final String texto;

    EstadoCita(String texto) {
        this.texto = texto;
    }

    // texto que viaja en la API y se guarda en la base
    public String texto() {
        return texto;
    }

    // busca el estado por su texto, vacio si no existe
    public static Optional<EstadoCita> desdeTexto(String texto) {
        return Arrays.stream(values()).filter(estado -> estado.texto.equals(texto)).findFirst();
    }
}
