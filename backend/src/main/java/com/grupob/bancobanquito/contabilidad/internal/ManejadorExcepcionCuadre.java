package com.grupob.bancobanquito.contabilidad.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ControllerCuadreContable.class)
class ManejadorExcepcionCuadre {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepcionCuadre.class);

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail fechaInvalida(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Fecha inválida",
                "Indica una fecha de negocio válida con formato AAAA-MM-DD.");
    }

    @ExceptionHandler(RuntimeException.class)
    ProblemDetail errorConsulta(RuntimeException e) {
        log.error("No se pudo consultar el cuadre contable", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo consultar el cuadre",
                "Ocurrió un error al consultar la contabilidad. Intenta nuevamente.");
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
