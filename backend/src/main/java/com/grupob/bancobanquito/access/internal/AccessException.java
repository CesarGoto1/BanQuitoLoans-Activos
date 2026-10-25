package com.grupob.bancobanquito.access.internal;

import org.springframework.http.HttpStatus;

class AccessException extends RuntimeException {

    private final HttpStatus status;

    AccessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
