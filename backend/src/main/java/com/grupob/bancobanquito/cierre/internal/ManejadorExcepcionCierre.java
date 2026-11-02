package com.grupob.bancobanquito.cierre.internal;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ControllerCierreDia.class)
class ManejadorExcepcionCierre {

    @ExceptionHandler(CierreException.class)
    ProblemDetail closing(CierreException e) {
        return problem(e.getStatus(), "No se pudo procesar el cierre", e.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail locked(PessimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "No se pudo procesar el cierre",
                "El cierre del día ya está en ejecución.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail wrongType(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos", "El parámetro " + e.getName() + " tiene un valor inválido.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail wrongSort(PropertyReferenceException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos", "No se puede ordenar por " + e.getPropertyName() + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos", "El cuerpo de la petición no es un JSON válido.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
