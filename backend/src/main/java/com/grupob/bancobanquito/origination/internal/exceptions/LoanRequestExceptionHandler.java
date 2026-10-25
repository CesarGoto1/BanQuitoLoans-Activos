package com.grupob.bancobanquito.origination.internal.exceptions;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
import com.grupob.bancobanquito.liabilities.CustomerNotFoundException;
import com.grupob.bancobanquito.liabilities.LiabilitiesUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClientLoanRequestController.class)
public class LoanRequestExceptionHandler {

    @ExceptionHandler(InvalidLoanRequestStatusException.class)
    public ProblemDetail estadoInvalido(InvalidLoanRequestStatusException ex) {
        return problema(HttpStatus.BAD_REQUEST, "LoanRequest inválida", ex.getMessage());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail clienteNoEncontrado(CustomerNotFoundException ex) {
        return problema(HttpStatus.NOT_FOUND, "Cliente no encontrado", ex.getMessage());
    }

    @ExceptionHandler(LiabilitiesUnavailableException.class)
    public ProblemDetail pasivosNoDisponible(LiabilitiesUnavailableException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", ex.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
















