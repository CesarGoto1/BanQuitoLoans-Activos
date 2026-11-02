package com.grupob.bancobanquito.originacion.internal;

import org.springframework.http.HttpStatus;

class OriginacionException extends RuntimeException {

    private final HttpStatus status;

    OriginacionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
