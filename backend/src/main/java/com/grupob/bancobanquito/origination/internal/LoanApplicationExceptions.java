package com.grupob.bancobanquito.origination.internal;

import java.math.BigDecimal;

final class LoanApplicationExceptions {

    private LoanApplicationExceptions() {
    }

    static class AmountOutOfRange extends RuntimeException {

        AmountOutOfRange(BigDecimal minimum, BigDecimal maximum) {
            super("El monto debe estar entre USD " + minimum.toPlainString() + " y USD " + maximum.toPlainString());
        }
    }

    static class TermOutOfRange extends RuntimeException {

        TermOutOfRange(int minimum, int maximum) {
            super("El plazo debe estar entre " + minimum + " y " + maximum + " meses");
        }
    }

    static class InvalidIdentification extends RuntimeException {

        InvalidIdentification() {
            super("La cabecera X-Identificacion debe ser una cédula de 10 dígitos");
        }
    }

    static class PendingApplication extends RuntimeException {

        PendingApplication() {
            super("Ya tiene una solicitud pendiente de resolución; espere a que el analista la resuelva");
        }
    }
}
