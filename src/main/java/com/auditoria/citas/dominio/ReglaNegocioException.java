package com.auditoria.citas.dominio;

// error que se lanza cuando se rompe una regla del negocio
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
