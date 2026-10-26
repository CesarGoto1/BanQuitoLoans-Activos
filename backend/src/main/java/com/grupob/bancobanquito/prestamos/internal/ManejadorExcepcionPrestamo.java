package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.pasivos.PasivosNoDisponibleException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {ControllerDesembolso.class, ControllerPrestamo.class})
class ManejadorExcepcionPrestamo {

    @ExceptionHandler(ExcepcionPrestamo.class)
    ProblemDetail loan(ExcepcionPrestamo e) {
        return problem(e.getStatus(), "No se pudo procesar", e.getMessage());
    }

    @ExceptionHandler(PasivosNoDisponibleException.class)
    ProblemDetail liabilitiesUnavailable(PasivosNoDisponibleException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Core de Pasivos no disponible", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail invalidParameter(HandlerMethodValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                e.getParameterValidationResults().getFirst().getResolvableErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(MissingServletRequestParameterException e) {
        return problem(HttpStatus.BAD_REQUEST, "Datos inválidos", "El parámetro " + e.getParameterName() + " es obligatorio.");
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicated(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT, "No se pudo procesar", "La solicitud ya fue desembolsada.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
