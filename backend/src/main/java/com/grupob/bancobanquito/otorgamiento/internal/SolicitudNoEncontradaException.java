package com.grupob.bancobanquito.otorgamiento.internal;

public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException(Integer idSolicitud) {
        super("No se encontró la solicitud de crédito con ID: " + idSolicitud);
    }

    public SolicitudNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
