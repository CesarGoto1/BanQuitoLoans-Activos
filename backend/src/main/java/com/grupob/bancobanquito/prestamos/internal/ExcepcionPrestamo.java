package com.grupob.bancobanquito.prestamos.internal;

import org.springframework.http.HttpStatus;

class ExcepcionPrestamo extends RuntimeException {

    private final HttpStatus status;

    ExcepcionPrestamo(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
