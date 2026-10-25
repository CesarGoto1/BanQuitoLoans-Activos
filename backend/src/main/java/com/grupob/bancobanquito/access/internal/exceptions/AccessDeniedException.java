package com.grupob.bancobanquito.access.internal.exceptions;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import org.springframework.http.HttpStatus;

public class AccessDeniedException extends RuntimeException {

    private final HttpStatus estado;

    public AccessDeniedException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
















