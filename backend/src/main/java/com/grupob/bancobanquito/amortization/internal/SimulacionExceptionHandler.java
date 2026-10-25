package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.parameters.ParameterNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = SimulacionController.class)
class SimulacionExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SimulacionExceptionHandler.class);

    @ExceptionHandler(CondicionesSimulacionException.class)
    ProblemDetail manejarCondiciones(CondicionesSimulacionException excepcion) {
        return problema(HttpStatus.CONFLICT, "Condiciones no permitidas", excepcion.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, DataAccessException.class, ParameterNotFoundException.class})
    ProblemDetail manejarConfiguracion(RuntimeException excepcion) {
        LOG.error("Simulación no disponible: {}", excepcion.getClass().getSimpleName());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Simulación no disponible",
                "No se pudieron consultar las condiciones vigentes. Intenta nuevamente o contacta al administrador.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail manejarInesperado(Exception excepcion) {
        LOG.error("Fallo inesperado en simulación: {}", excepcion.getClass().getSimpleName());
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error al simular",
                "No se pudo completar la simulación. Intenta nuevamente.");
    }

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(ErrorResponseException excepcion,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String titulo = switch (status.value()) {
            case 400 -> "Datos inválidos";
            case 401 -> "Identificación requerida";
            case 403 -> "Acceso no permitido";
            default -> excepcion.getBody().getTitle();
        };
        var respuesta = excepcion.getBody();
        respuesta.setTitle(titulo);
        return handleExceptionInternal(excepcion, respuesta, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excepcion,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalle = excepcion.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).sorted().distinct()
                .collect(Collectors.joining(" "));
        return handleExceptionInternal(excepcion, problema(HttpStatus.BAD_REQUEST, "Datos inválidos", detalle),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException excepcion,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(excepcion, problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Envía un JSON válido con montoSolicitado, plazoMeses entero y sistemaAmortizacion FRANCES o ALEMAN."),
                headers, status, request);
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        var problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
