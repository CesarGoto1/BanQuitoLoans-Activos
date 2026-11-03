package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.pasivos.PasivosNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ControllerArchivoRecaudacion.class)
class ManejadorExcepcionArchivoRecaudacion {

    @ExceptionHandler(PasivosNoDisponibleException.class)
    ProblemDetail pasivosNoDisponible(PasivosNoDisponibleException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problema.setTitle("Core de Pasivos no disponible");
        return problema;
    }
}
