package com.grupob.bancobanquito.access.internal.exceptions;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccessExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getEstado(), ex.getMessage());
        problema.setTitle(ex.getEstado().getReasonPhrase());
        return problema;
    }
}
















