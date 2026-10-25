package com.grupob.bancobanquito.origination.internal;

import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.MismatchedInputException;


@RestControllerAdvice(basePackageClasses = LoanApplicationExceptionHandler.class)
class LoanApplicationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationExceptionHandler.class);

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "requestedAmount", "monto solicitado",
            "termMonths", "plazo",
            "amortizationSystem", "sistema de amortización",
            "creditPurpose", "destino del crédito",
            "monthlyIncome", "ingresos mensuales",
            "monthlyExpenses", "gastos mensuales");

    @ExceptionHandler({LoanApplicationExceptions.AmountOutOfRange.class, LoanApplicationExceptions.TermOutOfRange.class,
            LoanApplicationExceptions.PendingApplication.class})
    ProblemDetail businessRule(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, "La solicitud no cumple una regla de negocio", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidFields(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .map(field -> FIELD_LABELS.getOrDefault(field, field))
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "Datos de la solicitud inválidos", "Revise los campos: " + fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().getLast().getPropertyName();
            return problem(HttpStatus.BAD_REQUEST, "Datos de la solicitud inválidos",
                    "Revise los campos: " + FIELD_LABELS.getOrDefault(field, field));
        }
        return problem(HttpStatus.BAD_REQUEST, "Datos de la solicitud inválidos",
                "El cuerpo de la solicitud no es un JSON válido");
    }

    @ExceptionHandler({MissingRequestHeaderException.class, LoanApplicationExceptions.InvalidIdentification.class})
    ProblemDetail invalidIdentification(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Identificación inválida",
                "La cabecera X-Identificacion debe ser una cédula de 10 dígitos");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpectedError(Exception e) {
        if (e instanceof ErrorResponse springResponse) {
            return springResponse.getBody();
        }
        log.error("Error inesperado en origination", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado al procesar la solicitud; intente nuevamente más tarde");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
