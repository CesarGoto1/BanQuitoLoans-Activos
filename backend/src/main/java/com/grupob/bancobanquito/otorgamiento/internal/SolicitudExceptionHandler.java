package com.grupob.bancobanquito.otorgamiento.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.grupob.bancobanquito.otorgamiento.internal")
public class SolicitudExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SolicitudExceptionHandler.class);

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    public ResponseEntity<ProblemDetail> manejarSolicitudNoEncontrada(SolicitudNoEncontradaException ex) {
        log.warn("Solicitud no encontrada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Solicitud de crédito no encontrada");
        problem.setType(URI.create("urn:banquito:error:solicitud-no-encontrada"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AccesoDenegadoAnalistaException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccesoDenegadoAnalistaException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acceso no autorizado");
        problem.setType(URI.create("urn:banquito:error:acceso-denegado"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(CabeceraFaltanteException.class)
    public ResponseEntity<ProblemDetail> manejarCabeceraFaltante(CabeceraFaltanteException ex) {
        log.warn("Cabecera requerida faltante: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cabecera de autenticación requerida");
        problem.setType(URI.create("urn:banquito:error:cabecera-requerida"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
