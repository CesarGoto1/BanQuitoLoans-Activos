package com.grupob.bancobanquito.acceso.internal;

import org.springframework.http.HttpStatus;

class ExcepcionAcceso extends RuntimeException {

    private final HttpStatus status;

    ExcepcionAcceso(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
