package com.grupob.bancobanquito.acceso.internal;

import org.springframework.http.HttpStatus;

class AccesoException extends RuntimeException {

    private final HttpStatus status;

    AccesoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
