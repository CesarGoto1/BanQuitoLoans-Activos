package com.grupob.bancobanquito.cobranza.internal;

class ArchivoRecaudacionInvalidoException extends RuntimeException {

    private final String codigo;

    ArchivoRecaudacionInvalidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    String codigo() {
        return codigo;
    }
}
