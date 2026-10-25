package com.grupob.bancobanquito.origination.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.grupob.bancobanquito.origination.internal")
public class SolicitudExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SolicitudExceptionHandler.class);

    @ExceptionHandler(LoanDecisionRuleException.class)
    public ProblemDetail handleDecisionConflict(LoanDecisionRuleException exception) {
        return problem(HttpStatus.CONFLICT, "No se puede resolver la solicitud", exception.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentDecision(PessimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, "Solicitud en proceso de resolución",
                "Otro analista está resolviendo la solicitud. Actualiza el detalle antes de continuar.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).distinct().sorted().collect(Collectors.joining(". "));
        return problem(HttpStatus.BAD_REQUEST, "Datos de resolución inválidos", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleInvalidRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Revisa el identificador de la solicitud y envía los campos con el formato correcto.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedFailure(Exception exception) {
        log.error("Loan request failed: {}", exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación",
                "Ocurrió un error al procesar la solicitud. Actualiza el detalle e intenta nuevamente.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

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
