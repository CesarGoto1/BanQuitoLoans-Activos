package com.grupob.bancobanquito.cierre.internal;

import org.springframework.http.HttpStatus;

class ExcepcionCierre extends RuntimeException {

    private final HttpStatus status;

    ExcepcionCierre(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
