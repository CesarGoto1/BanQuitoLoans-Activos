package com.grupob.bancobanquito.prestamos.internal;

import org.springframework.http.HttpStatus;

class PrestamoException extends RuntimeException {

    private final HttpStatus status;

    PrestamoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
