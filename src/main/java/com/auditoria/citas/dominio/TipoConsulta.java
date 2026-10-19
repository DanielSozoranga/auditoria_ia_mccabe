package com.auditoria.citas.dominio;

import java.util.Arrays;
import java.util.Optional;

// tipos de consulta que ofrece el consultorio
public enum TipoConsulta {
    PEDIATRICA("pediatrica"),
    GENERAL("general");

    private final String texto;

    TipoConsulta(String texto) {
        this.texto = texto;
    }

    // texto que viaja en la API y se guarda en la base
    public String texto() {
        return texto;
    }

    // busca el tipo por su texto, vacio si no existe
    public static Optional<TipoConsulta> desdeTexto(String texto) {
        return Arrays.stream(values()).filter(tipo -> tipo.texto.equals(texto)).findFirst();
    }
}
