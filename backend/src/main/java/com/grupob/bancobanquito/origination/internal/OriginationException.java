package com.grupob.bancobanquito.origination.internal;

import org.springframework.http.HttpStatus;

class OriginationException extends RuntimeException {

    private final HttpStatus status;

    OriginationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
