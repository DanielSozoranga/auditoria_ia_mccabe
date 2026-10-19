package com.auditoria.citas.web;

// error para datos mal enviados por el cliente
public class EntradaInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntradaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
