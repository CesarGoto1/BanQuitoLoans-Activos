package com.grupob.bancobanquito.cierre.internal;

import org.springframework.http.HttpStatus;

class CierreException extends RuntimeException {

    private final HttpStatus status;

    CierreException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
