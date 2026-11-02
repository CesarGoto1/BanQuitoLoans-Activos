package com.grupob.bancobanquito.prestamos.internal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ControllerAbonoCancelacion.class)
class ManejadorExcepcionAbonoCancelacion {

    @ExceptionHandler(PrestamoException.class)
    ProblemDetail negocio(PrestamoException e) {
        return problema(e.getStatus(), "No se pudo procesar", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail cuerpoInvalido(MethodArgumentNotValidException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "El cuerpo de la petición no es válido. La modalidad debe ser DISMINUIR_CUOTA o DISMINUIR_PLAZO.");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ProblemDetail conflicto(RuntimeException e) {
        return problema(HttpStatus.CONFLICT, "No se pudo procesar",
                "La operación está siendo modificada por otro proceso. Intenta de nuevo.");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
