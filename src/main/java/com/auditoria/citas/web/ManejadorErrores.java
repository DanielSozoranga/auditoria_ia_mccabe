package com.auditoria.citas.web;

import com.auditoria.citas.dominio.ReglaNegocioException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// convierte los errores en respuestas con el codigo correcto y el cuerpo {"error": "..."}
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorErrores.class);

    // arma la respuesta de error con el formato de la API
    private static ResponseEntity<Object> respuesta(HttpStatusCode estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }

    // 400 cuando faltan campos obligatorios
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error, HttpHeaders cabeceras, HttpStatusCode estado,
            WebRequest peticion) {
        return respuesta(HttpStatus.BAD_REQUEST, ConversorFechas.MENSAJE_FECHA);
    }

    // JSON roto, tipo de dato incorrecto, ruta inexistente y demas errores de Spring MVC
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception error, Object cuerpo, HttpHeaders cabeceras, HttpStatusCode estado,
            WebRequest peticion) {
        return respuesta(estado, "Peticion invalida");
    }

    // 400 cuando la fecha o los datos no se pueden leer
    @ExceptionHandler(EntradaInvalidaException.class)
    public ResponseEntity<Object> entradaInvalida(EntradaInvalidaException error) {
        return respuesta(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    // 422 cuando se rompe una regla del negocio
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Object> reglaRota(ReglaNegocioException error) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
    }

    // 500 para cualquier fallo inesperado, el detalle solo va al registro
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> falloInesperado(Exception error) {
        REGISTRO.error("Fallo inesperado", error);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error del servidor");
    }
}
