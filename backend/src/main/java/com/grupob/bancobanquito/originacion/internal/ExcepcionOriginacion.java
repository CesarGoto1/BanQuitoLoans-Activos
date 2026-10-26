package com.grupob.bancobanquito.originacion.internal;

import org.springframework.http.HttpStatus;

class ExcepcionOriginacion extends RuntimeException {

    private final HttpStatus status;

    ExcepcionOriginacion(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
