package com.grupob.bancobanquito.loans.internal;

import org.springframework.http.HttpStatus;

class LoanException extends RuntimeException {

    private final HttpStatus status;

    LoanException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
